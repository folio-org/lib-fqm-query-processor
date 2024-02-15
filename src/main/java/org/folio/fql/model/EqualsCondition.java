package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

public record EqualsCondition(FqlField field, Object value) implements FieldCondition<Object> {
  public static final String $EQ = "$eq";
}
