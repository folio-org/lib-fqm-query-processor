package org.folio.fql.model;

public sealed interface FqlCondition<T> permits FieldCondition, LogicalCondition {
  T value();
}
