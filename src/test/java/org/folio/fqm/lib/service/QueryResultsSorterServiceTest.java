package org.folio.fqm.lib.service;

import org.folio.fqm.lib.model.IdsWithCancelCallback;
import org.folio.fqm.lib.repository.IdStreamer;
import org.folio.fqm.lib.repository.MetaDataRepository;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

class QueryResultsSorterServiceTest {
  private QueryResultsSorterService queryResultsService;
  private IdStreamer idStreamer;

  @BeforeEach
  void setup() {
    idStreamer = mock(IdStreamer.class);
    this.queryResultsService = new QueryResultsSorterService(idStreamer);
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

    queryResultsService.streamSortedIds(
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
}
