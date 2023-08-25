package org.folio.fqm.lib.repository;

import org.folio.fqm.lib.repository.dataproviders.MetaDataRepositoryTestDataProvider;
import org.folio.querytool.domain.dto.EntityType;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * NOTE - Tests in this class depends on the mock results returned from {@link MetaDataRepositoryTestDataProvider} class
 */
class MetaDataRepositoryTest {

  private MetaDataRepository metaDataRepository;

  @BeforeEach
  void setup() {
    this.metaDataRepository = new MetaDataRepository(
      DSL.using(new MockConnection(new MetaDataRepositoryTestDataProvider()), SQLDialect.POSTGRES).parsingDataSource());
  }

  @Test
  void shouldReturnValidDerivedTableName() {
    String tenant = "tenant_01";
    UUID entityTypeId = UUID.randomUUID();
    String actualTableName = metaDataRepository.getDerivedTableName(tenant, entityTypeId).get();
    assertEquals(tenant + "_mod_fqm_manager." + MetaDataRepositoryTestDataProvider.TEST_DERIVED_TABLE_NAME, actualTableName);
  }

  @Test
  void shouldReturnValidEntityTypeDefinition() {
    UUID entityTypeId = UUID.randomUUID();
    EntityType actualEntityTypeDefinition = metaDataRepository.getEntityTypeDefinition("tenant_01", entityTypeId).get();
    assertEquals(MetaDataRepositoryTestDataProvider.TEST_ENTITY_DEFINITION, actualEntityTypeDefinition);
  }
}
