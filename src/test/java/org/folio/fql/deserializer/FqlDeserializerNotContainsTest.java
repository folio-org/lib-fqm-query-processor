package org.folio.fql.deserializer;

import org.folio.fql.model.NotContainsCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerNotContainsTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetSimpleContainsFqlWithStringValue() {
    String simpleNotContainsJson =
      """
         {"arrayField1": {"$not_contains": "value"}}
        """;
    FqlCondition<?> fqlCondition = new NotContainsCondition(new FqlField("arrayField1"), "value");
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleNotContainsJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleContainsFqlWithIntegerValue() {
    String simpleNotContainsJson =
      """
        {"arrayField1": {"$not_contains": 11}}
        """;
    FqlCondition<?> fqlCondition = new NotContainsCondition(new FqlField("arrayField1"), 11);
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleNotContainsJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldThrowExceptionWhenContainsConditionIsNotValueNode() {
    String invalidNotContainsJson =
      """
        {"arrayField1": {"$not_contains": ["value1", "value2", "value3" ] }}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidNotContainsJson));
  }
}
