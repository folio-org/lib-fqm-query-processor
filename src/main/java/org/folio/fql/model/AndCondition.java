package org.folio.fql.model;

import java.util.List;

public record AndCondition(List<FqlCondition<?>> value) implements LogicalCondition {
  public static final String $AND = "$and";
}
