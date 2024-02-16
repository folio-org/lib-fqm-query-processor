package org.folio.fql.deserializer;

import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.GreaterThanCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerGreaterThanTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetSimpleFqlWithGtStringValue() {
    String simpleStringJson =
      """
         {"field1": {"$gt": "value"}}
        """;
    FqlCondition<?> fqlCondition = new GreaterThanCondition(new FqlField("field1"), false, "value");
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleStringJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleFqlWithGteStringValue() {
    String simpleStringJson =
      """
         {"field1": {"$gte": "value"}}
        """;
    FqlCondition<?> fqlCondition = new GreaterThanCondition(new FqlField("field1"), true, "value");
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleStringJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldFailForInvalidGtValue() {
    String invalidGtConditionJson =
      """
         {"field1": {"$gt": ["value1", "value2", "value3" ]}}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidGtConditionJson));
  }

  @Test
  void shouldFailForInvalidGteValue() {
    String invalidGteConditionJson =
      """
         {"field1": {"$gte": ["value1", "value2", "value3" ]}}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidGteConditionJson));
  }

  @Test
  void shouldGetSimpleFqlWithGtIntegerValue() {
    String simpleStringJson =
      """
         {"field1": {"$gt": 10}}
        """;
    FqlCondition<?> fqlCondition = new GreaterThanCondition(new FqlField("field1"), false, 10);
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleStringJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleFqlWithGteIntegerValue() {
    String simpleStringJson =
      """
         {"field1": {"$gte": 10}}
        """;
    FqlCondition<?> fqlCondition = new GreaterThanCondition(new FqlField("field1"), true, 10);
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleStringJson);
    assertEquals(expectedFql, actualFql);
  }
}
