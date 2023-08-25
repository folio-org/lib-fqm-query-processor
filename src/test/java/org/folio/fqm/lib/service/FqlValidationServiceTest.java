package org.folio.fqm.lib.service;

import org.folio.fql.FqlService;
import org.folio.fqm.lib.repository.MetaDataRepository;
import org.folio.querytool.domain.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FqlValidationServiceTest {

  private static final String TENANT_ID = "tenant_01";

  private static final UUID ENTITY_TYPE_ID = UUID.randomUUID();

  private static final EntityType entityType = new EntityType()
    .name("test_entity_type")
    .columns(
      List.of(
        new EntityTypeColumn().name("field1").dataType(new StringType()),
        new EntityTypeColumn().name("field2").dataType(new StringType()),
        new EntityTypeColumn().name("field3").dataType(new StringType())
      )
    );

  private FqlValidationService fqlValidationService;

  @BeforeEach
  public void setup() {
    MetaDataRepository metaDataRepository = mock(MetaDataRepository.class);
    this.fqlValidationService = new FqlValidationService(new FqlService(), metaDataRepository);
    when(metaDataRepository.getEntityTypeDefinition(TENANT_ID, ENTITY_TYPE_ID)).thenReturn(Optional.of(entityType));
  }

  @Test
  void shouldValidateFql() {
    String fqlCondition = """
      {"field1": {"$eq": "some value"}}
      """;
    Map<String, String> expectedErrors = Map.of();
    Map<String, String> actualErrors = fqlValidationService.validateFql(TENANT_ID, ENTITY_TYPE_ID, fqlCondition);
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
    Map<String, String> actualErrors = fqlValidationService.validateFql(TENANT_ID, ENTITY_TYPE_ID, complexFql);
    assertEquals(expectedErrors, actualErrors);
  }

  @Test
  void shouldReturnErrorForInvalidFqlSyntax() {
    String fqlCondition = """
      {"field1": {"$eq": "some value"}
      """;
    Map<String, String> actualErrors = fqlValidationService.validateFql(TENANT_ID, ENTITY_TYPE_ID, fqlCondition);
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
    Map<String, String> actualErrors = fqlValidationService.validateFql(TENANT_ID, ENTITY_TYPE_ID, fqlCondition);
    assertEquals(expectedErrors, actualErrors);
  }

  @Test
  void shouldReturnErrorWhenFieldMissingFromEntityType() {
    String fqlCondition = """
      {"field4": {"$eq": "some value"}}
      """;
    String expectedErrorMessage = "Field field4 is not present in definition of entity type " + entityType.getName();
    Map<String, String> expectedErrors = Map.of("field4", expectedErrorMessage);
    Map<String, String> actualErrors = fqlValidationService.validateFql(TENANT_ID, ENTITY_TYPE_ID, fqlCondition);
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
    Map<String, String> actualErrors = fqlValidationService.validateFql(TENANT_ID, ENTITY_TYPE_ID, fqlCondition);
    assertEquals(expectedErrors, actualErrors);
  }
}
