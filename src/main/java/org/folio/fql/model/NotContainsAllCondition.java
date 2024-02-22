package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

import java.util.List;

public record NotContainsAllCondition(FqlField field, List<Object> value) implements FieldCondition<List<Object>> {
  public static final String $NOT_CONTAINS_ALL = "$not_contains_all";
}
