package org.folio.fql.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.folio.fql.model.field.FqlField;
import org.folio.querytool.domain.dto.ArrayType;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.JsonbArrayType;
import org.folio.querytool.domain.dto.MarcType;
import org.folio.querytool.domain.dto.NestedObjectProperty;
import org.folio.querytool.domain.dto.ObjectType;
import org.folio.querytool.domain.dto.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FqlValidationServiceTest {

  private static final EntityType entityType = new EntityType()
    .name("test_entity_type")
    .columns(
      List.of(
        new EntityTypeColumn().name("field1").dataType(new StringType()),
        new EntityTypeColumn().name("field2").dataType(new StringType()),
        new EntityTypeColumn().name("field3").dataType(new StringType()),
        new EntityTypeColumn().name("objectField").dataType(
          new ObjectType().addPropertiesItem(new NestedObjectProperty().name("property1"))
            .addPropertiesItem(new NestedObjectProperty().name("hasValidIdColumn").idColumnName("field1"))
            .addPropertiesItem(new NestedObjectProperty().name("hasInvalidIdColumn").idColumnName("does-not-exist"))
            .addPropertiesItem(new NestedObjectProperty().name("hasNestedIdColumn").idColumnName("objectField->property1"))
        ),
        new EntityTypeColumn().name("objectObjectField").dataType(
          new ObjectType().addPropertiesItem(new NestedObjectProperty().name("property1").dataType(
            new ObjectType().addPropertiesItem(new NestedObjectProperty().name("innerProperty1"))
          ))
        ),
        new EntityTypeColumn().name("objectObjectObjectField").dataType(
          new ObjectType().addPropertiesItem(new NestedObjectProperty().name("property1").dataType(
            new ObjectType().addPropertiesItem(new NestedObjectProperty().name("innerProperty1").dataType(
              new ObjectType().addPropertiesItem(new NestedObjectProperty().name("innerInnerProperty1"))
            ))
          ))
        ),
        new EntityTypeColumn().name("arrayField").dataType(new ArrayType().itemDataType(new StringType())),
        new EntityTypeColumn().name("arrayObjectField").dataType(
          new ArrayType().itemDataType(
            new ObjectType().addPropertiesItem(new NestedObjectProperty().name("property1"))
          )
        ),
        new EntityTypeColumn().name("arrayArrayObjectField").dataType(
          new ArrayType().itemDataType(
            new ArrayType().itemDataType(
              new ObjectType().addPropertiesItem(new NestedObjectProperty().name("property1"))
            )
          )
        ),
        new EntityTypeColumn().name("jsonbArrayField").dataType(new JsonbArrayType().itemDataType(new StringType())),
        new EntityTypeColumn().name("jsonbArrayObjectField").dataType(
          new JsonbArrayType().itemDataType(
            new ObjectType().addPropertiesItem(new NestedObjectProperty().name("property1"))
          )
        ),
        new EntityTypeColumn().name("jsonbArrayJsonbArrayObjectField").dataType(
          new JsonbArrayType().itemDataType(
            new JsonbArrayType().itemDataType(
              new ObjectType().addPropertiesItem(new NestedObjectProperty().name("property1"))
            )
          )
        ),
        new EntityTypeColumn().name("hasValidIdColumn").dataType(new StringType()).idColumnName("field1"),
        new EntityTypeColumn().name("hasInvalidIdColumn").dataType(new StringType()).idColumnName("does-not-exist")
      )
    );

  private FqlValidationService fqlValidationService;

  @BeforeEach
  void setup() {
    this.fqlValidationService = new FqlValidationService(new FqlService());
  }

  @Test
  void shouldValidateFql() {
    String fqlCondition = """
      {"field1": {"$eq": "some value"}}
      """;
    Map<String, String> expectedErrors = Map.of();
    Map<String, String> actualErrors = fqlValidationService.validateFql(entityType, fqlCondition);
    assertEquals(expectedErrors, actualErrors);
  }

  @Test
  void shouldValidateComplexFql() {
    String complexFql = """
      {
         "$and":[
            { "field1": { "$eq":"some value" } },
            { "field2": { "$eq":true } },
            {
              "$and":[
                  { "field3": { "$lte":3 } },
                  { "field1": { "$eq":false } }
              ]},
            { "field2":{ "$in":[ "value1", 2, true ] }},
            { "field3":{ "$ne": 5 }},
            { "field1":{ "$gt": 9 }},
            { "field2":{ "$lt": 11 }},
            { "field3":{ "$nin":[ "value1", 2, true ] }}
         ]
      }
      """;
    Map<String, String> expectedErrors = Map.of();
    Map<String, String> actualErrors = fqlValidationService.validateFql(entityType, complexFql);
    assertEquals(expectedErrors, actualErrors);
  }

  @Test
  void shouldReturnErrorForInvalidFqlSyntax() {
    String fqlCondition = """
      {"field1": {"$eq": "some value"}
      """;
    Map<String, String> actualErrors = fqlValidationService.validateFql(entityType, fqlCondition);
    assertEquals(1, actualErrors.size());
    assertTrue(actualErrors.containsKey(fqlCondition) && actualErrors.get(fqlCondition) != null);
  }

  @Test
  void shouldReturnErrorForInvalidFqlOperator() {
    String fqlCondition = """
      {"field1": {"$xy": "some value"}}
      """;
    String expectedErrorMessage = "Condition {\"$xy\":\"some value\"} contains an invalid operator";
    Map<String, String> expectedErrors = Map.of("field1", expectedErrorMessage);
    Map<String, String> actualErrors = fqlValidationService.validateFql(entityType, fqlCondition);
    assertEquals(expectedErrors, actualErrors);
  }

  @Test
  void shouldReturnErrorWhenFieldMissingFromEntityType() {
    String fqlCondition = """
      {"field4": {"$eq": "some value"}}
      """;
    String expectedErrorMessage = "Field field4 is not present in definition of entity type " + entityType.getName();
    Map<String, String> expectedErrors = Map.of("field4", expectedErrorMessage);
    Map<String, String> actualErrors = fqlValidationService.validateFql(entityType, fqlCondition);
    assertEquals(expectedErrors, actualErrors);
  }

  @Test
  void shouldReturnMultipleErrorsForFqlWithMultipleErrors() {
    String fqlCondition = """
      {
         "$and":[
            { "field4": { "$eq":"some value" } },
            { "field5": { "$eq":true } }
         ]
      }
      """;
    String expectedErrorMessage1 = "Field field4 is not present in definition of entity type " + entityType.getName();
    String expectedErrorMessage2 = "Field field5 is not present in definition of entity type " + entityType.getName();
    Map<String, String> expectedErrors = Map.of(
      "field4", expectedErrorMessage1,
      "field5", expectedErrorMessage2
    );
    Map<String, String> actualErrors = fqlValidationService.validateFql(entityType, fqlCondition);
    assertEquals(expectedErrors, actualErrors);
  }

  static List<Arguments> nestedFieldValidityParameters() {
    return List.of(
      Arguments.of("foo->bar", false),
      Arguments.of("field1->foo", false),
      Arguments.of("field1[*]", false),
      Arguments.of("field1[*]->foo", false),
      Arguments.of("objectField", true),
      Arguments.of("objectField->property1", true),
      Arguments.of("objectField->property1->foo", false),
      Arguments.of("objectField->property1[*]", false),
      Arguments.of("objectField->property2", false),
      Arguments.of("objectField[*]", false),
      Arguments.of("objectObjectField", true),
      Arguments.of("objectObjectField->property1", true),
      Arguments.of("objectObjectField->property1->innerProperty1", true),
      Arguments.of("objectObjectField->property1->innerProperty1->foo", false),
      Arguments.of("objectObjectField->property1->innerProperty1[*]", false),
      Arguments.of("objectObjectField->property1[*]", false),
      Arguments.of("objectObjectField->property2", false),
      Arguments.of("objectObjectField->property1->foo", false),
      Arguments.of("objectObjectObjectField", true),
      Arguments.of("objectObjectObjectField->property1", true),
      Arguments.of("objectObjectObjectField->property1->innerProperty1", true),
      Arguments.of("objectObjectObjectField->property1->innerProperty1->innerInnerProperty1", true),
      Arguments.of("objectObjectObjectField->property1->innerProperty1->innerInnerProperty1->foo", false),
      Arguments.of("objectObjectObjectField->property1->innerProperty1->foo", false),
      Arguments.of("arrayField", true),
      Arguments.of("arrayField->foo", false),
      Arguments.of("arrayField[*]", false), // disallowed to have [*] with no property
      Arguments.of("arrayField[*]->foo", false),
      Arguments.of("arrayObjectField", true),
      Arguments.of("arrayObjectField->property1", false),
      Arguments.of("arrayObjectField[*]", false), // disallowed to have [*] with no property
      Arguments.of("arrayObjectField[*]->property1", true),
      Arguments.of("arrayObjectField[*]->property1->foo", false),
      Arguments.of("arrayObjectField[*]->property2", false),
      Arguments.of("arrayArrayObjectField", true),
      Arguments.of("arrayArrayObjectField->property1", false),
      Arguments.of("arrayArrayObjectField[*]", false), // disallowed to have [*] with no property
      Arguments.of("arrayArrayObjectField[*][*]", false), // disallowed to have [*] with no property
      Arguments.of("arrayArrayObjectField[*][*]->property1", true),
      Arguments.of("arrayArrayObjectField[*][*]->property1->foo", false),
      Arguments.of("arrayArrayObjectField[*][*]->property2", false),
      Arguments.of("jsonbArrayField", true),
      Arguments.of("jsonbArrayField->foo", false),
      Arguments.of("jsonbArrayField[*]", false), // disallowed to have [*] with no property
      Arguments.of("jsonbArrayField[*]->foo", false),
      Arguments.of("jsonbArrayObjectField", true),
      Arguments.of("jsonbArrayObjectField->property1", false),
      Arguments.of("jsonbArrayObjectField[*]", false), // disallowed to have [*] with no property
      Arguments.of("jsonbArrayObjectField[*]->property1", true),
      Arguments.of("jsonbArrayObjectField[*]->property1->foo", false),
      Arguments.of("jsonbArrayObjectField[*]->property2", false),
      Arguments.of("jsonbArrayJsonbArrayObjectField", true),
      Arguments.of("jsonbArrayJsonbArrayObjectField->property1", false),
      Arguments.of("jsonbArrayJsonbArrayObjectField[*]", false), // disallowed to have [*] with no property
      Arguments.of("jsonbArrayJsonbArrayObjectField[*][*]", false), // disallowed to have [*] with no property
      Arguments.of("jsonbArrayJsonbArrayObjectField[*][*]->property1", true),
      Arguments.of("jsonbArrayJsonbArrayObjectField[*][*]->property1->foo", false),
      Arguments.of("jsonbArrayJsonbArrayObjectField[*][*]->property2", false)
    );
  }

  @ParameterizedTest
  @MethodSource("nestedFieldValidityParameters")
  void testNestedFieldValidity(String fieldName, boolean expectedValidity) {
    Map<String, String> actualErrors = fqlValidationService.validateFql(entityType, "{ \"%s\": { \"$eq\": true } }".formatted(fieldName));
    assertEquals(expectedValidity, actualErrors.isEmpty());
  }

  static List<Arguments> idColumnResolutionParameters() {
    return List.of(
      Arguments.of("hasValidIdColumn", "field1"),
      Arguments.of("objectField->hasValidIdColumn", "field1"),
      Arguments.of("objectField->hasNestedIdColumn", "objectField->property1")
    );
  }

  @ParameterizedTest
  @MethodSource("idColumnResolutionParameters")
  void testIdColumnResolution(String search, String expectedResolution) {
    assertEquals(
      FqlValidationService.findFieldDefinition(new FqlField(expectedResolution), entityType).get(),
      FqlValidationService.findFieldDefinitionForQuerying(new FqlField(search), entityType).get()
    );
  }

  static List<Arguments> idColumnResolutionNonexistentParameters() {
    return List.of(
      Arguments.of("hasInvalidIdColumn"),
      Arguments.of("objectField->hasInvalidIdColumn")
    );
  }

  @ParameterizedTest
  @MethodSource("idColumnResolutionNonexistentParameters")
  void testIdColumnResolutionNonexistent(String search) {
    assertTrue(FqlValidationService.findFieldDefinitionForQuerying(new FqlField(search), entityType).isEmpty());
  }

  // Entity type that supports dynamic MARC fields, signalled by the hidden generic "marc" placeholder.
  private static final EntityType marcEntityType = new EntityType()
    .name("marc_entity_type")
    .columns(
      List.of(
        new EntityTypeColumn().name("field1").dataType(new StringType()),
        new EntityTypeColumn().name("marc").dataType(new MarcType().dataType("marcType"))
      )
    );

  static List<String> validMarcFieldNames() {
    return List.of(
      "marc_245",              // tag-only (data field)
      "marc_008",              // tag-only (control field)
      "marc_245_a",            // subfield
      "marc_245_ind1",         // indicator-only
      "marc_245_ind2",         // indicator-only
      "marc_245_ind1_7_a",     // constrained subfield (alphanumeric indicator)
      "marc_245_ind1_blank_a", // constrained subfield (blank indicator)
      "MARC_245_A"             // case-insensitive
    );
  }

  @ParameterizedTest
  @MethodSource("validMarcFieldNames")
  void shouldValidateMarcFieldWhenPlaceholderPresent(String fieldName) {
    Map<String, String> actualErrors =
      fqlValidationService.validateFql(marcEntityType, "{ \"%s\": { \"$eq\": \"x\" } }".formatted(fieldName));
    assertEquals(Map.of(), actualErrors);
  }

  @ParameterizedTest
  @MethodSource("validMarcFieldNames")
  void shouldRejectMarcFieldWhenNoPlaceholder(String fieldName) {
    // Same MARC names, but against an entity type without the marc placeholder: still unknown fields.
    Map<String, String> actualErrors =
      fqlValidationService.validateFql(entityType, "{ \"%s\": { \"$eq\": \"x\" } }".formatted(fieldName));
    assertEquals(1, actualErrors.size());
    assertTrue(actualErrors.containsKey(fieldName));
  }

  static List<String> malformedMarcFieldNames() {
    return List.of(
      "marc_24",               // tag too short
      "marc_2451",             // tag too long
      "marc_001_a",            // control field with subfield
      "marc_008_ind1",         // control field with indicator
      "marc_245_ind3",         // invalid indicator number
      "marc_245_ind1_ab_a",    // multi-char indicator value
      "marc_"                  // no tag
    );
  }

  @ParameterizedTest
  @MethodSource("malformedMarcFieldNames")
  void shouldRejectMalformedMarcFieldEvenWithPlaceholder(String fieldName) {
    Map<String, String> actualErrors =
      fqlValidationService.validateFql(marcEntityType, "{ \"%s\": { \"$eq\": \"x\" } }".formatted(fieldName));
    assertEquals(1, actualErrors.size());
    assertTrue(actualErrors.containsKey(fieldName));
  }
}
