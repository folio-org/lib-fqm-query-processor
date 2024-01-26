package org.folio.fql.model;

public record EmptyCondition(String fieldName, Object value) implements FieldCondition<Object> {
  public static final String $EMPTY = "$empty";
}
