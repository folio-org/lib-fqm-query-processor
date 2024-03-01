package org.folio.fql.model;

import org.folio.fql.model.field.FqlField;

public sealed interface FieldCondition<T>
  extends FqlCondition<T>
  permits
    EqualsCondition,
    GreaterThanCondition,
    InCondition,
    LessThanCondition,
    NotEqualsCondition,
    NotInCondition,
    RegexCondition,
    ContainsAllCondition,
    NotContainsAllCondition,
    ContainsAnyCondition,
    NotContainsAnyCondition,
    EmptyCondition {
  FqlField field();
}
