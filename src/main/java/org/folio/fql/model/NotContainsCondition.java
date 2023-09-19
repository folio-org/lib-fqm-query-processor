package org.folio.fql.model;

public record NotContainsCondition(String fieldName, Object value) implements FieldCondition<Object> {
  public static final String $NOT_CONTAINS = "$not_contains";
}
