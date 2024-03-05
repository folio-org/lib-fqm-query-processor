package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

import java.util.List;

public record ContainsAnyCondition(FqlField field, List<Object> value) implements FieldCondition<List<Object>> {
  public static final String $CONTAINS_ANY = "$contains_any";
}
