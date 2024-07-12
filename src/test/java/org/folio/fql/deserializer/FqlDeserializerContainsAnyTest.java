package org.folio.fql.deserializer;

import org.folio.fql.model.ContainsAnyCondition;
import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerContainsAnyTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetSimpleContainsAnyFqlWithSingleStringValue() {
    String simpleContainsAnyJson =
      """
         {"arrayField1": {"$contains_any": ["value"]}}
        """;
    FqlCondition<?> fqlCondition = new ContainsAnyCondition(new FqlField("arrayField1"), List.of("value"));
    Fql expectedFql = new Fql(0,fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsAnyJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleContainsAnyFqlWithSingleIntegerValue() {
    String simpleContainsAnyJson =
      """
        {"arrayField1": {"$contains_any": [11]}}
        """;
    FqlCondition<?> fqlCondition = new ContainsAnyCondition(new FqlField("arrayField1"), List.of(11));
    Fql expectedFql = new Fql(0,fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsAnyJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleContainsAnyFqlWithMultipleStringValues() {
    String simpleContainsAnyJson =
      """
         {"arrayField1": {"$contains_any": ["value1", "value2"]}}
        """;
    FqlCondition<?> fqlCondition = new ContainsAnyCondition(new FqlField("arrayField1"), List.of("value1", "value2"));
    Fql expectedFql = new Fql(0, fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsAnyJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleContainsAnyFqlWithMultipleIntegerValues() {
    String simpleContainsAnyJson =
      """
        {"arrayField1": {"$contains_any": [11, 22, 19]}}
        """;
    FqlCondition<?> fqlCondition = new ContainsAnyCondition(new FqlField("arrayField1"), List.of(11, 22, 19));
    Fql expectedFql = new Fql(0, fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsAnyJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldThrowExceptionWhenContainsAnyConditionIsNotArray() {
    String invalidContainsAnyConditionJson =
      """
        {"arrayField1": {"$contains_any": "value1" }}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidContainsAnyConditionJson));
  }
}
