package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

import java.util.List;

public record ContainsAllCondition(FqlField field, List<Object> value) implements FieldCondition<List<Object>> {
  public static final String $CONTAINS_ALL = "$contains_all";
}
