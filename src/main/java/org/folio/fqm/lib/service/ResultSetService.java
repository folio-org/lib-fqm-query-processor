package org.folio.fqm.lib.service;

import org.folio.fqm.lib.repository.MetaDataRepository;
import org.folio.fqm.lib.repository.ResultSetRepository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.folio.fqm.lib.repository.MetaDataRepository.ID_FIELD_NAME;

public class ResultSetService {

  private final ResultSetRepository resultSetRepository;

  public ResultSetService(DataSource dataSource) {
    this(new ResultSetRepository(new MetaDataRepository(dataSource)));
  }

  ResultSetService(ResultSetRepository resultSetRepository) {
    this.resultSetRepository = resultSetRepository;
  }

  public List<Map<String, Object>> getResultSet(String tenantId,
                                                UUID entityTypeId,
                                                List<String> fields,
                                                List<UUID> ids) {
    List<Map<String, Object>> unsortedResults = resultSetRepository.getResultSet(tenantId, entityTypeId, fields, ids);
    // Sort the contents in Java code as sorting in DB views run very slow intermittently
    return getSortedContents(ids, unsortedResults);
  }

  private static List<Map<String, Object>> getSortedContents(List<UUID> contentIds, List<Map<String, Object>> unsortedResults) {
    Map<UUID, Map<String, Object>> contentsMap = unsortedResults.stream()
      .collect(Collectors.toMap(content -> (UUID) content.get(ID_FIELD_NAME), Function.identity()));

    return contentIds.stream()
      .map(contentsMap::get)
      .toList();
  }
}
