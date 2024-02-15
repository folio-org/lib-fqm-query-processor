package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

public record ContainsCondition(FqlField field, Object value) implements FieldCondition<Object> {
  public static final String $CONTAINS = "$contains";
}
