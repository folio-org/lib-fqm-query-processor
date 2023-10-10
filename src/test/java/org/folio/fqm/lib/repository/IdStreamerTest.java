package org.folio.fqm.lib.repository;

import org.folio.fqm.lib.exception.EntityTypeNotFoundException;
import org.folio.fqm.lib.model.IdsWithCancelCallback;
import org.folio.fql.model.EqualsCondition;
import org.folio.fql.model.Fql;
import org.folio.fqm.lib.repository.dataproviders.IdStreamerTestDataProvider;
import org.folio.querytool.domain.dto.EntityType;
import org.jooq.Condition;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.folio.fqm.lib.repository.MetaDataRepository.ID_FIELD_NAME;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NOTE - Tests in this class depends on the mock results returned from {@link IdStreamerTestDataProvider} class
 */
class IdStreamerTest {

  private static final String TENANT_ID = "Tenant_01";
  private static final UUID ENTITY_TYPE_ID = UUID.randomUUID();

  private IdStreamer idStreamer;

  @BeforeEach
  void setup() {
    MetaDataRepository metaDataRepository = new MetaDataRepository(DSL.using(
      new MockConnection(new IdStreamerTestDataProvider()), SQLDialect.POSTGRES).parsingDataSource());
    this.idStreamer = new IdStreamer(metaDataRepository);
  }

  @Test
  void shouldFetchIdStreamForQueryId() {
    UUID queryId = UUID.randomUUID();
    List<UUID> actualList = new ArrayList<>();
    Consumer<IdsWithCancelCallback> idsConsumer = idsWithCancelCallback -> actualList.addAll(idsWithCancelCallback.ids());
    int idsCount = idStreamer.streamIdsInBatch(TENANT_ID, queryId, true, 2, idsConsumer);
    assertEquals(IdStreamerTestDataProvider.TEST_CONTENT_IDS, actualList, "Expected List should equal Actual List");
    assertEquals(IdStreamerTestDataProvider.TEST_CONTENT_IDS.size(), idsCount);
  }

  @Test
  void shouldFetchIdStreamForFql() {
    Fql fql = new Fql(new EqualsCondition("field1", "value1"));
    List<UUID> actualList = new ArrayList<>();
    Consumer<IdsWithCancelCallback> idsConsumer = idsWithCancelCallback -> actualList.addAll(idsWithCancelCallback.ids());
    int idsCount = idStreamer.streamIdsInBatch(TENANT_ID, ENTITY_TYPE_ID, true, fql, 2, idsConsumer);
    assertEquals(IdStreamerTestDataProvider.TEST_CONTENT_IDS, actualList, "Expected List should equal Actual List");
    assertEquals(IdStreamerTestDataProvider.TEST_CONTENT_IDS.size(), idsCount);
  }

  @Test
  void shouldGetSortedIds() {
    UUID queryId = UUID.randomUUID();
    int offset = 0;
    int limit = 0;
    String derivedTableName = "table_01";
    EntityType entityType = new EntityType().name("test-entity");
    Condition condition = field(ID_FIELD_NAME).in(
      select(field("result_id"))
        .from(table("Tenant_01_mod_fqm_manager.query_results"))
        .where(field("query_id").eq(queryId))
    );
    List<UUID> actualIds = idStreamer.getSortedIds(derivedTableName, offset, limit);
    assertEquals(IdStreamerTestDataProvider.TEST_CONTENT_IDS, actualIds);
  }

  @Test
  void shouldThrowExceptionWhenEntityTypeNotFound() {
    Fql fql = new Fql(new EqualsCondition("field", "value"));
    Consumer<IdsWithCancelCallback> noop = idsWithCancelCallback -> {
    };
    MetaDataRepository mockRepository = mock(MetaDataRepository.class);
    IdStreamer idStreamerWithMockRepo = new IdStreamer(mockRepository);

    when(mockRepository.getEntityTypeDefinition(TENANT_ID, ENTITY_TYPE_ID)).thenReturn(Optional.empty());

    assertThrows(
      EntityTypeNotFoundException.class,
      () -> idStreamerWithMockRepo.streamIdsInBatch(TENANT_ID, ENTITY_TYPE_ID, true, fql, 1, noop)
    );
  }
}
