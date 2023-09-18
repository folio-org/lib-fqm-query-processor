package org.folio.fql.model;

public sealed interface FieldCondition<T> extends FqlCondition<T> permits EqualsCondition, GreaterThanCondition,
  InCondition, LessThanCondition, NotEqualsCondition, NotInCondition, RegexCondition, ContainsCondition {
  String fieldName();
}
