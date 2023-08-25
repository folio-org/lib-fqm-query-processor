package org.folio.fql.model;

import java.util.List;

public record InCondition(String fieldName, List<Object> value) implements FieldCondition<List<Object>> {
  public static final String $IN = "$in";
}
