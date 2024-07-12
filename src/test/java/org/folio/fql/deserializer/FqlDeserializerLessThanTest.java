package org.folio.fql.deserializer;

import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.LessThanCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerLessThanTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetSimpleFqlWithLtStringValue() {
    String simpleStringJson =
      """
         {"field1": {"$lt": "value"}}
        """;
    FqlCondition<?> fqlCondition = new LessThanCondition(new FqlField("field1"), false, "value");
    Fql expectedFql = new Fql(0,fqlCondition);
    Fql actualFql = fqlService.getFql(simpleStringJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleFqlWithLteStringValue() {
    String simpleStringJson =
      """
         {"field1": {"$lte": "value"}}
        """;
    FqlCondition<?> fqlCondition = new LessThanCondition(new FqlField("field1"), true, "value");
    Fql expectedFql = new Fql(0, fqlCondition);
    Fql actualFql = fqlService.getFql(simpleStringJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldFailForInvalidGtValue() {
    String invalidGtConditionJson =
      """
         {"field1": {"$lt": ["value1", "value2", "value3" ]}}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidGtConditionJson));
  }

  @Test
  void shouldFailForInvalidGteValue() {
    String invalidGteConditionJson =
      """
         {"field1": {"$lte": ["value1", "value2", "value3" ]}}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidGteConditionJson));
  }

  @Test
  void shouldGetSimpleFqlWithLtIntegerValue() {
    String simpleStringJson =
      """
         {"field1": {"$lt": 10}}
        """;
    FqlCondition<?> fqlCondition = new LessThanCondition(new FqlField("field1"), false, 10);
    Fql expectedFql = new Fql(0,fqlCondition);
    Fql actualFql = fqlService.getFql(simpleStringJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleFqlWithLteIntegerValue() {
    String simpleStringJson =
      """
         {"field1": {"$lte": 10}}
        """;
    FqlCondition<?> fqlCondition = new LessThanCondition(new FqlField("field1"), true, 10);
    Fql expectedFql = new Fql(0, fqlCondition);
    Fql actualFql = fqlService.getFql(simpleStringJson);
    assertEquals(expectedFql, actualFql);
  }
}
