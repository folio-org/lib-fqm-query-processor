package org.folio.fql.deserializer;

import org.folio.fql.model.ContainsCondition;
import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FqlDeserializerContainsTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetSimpleContainsFqlWithStringValue() {
    String simpleContainsJson =
      """
         {"arrayField1": {"$contains": "value"}}
        """;
    FqlCondition<?> fqlCondition = new ContainsCondition("arrayField1", "value");
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleContainsFqlWithIntegerValue() {
    String simpleContainsJson =
      """
        {"arrayField1": {"$contains": 11}}
        """;
    FqlCondition<?> fqlCondition = new ContainsCondition("arrayField1", 11);
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleContainsJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldThrowExceptionWhenContainsConditionIsNotValueNode() {
    String invalidEqualsConditionJson =
      """
        {"arrayField1": {"$contains": ["value1", "value2", "value3" ] }}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidEqualsConditionJson));
  }
}
