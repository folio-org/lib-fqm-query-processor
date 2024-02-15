package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

public record GreaterThanCondition(FqlField field, boolean orEqualTo,
                                   Object value) implements FieldCondition<Object> {
  public static final String $GT = "$gt";
  public static final String $GTE = "$gte";
}
