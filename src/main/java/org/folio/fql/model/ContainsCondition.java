package org.folio.fql.model;

public record ContainsCondition(String fieldName, Object value) implements FieldCondition<Object> {
  public static final String $CONTAINS = "$contains";
}
