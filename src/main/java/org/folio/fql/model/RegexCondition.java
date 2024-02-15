package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

public record RegexCondition(FqlField field, String value) implements FieldCondition<String> {
  public static final String $REGEX = "$regex";
}
