package org.folio.fqm.lib.service;

import org.folio.fqm.lib.exception.EntityTypeNotFoundException;
import org.folio.fqm.lib.repository.MetaDataRepository;
import org.folio.querytool.domain.dto.EntityType;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.UUID;

public class FqmMetaDataService {

  private final MetaDataRepository metaDataRepository;

  public FqmMetaDataService(DataSource dataSource) {
    this(new MetaDataRepository(dataSource));
  }

  FqmMetaDataService(MetaDataRepository metaDataRepository) {
    this.metaDataRepository = metaDataRepository;
  }

  public Optional<EntityType> getEntityTypeDefinition(String tenantId, UUID entityTypeId) {
    return metaDataRepository.getEntityTypeDefinition(tenantId, entityTypeId);
  }

  public String getDerivedTableName(String tenantId, UUID entityTypeId) {
    return metaDataRepository.getDerivedTableName(tenantId, entityTypeId)
      .orElseThrow(() -> new EntityTypeNotFoundException(entityTypeId));
  }
}
