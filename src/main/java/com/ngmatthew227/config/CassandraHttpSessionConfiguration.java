package com.ngmatthew227.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.util.StringUtils;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.ngmatthew227.CassandraSessionRepository;

@Configuration
public class CassandraHttpSessionConfiguration extends SpringHttpSessionConfiguration implements ImportAware {

	private static final Logger logger = LoggerFactory.getLogger(CassandraSessionRepository.class);
	private String tableName = CassandraSessionRepository.DEFAULT_TABLE_NAME;
	private Integer maxInactiveIntervalInSeconds = 1800;
	private ConsistencyLevel consistencyLevel = DefaultConsistencyLevel.ONE;

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		logger.debug("Setting import metadata for CassandraHttpSession");
		Map<String, Object> attributeMap = importMetadata
				.getAnnotationAttributes(EnableCassandraHttpSession.class.getName());
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(attributeMap);
		this.tableName = attributes.getString("tableName");
		logger.debug("Table name set to: {}", tableName);
		this.maxInactiveIntervalInSeconds = attributes.getNumber("maxInactiveIntervalInSeconds");
		logger.debug("Max inactive interval set to: {} seconds", maxInactiveIntervalInSeconds);
		this.consistencyLevel = attributes.getEnum("consistencyLevel");
		logger.debug("Consistency level set to: {}", consistencyLevel);
	}

	@Bean
	public CassandraSessionRepository sessionRepository(CassandraTemplate cqlOperations) {
		logger.debug("Creating CassandraSessionRepository bean");
		CassandraSessionRepository cassandraSessionRepository = new CassandraSessionRepository(cqlOperations);

		if (StringUtils.hasText(this.tableName)) {
			cassandraSessionRepository.setTableName(this.tableName);
			logger.debug("Table name set to: {}", tableName);
		} else {
			cassandraSessionRepository.setTableName(CassandraSessionRepository.DEFAULT_TABLE_NAME);
			logger.debug("No table name provided, using default: {}", CassandraSessionRepository.DEFAULT_TABLE_NAME);
		}

		cassandraSessionRepository.setDefaultMaxInactiveInterval(this.maxInactiveIntervalInSeconds);
		logger.debug("Default max inactive interval set to: {} seconds", maxInactiveIntervalInSeconds);

		cassandraSessionRepository.setConsistencyLevel(this.consistencyLevel);
		logger.debug("Consistency level set to: {}", consistencyLevel);

		return cassandraSessionRepository;
	}

}
