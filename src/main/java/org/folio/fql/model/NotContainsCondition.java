package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

public record NotContainsCondition(FqlField field, Object value) implements FieldCondition<Object> {
  public static final String $NOT_CONTAINS = "$not_contains";
}
