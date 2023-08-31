package org.folio.fqm.lib;

import org.folio.fql.FqlService;
import org.folio.fqm.lib.service.FqmMetaDataService;
import org.folio.fqm.lib.service.QueryProcessorService;
import org.folio.fqm.lib.service.QueryResultsSorterService;
import org.folio.fqm.lib.service.ResultSetService;
import org.folio.fqm.lib.service.FqlValidationService;

import javax.sql.DataSource;

public class FQM {

  // private constructor to prevent instantiation
  private FQM() {
  }

  public static FqmMetaDataService fqmMetaDataService(DataSource dataSource) {
    return new FqmMetaDataService(dataSource);
  }

  public static QueryProcessorService queryProcessorService(DataSource dataSource) {
    return new QueryProcessorService(dataSource);
  }

  public static ResultSetService resultSetService(DataSource dataSource) {
    return new ResultSetService(dataSource);
  }

  public static QueryResultsSorterService queryResultsSorterService(DataSource dataSource) {
    return new QueryResultsSorterService(dataSource);
  }

  public static FqlValidationService fqlValidationService() {
    return new FqlValidationService(new FqlService());
  }

  public static FqlService fqlService() {
    return new FqlService();
  }
}
