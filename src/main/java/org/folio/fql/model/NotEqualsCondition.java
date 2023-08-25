package org.folio.fql.model;

public record NotEqualsCondition(String fieldName, Object value) implements FieldCondition<Object> {
  public static final String $NE = "$ne";
}
