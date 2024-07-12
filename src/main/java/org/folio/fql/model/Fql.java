package org.folio.fql.model;


public record Fql(Integer _version, FqlCondition<?> fqlCondition) {
}
