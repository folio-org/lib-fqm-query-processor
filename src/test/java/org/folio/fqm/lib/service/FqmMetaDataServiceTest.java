package org.folio.fqm.lib.service;

import org.folio.fqm.lib.repository.MetaDataRepository;
import org.folio.fqm.lib.util.TestDataFixture;
import org.folio.querytool.domain.dto.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FqmMetaDataServiceTest {
  private FqmMetaDataService fqmMetaDataService;
  private MetaDataRepository metaDataRepository;

  @BeforeEach
  void setup() {
    metaDataRepository = mock(MetaDataRepository.class);
    this.fqmMetaDataService = new FqmMetaDataService(metaDataRepository);
  }

  @Test
  void shouldReturnEntityTypeDefinition() {
    UUID entityTypeId = UUID.randomUUID();
    String tenantId = "tenant_01";
    EntityType expectedEntityType = TestDataFixture.getEntityDefinition();

    when(metaDataRepository.getEntityTypeDefinition(tenantId, entityTypeId)).thenReturn(Optional.of(expectedEntityType));
    EntityType actualDefinition = fqmMetaDataService.getEntityTypeDefinition(tenantId, entityTypeId).get();

    assertEquals(expectedEntityType, actualDefinition);
  }

  @Test
  void shouldReturnDerivedTableName() {
    UUID entityTypeId = UUID.randomUUID();
    String tenantId = "tenant_01";
    String derivedTableName = "derived_table_01";

    when(metaDataRepository.getDerivedTableName(tenantId, entityTypeId)).thenReturn(Optional.of(derivedTableName));

    String actualDerivedTableName = fqmMetaDataService.getDerivedTableName(tenantId, entityTypeId);
    assertEquals(derivedTableName, actualDerivedTableName);
  }
}

