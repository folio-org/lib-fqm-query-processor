package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

public record StartsWithCondition (FqlField field, String value) implements FieldCondition<String> {
  public static final String $STARTS_WITH = "$starts_with";
}
