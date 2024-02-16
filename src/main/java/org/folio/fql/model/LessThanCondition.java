package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

public record LessThanCondition(FqlField field, boolean orEqualTo, Object value) implements FieldCondition<Object> {
  public static final String $LT = "$lt";
  public static final String $LTE = "$lte";
}
