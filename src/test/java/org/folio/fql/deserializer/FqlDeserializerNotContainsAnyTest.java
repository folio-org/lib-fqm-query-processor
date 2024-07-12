package org.folio.fql.deserializer;

import org.folio.fql.model.NotContainsAnyCondition;
import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerNotContainsAnyTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetSimpleNotContainsAnyFqlWithSingleStringValue() {
    String simpleNotContainsAnyJson =
      """
         {"arrayField1": {"$not_contains_any": ["value"]}}
        """;
    FqlCondition<?> fqlCondition = new NotContainsAnyCondition(new FqlField("arrayField1"), List.of("value"));
    Fql expectedFql = new Fql(0, fqlCondition);
    Fql actualFql = fqlService.getFql(simpleNotContainsAnyJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleNotContainsAnyFqlWithSingleIntegerValue() {
    String simpleNotContainsAnyJson =
      """
        {"arrayField1": {"$not_contains_any": [11]}}
        """;
    FqlCondition<?> fqlCondition = new NotContainsAnyCondition(new FqlField("arrayField1"), List.of(11));
    Fql expectedFql = new Fql(0, fqlCondition);
    Fql actualFql = fqlService.getFql(simpleNotContainsAnyJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleNotContainsAnyFqlWithMultipleStringValues() {
    String simpleNotContainsAnyJson =
      """
         {"arrayField1": {"$not_contains_any": ["value1", "value2"]}}
        """;
    FqlCondition<?> fqlCondition = new NotContainsAnyCondition(new FqlField("arrayField1"), List.of("value1", "value2"));
    Fql expectedFql = new Fql(0, fqlCondition);
    Fql actualFql = fqlService.getFql(simpleNotContainsAnyJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleNotContainsAnyFqlWithMultipleIntegerValues() {
    String simpleNotContainsAnyJson =
      """
        {"arrayField1": {"$not_contains_any": [11, 22, 19]}}
        """;
    FqlCondition<?> fqlCondition = new NotContainsAnyCondition(new FqlField("arrayField1"), List.of(11, 22, 19));
    Fql expectedFql = new Fql(0, fqlCondition);
    Fql actualFql = fqlService.getFql(simpleNotContainsAnyJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldThrowExceptionWhenNotContainsAnyConditionIsNotArray() {
    String invalidNotContainsAnyConditionJson =
      """
        {"arrayField1": {"$not_contains_any": "value1" }}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidNotContainsAnyConditionJson));
  }
}
