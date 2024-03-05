package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

import java.util.List;

public record NotContainsAnyCondition(FqlField field, List<Object> value) implements FieldCondition<List<Object>> {
  public static final String $NOT_CONTAINS_ANY = "$not_contains_any";
}
