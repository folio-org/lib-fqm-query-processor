package org.folio.fql.deserializer;

import org.folio.fql.model.NotContainsAllCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerNotContainsAllTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetSimpleNotContainsAllFqlWithSingleStringValue() {
    String simpleContainsJson =
      """
         {"arrayField1": {"$not_contains_all": ["value"]}}
        """;
    FqlCondition<?> fqlCondition = new NotContainsAllCondition(new FqlField("arrayField1"), List.of("value"));
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleNotContainsAllFqlWithSingleIntegerValue() {
    String simpleContainsJson =
      """
        {"arrayField1": {"$not_contains_all": [11]}}
        """;
    FqlCondition<?> fqlCondition = new NotContainsAllCondition(new FqlField("arrayField1"), List.of(11));
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleContainsAllFqlWithMultipleStringValues() {
    String simpleContainsJson =
      """
         {"arrayField1": {"$not_contains_all": ["value1", "value2"]}}
        """;
    FqlCondition<?> fqlCondition = new NotContainsAllCondition(new FqlField("arrayField1"), List.of("value1", "value2"));
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleContainsAllFqlWithMultipleIntegerValues() {
    String simpleContainsJson =
      """
        {"arrayField1": {"$not_contains_all": [11, 22, 19]}}
        """;
    FqlCondition<?> fqlCondition = new NotContainsAllCondition(new FqlField("arrayField1"), List.of(11, 22, 19));
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldThrowExceptionWhenContainsAllConditionIsNotArray() {
    String invalidContainsConditionJson =
      """
        {"arrayField1": {"$not_contains_all": "value1" }}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidContainsConditionJson));
  }
}
