package com.ngmatthew227;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.delete.Delete;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.ngmatthew227.utils.CustomQueryBuilder;
import com.ngmatthew227.utils.SessionAttributeSerializationUtils;

public class CassandraSessionRepository implements FindByIndexNameSessionRepository<CassandraHttpSession> {

	private static final Logger logger = LoggerFactory.getLogger(CassandraSessionRepository.class);
	public static final String DEFAULT_TABLE_NAME = "spring_session";
	private final CassandraOperations cassandraOperations;
	private int defaultMaxInactiveInterval = MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;
	private String tableName;
	private ConsistencyLevel consistencyLevel = DefaultConsistencyLevel.ONE;

	public CassandraSessionRepository(CassandraOperations cassandraOperations) {
		this.cassandraOperations = cassandraOperations;
	}

	public void setDefaultMaxInactiveInterval(int defaultMaxInactiveInterval) {
		this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public void setConsistencyLevel(ConsistencyLevel consistencyLevel) {
		this.consistencyLevel = consistencyLevel;
	}

	public String getIndexTableName() {
		return this.tableName + "_by_name";
	}

	@Override
	public CassandraHttpSession createSession() {
		logger.debug("Creating new CassandraHttpSession");
		CassandraHttpSession cassandraHttpSession = new CassandraHttpSession();
		cassandraHttpSession.setMaxInactiveInterval(Duration.ofSeconds(defaultMaxInactiveInterval));
		logger.debug("New session created with max inactive interval: {} seconds", defaultMaxInactiveInterval);
		return cassandraHttpSession;
	}

	@Override
	public void save(CassandraHttpSession session) {
		if (session.isExpired()) {
			logger.debug("Skipping save for expired session: {}", session.getId());
			return;
		}

		logger.debug("Saving session: {}", session.getId());
		BatchStatement batchStatement = CustomQueryBuilder.createNewSession(
				tableName, getIndexTableName(), consistencyLevel, session);
		logger.debug("Execute batch statement: {}", batchStatement);
		cassandraOperations.getCqlOperations().execute(batchStatement);

		session.onSave();
		logger.debug("Session saved successfully: {}", session.getId());
	}

	@Override
	public CassandraHttpSession findById(String id) {
		logger.debug("Finding session by ID: {}", id);
		Select select = QueryBuilder.selectFrom(this.tableName)
				.all()
				.whereColumn("id")
				.isEqualTo(QueryBuilder.literal(UUID.fromString(id)));

		List<Map<String, Object>> results = cassandraOperations.getCqlOperations().queryForList(select.asCql());
		if (results.isEmpty()) {
			logger.debug("No session found with ID: {}", id);
			return null;
		}

		Map<String, Object> row = results.get(0);
		Instant creationTime = (Instant) row.get("creation_time");
		Instant lastAccessed = (Instant) row.get("last_accessed");
		Integer maxInactiveIntervalInSeconds = (Integer) row.get("max_inactive_interval_in_seconds");
		Map<String, String> attributes = (Map<String, String>) row.get("attributes");

		CassandraHttpSession session = new CassandraHttpSession(id);
		session.setCreationTime(creationTime);
		session.setLastAccessedTime(lastAccessed);
		session.setMaxInactiveInterval(Duration.ofSeconds(maxInactiveIntervalInSeconds));
		session = SessionAttributeSerializationUtils.deserialize(attributes, session);
		session.onSave();

		logger.debug("Session found successfully: {}", session);
		return session;
	}

	@Override
	public void deleteById(String id) {
		logger.debug("Deleting session by ID: {}", id);
		CassandraHttpSession session = findById(id);
		if (session == null) {
			logger.debug("No session found with ID: {}", id);
			return;
		}

		Delete delete = QueryBuilder.deleteFrom(this.tableName)
				.whereColumn("id")
				.isEqualTo(QueryBuilder.literal(UUID.fromString(id)));

		BatchStatementBuilder batchBuilder = BatchStatement.builder(DefaultBatchType.LOGGED);
		batchBuilder.setConsistencyLevel(this.consistencyLevel);
		batchBuilder.addStatement(delete.build());

		String savedPrincipalName = session.getSavedPrincipalName();
		if (savedPrincipalName != null) {
			Delete deleteIdx = QueryBuilder.deleteFrom(this.getIndexTableName())
					.whereColumn("principal_name").isEqualTo(QueryBuilder.literal(savedPrincipalName))
					.whereColumn("id").isEqualTo(QueryBuilder.literal(UUID.fromString(id)));
			batchBuilder.addStatement(deleteIdx.build());
		}

		cassandraOperations.getCqlOperations().execute(batchBuilder.build());
		logger.debug("Session deleted successfully: {}", id);
	}

	@Override
	public Map<String, CassandraHttpSession> findByIndexNameAndIndexValue(String indexName, String indexValue) {
		logger.debug("Finding sessions by index name: {} and index value: {}", indexName, indexValue);
		Select select = QueryBuilder.selectFrom(this.getIndexTableName())
				.column("id")
				.whereColumn("principal_name").isEqualTo(QueryBuilder.literal(indexValue));
		List<UUID> uuids = cassandraOperations.select(select.build(), UUID.class);
		Map<String, CassandraHttpSession> result = new HashMap<String, CassandraHttpSession>();
		for (UUID id : uuids) {
			logger.debug("Found session with ID: {}", id.toString());
			CassandraHttpSession session = findById(id.toString());
			if (session != null) {
				result.put(id.toString(), findById(id.toString()));
			}
		}
		logger.debug("Found {} sessions successfully", result.size());
		return result;
	}

}
