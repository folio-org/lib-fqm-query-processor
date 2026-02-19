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
import org.folio.querytool.domain.dto.Field;
import org.folio.querytool.domain.dto.JsonbArrayType;
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

      findFieldDefinition(field, entityType)
        .ifPresentOrElse(
          value -> {},
          () -> errorMap.put(
            field.serialize(),
            "Field " + field.serialize() + " is not present in definition of entity type " + entityType.getName()
          )
        );
    }
    return errorMap;
  }

  /** Get the Field (entity type column or nested property) based on a FQL field */
  public static Optional<Field> findFieldDefinition(FqlField search, EntityType entityType) {
    EntityTypeColumn column = entityType.getColumns().stream()
      .filter(col -> col.getName().equals(search.getColumnName()))
      .findFirst()
      .orElse(null);

    if (column == null) {
      return Optional.empty();
    }

    Deque<SubField> subFields = new ArrayDeque<>(search.getSubFields());
    Field curField = column;
    EntityDataType curDataType = column.getDataType();

    while (!subFields.isEmpty()) {
      SubField subField = subFields.pop();
      if (subField instanceof ArraySubField) {
        // For array types, we can assume that an array subfield will be followed by a property (enforced in FqlField
        // parser) therefore, we don't need to worry about curField not being set here
        if (curDataType instanceof ArrayType arrayDataType) {
          curDataType = arrayDataType.getItemDataType();
        } else if (curDataType instanceof JsonbArrayType jsonbArrayDataType) {
          curDataType = jsonbArrayDataType.getItemDataType();
        } else {
          return Optional.empty();
        }
      } else if (subField instanceof PropertySubField propertySubField) {
        if (curDataType instanceof ObjectType objectDataType) {
          Optional<NestedObjectProperty> nestedProperty = objectDataType.getProperties().stream()
            .filter(subCol -> subCol.getName().equals(propertySubField.propertyName()))
            .findFirst();

          if (nestedProperty.isPresent()) {
            curDataType = nestedProperty.get().getDataType();
            curField = nestedProperty.get();
          } else {
            return Optional.empty();
          }
        } else {
          return Optional.empty();
        }
      }
    }

    return Optional.of(curField);
  }

  public static Optional<Field> findFieldDefinitionForQuerying(FqlField search, EntityType entityType) {
    return findFieldDefinition(search, entityType).map(field -> {
      if (field.getIdColumnName() != null) {
        return findFieldDefinitionForQuerying(new FqlField(field.getIdColumnName()), entityType).orElse(null);
      } else {
        return field;
      }
    });
  }
}
