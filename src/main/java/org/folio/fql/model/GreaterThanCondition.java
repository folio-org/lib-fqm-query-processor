package org.folio.fql.model;

public record GreaterThanCondition(String fieldName, boolean orEqualTo,
                                   Object value) implements FieldCondition<Object> {
  public static final String $GT = "$gt";
  public static final String $GTE = "$gte";
}
