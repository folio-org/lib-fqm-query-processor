package org.folio.fqm.lib.service;

import org.folio.fql.FqlService;
import org.folio.fql.deserializer.FqlParsingException;
import org.folio.fql.model.AndCondition;
import org.folio.fql.model.FieldCondition;
import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.querytool.domain.dto.EntityType;

import java.util.HashMap;
import java.util.Map;

public class FqlValidationService {

  private final FqlService fqlService;

  public FqlValidationService(FqlService fqlService) {
    this.fqlService = fqlService;
  }

  public Map<String, String> validateFql(EntityType entityType, String fqlCriteria) {
    try {
      Fql fql = fqlService.getFql(fqlCriteria);
      FqlCondition<?> fqlCondition = fql.fqlCondition();
      return assertFqlFieldPresentInEntityType(fqlCondition, entityType);
    } catch (FqlParsingException e) {
      return Map.of(e.getErrorField(), e.getErrorMessage());
    }
  }

  private Map<String, String> assertFqlFieldPresentInEntityType(FqlCondition<?> fqlCondition, EntityType entityType) {
    Map<String, String> errorMap = new HashMap<>();
    if (fqlCondition instanceof AndCondition andCondition) {
      andCondition.value()
        .forEach(subCondition -> errorMap.putAll(assertFqlFieldPresentInEntityType(subCondition, entityType)));
    } else {
      entityType.getColumns()
        .stream()
        .filter(col -> col.getName().equals(((FieldCondition<?>) fqlCondition).fieldName()))
        .findFirst()
        .ifPresentOrElse(
          value -> {},
          () -> {
            String fieldName = ((FieldCondition<?>) fqlCondition).fieldName();
            errorMap.put(fieldName, "Field " + fieldName + " is not present in definition of entity type " + entityType.getName());
          }
        );
    }
    return errorMap;
  }
}
