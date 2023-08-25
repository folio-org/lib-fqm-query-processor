package org.folio.fql.model;

public record LessThanCondition(String fieldName, boolean orEqualTo, Object value) implements FieldCondition<Object> {
  public static final String $LT = "$lt";
  public static final String $LTE = "$lte";
}
