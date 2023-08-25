package org.folio.fqm.lib.repository;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.fql.model.Fql;
import org.folio.fqm.lib.exception.EntityTypeNotFoundException;
import org.folio.querytool.domain.dto.EntityType;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.util.*;

import static org.folio.fqm.lib.repository.MetaDataRepository.ID_FIELD_NAME;
import static org.jooq.impl.DSL.field;

@Log4j2
public class ResultSetRepository {
  private final DSLContext jooqContext;
  private final MetaDataRepository metaDataRepository;
  private final FqlToSqlConverter fqlToSqlConverter;
  private final SqlFieldIdentificationService sqlFieldService;

  public ResultSetRepository(MetaDataRepository metaDataRepository) {
    this(metaDataRepository, new FqlToSqlConverter());
  }

  ResultSetRepository(MetaDataRepository metaDataRepository, FqlToSqlConverter fqlToSqlConverter) {
    this.jooqContext = metaDataRepository.dsl();
    this.metaDataRepository = metaDataRepository;
    this.fqlToSqlConverter = fqlToSqlConverter;
    this.sqlFieldService = new SqlFieldIdentificationService();
  }

  public List<Map<String, Object>> getResultSet(String tenantId,
                                                UUID entityTypeId,
                                                List<String> fields,
                                                List<UUID> ids) {
    if (CollectionUtils.isEmpty(fields)) {
      log.info("List of fields to retrieve is empty. Returning empty results list.");
      return List.of();
    }
    var fieldsToSelect = getSqlFields(getEntityType(tenantId, entityTypeId), fields);
    return jooqContext.select(fieldsToSelect)
      .from(metaDataRepository.getDerivedTableName(tenantId, entityTypeId)
        .orElseThrow(() -> new EntityTypeNotFoundException(entityTypeId)))
      .where(field(ID_FIELD_NAME).in(ids))
      .fetch()
      .map(Record::intoMap);
  }

  public List<Map<String, Object>> getResultSet(String tenantId, UUID entityTypeId, Fql fql, List<String> fields, UUID afterId, int limit) {
    if (CollectionUtils.isEmpty(fields)) {
      log.info("List of fields to retrieve is empty. Returning empty results list.");
      return List.of();
    }
    Condition afterIdCondition = afterId != null ? field(ID_FIELD_NAME).greaterThan(afterId) : DSL.noCondition();
    EntityType entityType = getEntityType(tenantId, entityTypeId);
    Condition condition = fqlToSqlConverter.getSqlCondition(fql.fqlCondition(), entityType);
    var fieldsToSelect = getSqlFields(entityType, fields);
    var sortCriteria = hasIdColumn(entityType) ? field(ID_FIELD_NAME) : DSL.noField();
    return jooqContext.select(fieldsToSelect)
      .from(metaDataRepository.getDerivedTableName(tenantId, entityTypeId)
        .orElseThrow(() -> new EntityTypeNotFoundException(entityTypeId)))
      .where(condition)
      .and(afterIdCondition)
      .orderBy(sortCriteria)
      .limit(limit)
      .fetch()
      .map(Record::intoMap);
  }

  private List<Field<Object>> getSqlFields(EntityType entityType, List<String> fields) {
    Set<String> fieldSet = new HashSet<>(fields);
    return entityType.getColumns()
      .stream()
      .filter(col -> fieldSet.contains(col.getName()))
      .map(col -> sqlFieldService.getSqlField(col).as(col.getName()))
      .toList();
  }

  private EntityType getEntityType(String tenantId, UUID entityTypeId) {
    return metaDataRepository.getEntityTypeDefinition(tenantId, entityTypeId)
      .orElseThrow(() -> new EntityTypeNotFoundException(entityTypeId));
  }

  private boolean hasIdColumn(EntityType entityType) {
    return entityType.getColumns().stream()
      .anyMatch(col -> ID_FIELD_NAME.equals(col.getName()));
  }
}
