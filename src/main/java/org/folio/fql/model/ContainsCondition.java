package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

public record ContainsCondition (FqlField field, String value) implements FieldCondition<String> {
  public static final String $CONTAINS = "$contains";
}
