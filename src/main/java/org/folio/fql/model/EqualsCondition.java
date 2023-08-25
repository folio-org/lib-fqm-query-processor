package org.folio.fql.model;

public record EqualsCondition(String fieldName, Object value) implements FieldCondition<Object> {
  public static final String $EQ = "$eq";
}
