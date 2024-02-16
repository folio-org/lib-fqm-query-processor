package org.folio.fql.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.folio.fql.deserializer.FqlParsingException;
import org.folio.fql.model.AndCondition;
import org.folio.fql.model.FieldCondition;
import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.field.ArraySubField;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.model.field.PropertySubField;
import org.folio.fql.model.field.SubField;
import org.folio.querytool.domain.dto.ArrayType;
import org.folio.querytool.domain.dto.EntityDataType;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.NestedObjectProperty;
import org.folio.querytool.domain.dto.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FqlValidationService {

  private final FqlService fqlService;

  @Autowired
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
      andCondition
        .value()
        .forEach(subCondition -> errorMap.putAll(assertFqlFieldPresentInEntityType(subCondition, entityType)));
    } else {
      FqlField field = ((FieldCondition<?>) fqlCondition).field();

      entityType
        .getColumns()
        .stream()
        .filter(col -> checkColumnMatch(field, col))
        .findFirst()
        .ifPresentOrElse(
          value -> {},
          () -> {
            String fieldName = field.serialize();
            errorMap.put(
              fieldName,
              "Field " + fieldName + " is not present in definition of entity type " + entityType.getName()
            );
          }
        );
    }
    return errorMap;
  }

  public static boolean checkColumnMatch(FqlField field, EntityTypeColumn col) {
    if (!col.getName().equals(field.getColumnName())) {
      return false;
    }

    Deque<SubField> subFields = new ArrayDeque<>(field.getSubFields());
    EntityDataType dataType = col.getDataType();

    while (!subFields.isEmpty()) {
      SubField subField = subFields.pop();
      if (subField instanceof ArraySubField) {
        if (dataType instanceof ArrayType arrayDataType) {
          dataType = arrayDataType.getItemDataType();
        } else {
          return false;
        }
      } else if (subField instanceof PropertySubField propertySubField) {
        if (dataType instanceof ObjectType objectDataType) {
          Optional<NestedObjectProperty> nestedProperty = objectDataType.getProperties().stream()
            .filter(subCol -> subCol.getName().equals(propertySubField.propertyName()))
            .findFirst();

          if (nestedProperty.isPresent()) {
            dataType = nestedProperty.get().getDataType();
          } else {
            return false;
          }
        } else {
          return false;
        }
      }
    }

    return true;
  }
}
