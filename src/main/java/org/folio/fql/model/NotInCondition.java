package org.folio.fql.model;

import java.util.List;
import org.folio.fql.model.field.FqlField;

public record NotInCondition(FqlField field, List<Object> value) implements FieldCondition<List<Object>> {
  public static final String $NIN = "$nin";
}
