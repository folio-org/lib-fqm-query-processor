package org.folio.fql.deserializer;

import org.folio.fql.model.ContainsAllCondition;
import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerContainsAllTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetSimpleContainsAllFqlWithSingleStringValue() {
    String simpleContainsJson =
      """
         {"arrayField1": {"$contains_all": ["value"]}}
        """;
    FqlCondition<?> fqlCondition = new ContainsAllCondition(new FqlField("arrayField1"), List.of("value"));
    Fql expectedFql = new Fql("0", fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleContainsAllFqlWithSingleIntegerValue() {
    String simpleContainsJson =
      """
        {"arrayField1": {"$contains_all": [11]}}
        """;
    FqlCondition<?> fqlCondition = new ContainsAllCondition(new FqlField("arrayField1"), List.of(11));
    Fql expectedFql = new Fql("0", fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleContainsAllFqlWithMultipleStringValues() {
    String simpleContainsJson =
      """
         {"arrayField1": {"$contains_all": ["value1", "value2"]}}
        """;
    FqlCondition<?> fqlCondition = new ContainsAllCondition(new FqlField("arrayField1"), List.of("value1", "value2"));
    Fql expectedFql = new Fql("0", fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleContainsAllFqlWithMultipleIntegerValues() {
    String simpleContainsJson =
      """
        {"arrayField1": {"$contains_all": [11, 22, 19]}}
        """;
    FqlCondition<?> fqlCondition = new ContainsAllCondition(new FqlField("arrayField1"), List.of(11, 22, 19));
    Fql expectedFql = new Fql("0", fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldThrowExceptionWhenContainsAllConditionIsNotArray() {
    String invalidContainsConditionJson =
      """
        {"arrayField1": {"$contains_all": "value1" }}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidContainsConditionJson));
  }
}
