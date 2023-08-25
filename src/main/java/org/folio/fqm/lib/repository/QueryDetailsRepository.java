package org.folio.fqm.lib.repository;

import org.jooq.DSLContext;
import org.jooq.Field;

import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.val;

public class QueryDetailsRepository {
  private static final Field<UUID> ENTITY_TYPE_ID = field("entity_type_id", UUID.class);
  private static final Field<UUID> QUERY_ID = field("query_id", UUID.class);

  private final DSLContext jooqContext;
  private final MetaDataRepository metaDataRepository;

  public QueryDetailsRepository(MetaDataRepository metaDataRepository) {
    this.jooqContext = metaDataRepository.dsl();
    this.metaDataRepository = metaDataRepository;
  }

  public UUID getEntityTypeId(String tenantId, UUID queryId) {
    return jooqContext.select(ENTITY_TYPE_ID)
      .from(table(metaDataRepository.getFqmSchemaName(tenantId) + ".query_details"))
      .where(QUERY_ID.eq(val(queryId)))
      .fetchOne(ENTITY_TYPE_ID);
  }
}
