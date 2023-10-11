package org.folio.fqm.lib.service;

import org.folio.fqm.lib.model.IdsWithCancelCallback;
import org.folio.fqm.lib.repository.IdStreamer;
import org.folio.fqm.lib.repository.MetaDataRepository;
import org.folio.querytool.domain.dto.EntityType;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static org.folio.fqm.lib.repository.MetaDataRepository.ID_FIELD_NAME;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

class QueryResultsSorterServiceTest {
  private QueryResultsSorterService queryResultsSorterService;
  private IdStreamer idStreamer;

  @BeforeEach
  void setup() {
    idStreamer = mock(IdStreamer.class);
    this.queryResultsSorterService = new QueryResultsSorterService(idStreamer);
  }

  @Test
  void shouldStreamSortedIds() {
    String tenantId = "tenant_01";
    UUID queryId = UUID.randomUUID();
    int batchSize = 100;
    var sqlSelectionClause = select(field("result_id"))
      .from("corsair_lib_mod_fqm_manager.query_results")
      .where(field("query_id").eq(queryId));
    Condition sqlWhereClause = DSL.field(MetaDataRepository.ID_FIELD_NAME).in(sqlSelectionClause);
    Consumer<IdsWithCancelCallback> idsConsumer = mock(Consumer.class);
    IntConsumer totalCountConsumer = mock(IntConsumer.class);
    Consumer<Throwable> errorConsumer = mock(Consumer.class);

    queryResultsSorterService.streamSortedIds(
      tenantId,
      queryId,
      batchSize,
      idsConsumer,
      totalCountConsumer,
      errorConsumer
    );
    verify(idStreamer, times(1))
      .streamIdsInBatch(
        tenantId,
        queryId,
        true,
        batchSize,
        idsConsumer
      );
  }

  @Test
  void shouldGetSortedIds() {
    String tenantId = "tenant_01";
    UUID queryId = UUID.randomUUID();
    int offset = 0;
    int limit = 0;
    List<UUID> expectedIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    when(idStreamer.getSortedIds(tenantId, queryId, offset, limit)).thenReturn(expectedIds);
    List<UUID> actualIds = queryResultsSorterService.getSortedIds(tenantId, queryId, offset, limit);
    assertEquals(expectedIds, actualIds);
  }
}
