package org.folio.fql.model;

import java.util.List;

public sealed interface LogicalCondition extends FqlCondition<List<FqlCondition<?>>> permits AndCondition {
}
