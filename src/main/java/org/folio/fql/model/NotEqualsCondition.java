package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

public record NotEqualsCondition(FqlField field, Object value) implements FieldCondition<Object> {
  public static final String $NE = "$ne";
}
