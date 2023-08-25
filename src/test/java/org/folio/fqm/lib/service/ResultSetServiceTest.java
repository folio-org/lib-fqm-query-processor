package org.folio.fqm.lib.service;

import com.google.common.collect.Lists;
import org.folio.fqm.lib.repository.MetaDataRepository;
import org.folio.fqm.lib.repository.ResultSetRepository;
import org.folio.fqm.lib.util.TestDataFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResultSetServiceTest {

  private ResultSetRepository resultSetRepository;
  private ResultSetService service;

  @BeforeEach
  void setUp() {
    this.resultSetRepository = mock(ResultSetRepository.class);
    this.service = new ResultSetService(resultSetRepository);
  }

  @Test
  void shouldGetResultSet() {
    String tenantId = "tenant_01";
    UUID entityTypeId = UUID.randomUUID();
    List<Map<String, Object>> expectedResult = TestDataFixture.getEntityContents();
    List<Map<String, Object>> reversedContent = Lists.reverse(expectedResult);
    List<String> fields = List.of("id", "key1", "key2");
    List<UUID> listIds = new ArrayList<>();
    expectedResult.forEach(content -> listIds.add((UUID) content.get(MetaDataRepository.ID_FIELD_NAME)));
    when(resultSetRepository.getResultSet(tenantId, entityTypeId, fields, listIds)).thenReturn(reversedContent);
    List<Map<String, Object>> actualResult = service.getResultSet(tenantId, entityTypeId, fields, listIds);
    assertEquals(expectedResult, actualResult);
  }
}
