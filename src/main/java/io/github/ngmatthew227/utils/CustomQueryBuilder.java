package io.github.ngmatthew227.utils;

import java.util.Map;
import java.util.UUID;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.delete.Delete;
import com.datastax.oss.driver.api.querybuilder.insert.Insert;
import io.github.ngmatthew227.CassandraHttpSession;

public class CustomQueryBuilder {
  public static BatchStatement createNewSession(String tableName, String indexTableName,
      ConsistencyLevel consistencyLevel, CassandraHttpSession session) {

    int ttl = TtlCalculator.calculateTtlInSeconds(System.currentTimeMillis(), session);
    BatchStatementBuilder batchBuilder = BatchStatement.builder(DefaultBatchType.LOGGED);
    batchBuilder.setConsistencyLevel(consistencyLevel);
    Map<String, String> serializedAttributes = SessionAttributeSerializationUtils.serialize(session);
    batchBuilder.addStatement(QueryBuilder.insertInto(tableName)
        .value("id", QueryBuilder.literal(UUID.fromString(session.getId())))
        .value("creation_time", QueryBuilder.literal(session.getCreationTime()))
        .value("last_accessed", QueryBuilder.literal(session.getLastAccessedTime()))
        .value("max_inactive_interval_in_seconds", QueryBuilder.literal(session.getMaxInactiveInterval().toSeconds()))
        .value("attributes", QueryBuilder.literal(serializedAttributes))
        .usingTtl(ttl).build());

    String savedPrincipalName = session.getSavedPrincipalName();
    String currentPrincipalName = session.getCurrentPrincipalName();

    boolean shouldDeleteIdx = savedPrincipalName != null && !savedPrincipalName.equals(currentPrincipalName);
    boolean shouldInsertIdx = currentPrincipalName != null;

    if (shouldDeleteIdx) {
      Delete delete = QueryBuilder.deleteFrom(indexTableName)
          .whereColumn("principal_name").isEqualTo(QueryBuilder.literal(savedPrincipalName))
          .whereColumn("id").isEqualTo(QueryBuilder.literal(UUID.fromString(session.getId())));
      batchBuilder.addStatement(delete.build());
    }
    if (shouldInsertIdx) {
      Insert insertIdx = QueryBuilder.insertInto(indexTableName)
          .value("id", QueryBuilder.literal(UUID.fromString(session.getId())))
          .value("principal_name", QueryBuilder.literal(currentPrincipalName))
          .usingTtl(ttl);
      batchBuilder.addStatement(insertIdx.build());
    }

    return batchBuilder.build();

  }


}
