package org.folio.fql.deserializer;

import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.StartsWithCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerStartsWithTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetSimpleFqlWithStringValue() {
    String simpleStringJson =
      """
         {"field1": {"$starts_with": "value"}}
        """;
    FqlCondition<?> fqlCondition = new StartsWithCondition(new FqlField("field1"), "value");
    Fql expectedFql = new Fql("0", fqlCondition);
    Fql actualFql = fqlService.getFql(simpleStringJson);
    System.out.println(fqlCondition.value());
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldThrowExceptionWithBooleanValue() {
    String simpleBooleanJson =
      """
        {"field1": {"$starts_with": true}}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(simpleBooleanJson));
  }

  @Test
  void shouldThrowExceptionWithIntegerValue() {
    String simpleIntegerJson =
      """
        {"field1": {"$starts_with": 11}}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(simpleIntegerJson));
  }

  @Test
  void shouldThrowExceptionWhenStartsWithConditionIsNotValueNode() {
    String invalidStartsWithConditionJson =
      """
        {"field1": {"$starts_with": ["value1", "value2", "value3"]}}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidStartsWithConditionJson));
  }
}

