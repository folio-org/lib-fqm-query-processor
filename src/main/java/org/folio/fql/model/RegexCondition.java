package org.folio.fql.model;

public record RegexCondition(String fieldName, String value) implements FieldCondition<String> {
  public static final String $REGEX = "$regex";
}
