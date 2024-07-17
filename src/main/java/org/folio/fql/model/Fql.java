package org.folio.fql.model;


public record Fql(String _version, FqlCondition<?> fqlCondition) {
}
