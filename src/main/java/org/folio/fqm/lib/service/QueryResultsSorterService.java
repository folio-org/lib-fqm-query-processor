package org.folio.fqm.lib.service;

import lombok.extern.log4j.Log4j2;
import org.folio.fqm.lib.model.IdsWithCancelCallback;
import org.folio.fqm.lib.repository.IdStreamer;
import org.folio.fqm.lib.repository.MetaDataRepository;
import org.folio.querytool.domain.dto.EntityType;
import org.jooq.Condition;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static org.folio.fqm.lib.repository.MetaDataRepository.ID_FIELD_NAME;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

/**
 * Sorts the results of a query according to the default sort criteria of the entity type, and then
 * streams back to calling application.
 */
@Log4j2
public class QueryResultsSorterService {
  private final IdStreamer idStreamer;

  public QueryResultsSorterService(DataSource dataSource) {
    this(new IdStreamer(new MetaDataRepository(dataSource)));
  }

  QueryResultsSorterService(IdStreamer idStreamer) {
    this.idStreamer = idStreamer;
  }

  /**
   * Streams the result IDs of provided queryId, after sorting the results based on default sort criteria of entity type.
   *
   * @param tenantId       Tenant ID
   * @param queryId        Query ID
   * @param batchSize      Count of IDs to be returned in a single batch
   * @param idsConsumer    This consumer will receive the IDs in batches, and each batch will also include a
   *                       callback for canceling the query. The consuming application can utilize this callback
   *                       to cancel the query.
   * @param successHandler Once the query execution is finished, this consumer is triggered. It will be provided
   *                       with the overall count of records that corresponded to the query.
   * @param errorHandler   If an error occurs during query execution, this consumer will be called and given the
   *                       exception responsible for the failure
   */
  public void streamSortedIds(String tenantId,
                              UUID queryId,
                              int batchSize,
                              Consumer<IdsWithCancelCallback> idsConsumer,
                              IntConsumer successHandler,
                              Consumer<Throwable> errorHandler) {
    try {
      log.info("Streaming sorted result ids for tenant {}, queryId: {}", tenantId, queryId);
      int idsCount = idStreamer.streamIdsInBatch(
        tenantId,
        queryId,
        true,
        batchSize,
        idsConsumer
      );
      successHandler.accept(idsCount);
    } catch (Exception exception) {
      errorHandler.accept(exception);
    }
  }

  public List<UUID> getSortedIds(String tenantId, UUID queryId, String derivedTableName,
                                 EntityType entityType, int offset, int limit) {
    log.debug("Getting sorted ids for query {}, offset {}, limit {}", queryId, offset, limit);
    Condition condition = field(ID_FIELD_NAME).in(
      select(field("result_id"))
        .from(table(getFqmSchemaName(tenantId) + ".query_results"))
        .where(field("query_id").eq(queryId))
    );
    // Sort ids based on the sort criteria defined in the entity type definition
    return idStreamer.getSortedIds(derivedTableName, entityType, condition, offset, limit);
  }

  private String getFqmSchemaName(String tenantId) {
    return tenantId + "_mod_fqm_manager";
  }
}
