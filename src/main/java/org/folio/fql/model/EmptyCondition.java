package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

public record EmptyCondition(FqlField field, Object value) implements FieldCondition<Object> {
  public static final String $EMPTY = "$empty";
}
