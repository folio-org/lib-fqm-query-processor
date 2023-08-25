package org.folio.fqm.lib.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.querytool.domain.dto.EntityType;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.field;

@Log4j2
public class MetaDataRepository {

  public static final String ID_FIELD_NAME = "id";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final DSLContext jooqContext;

  public MetaDataRepository(DataSource dataSource) {
    this(DSL.using(dataSource, SQLDialect.POSTGRES));
  }

  MetaDataRepository(DSLContext jooqContext) {
    this.jooqContext = jooqContext;
  }

  public Optional<String> getDerivedTableName(String tenantId, UUID entityTypeId) {
    log.info("Getting derived table name for tenant {}, entity type ID: {}", tenantId, entityTypeId);
    String schemaName = getFqmSchemaName(tenantId);
    Field<String> derivedTableNameField = field("derived_table_name", String.class);
    return jooqContext
      .select(derivedTableNameField)
      .from(schemaName + ".entity_type_definition")
      .where(field(ID_FIELD_NAME).eq(entityTypeId))
      .fetchOptional(derivedTableNameField)
      .map(tableName -> schemaName + "." + tableName);
  }

  public Optional<EntityType> getEntityTypeDefinition(String tenantId, UUID entityTypeId) {
    log.info("Getting definition name for tenant {}, entity type ID: {}", tenantId, entityTypeId);
    var definitionField = field("definition", String.class);
    return jooqContext
      .select(definitionField)
      .from(getFqmSchemaName(tenantId) + ".entity_type_definition")
      .where(field(ID_FIELD_NAME).eq(entityTypeId))
      .fetchOptional(definitionField)
      .map(this::unmarshallEntityType);
  }

  String getFqmSchemaName(String tenantId) {
    return tenantId + "_mod_fqm_manager";
  }

  DSLContext dsl() {
    return jooqContext;
  }

  @SneakyThrows
  private EntityType unmarshallEntityType(String str) {
    return objectMapper.readValue(str, EntityType.class);
  }
}
