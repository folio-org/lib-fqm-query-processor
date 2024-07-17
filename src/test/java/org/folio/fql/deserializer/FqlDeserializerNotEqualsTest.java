package org.folio.fql.deserializer;

import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.NotEqualsCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerNotEqualsTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetSimpleFqlWithStringValue() {
    String simpleStringJson =
      """
         {"field1": {"$ne": "value"}}
        """;
    FqlCondition<?> fqlCondition = new NotEqualsCondition(new FqlField("field1"), "value");
    Fql expectedFql = new Fql("0", fqlCondition);
    Fql actualFql = fqlService.getFql(simpleStringJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleFqlWithBooleanValue() {
    String simpleBooleanJson =
      """
        {"field1": {"$ne": true}}
        """;
    FqlCondition<?> fqlCondition = new NotEqualsCondition(new FqlField("field1"), true);
    Fql expectedFql = new Fql("0", fqlCondition);
    Fql actualFql = fqlService.getFql(simpleBooleanJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleFqlWithIntegerValue() {
    String simpleIntegerJson =
      """
        {"field1": {"$ne": 11}}
        """;
    FqlCondition<?> fqlCondition = new NotEqualsCondition(new FqlField("field1"), 11);
    Fql expectedFql = new Fql("0", fqlCondition);
    Fql actualFql = fqlService.getFql(simpleIntegerJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldThrowExceptionWhenNotEqualsConditionIsNotValueNode() {
    String invalidNotEqualsConditionJson =
      """
        {"field1": {"$ne": ["value1", "value2", "value3" ] }}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidNotEqualsConditionJson));
  }
}
