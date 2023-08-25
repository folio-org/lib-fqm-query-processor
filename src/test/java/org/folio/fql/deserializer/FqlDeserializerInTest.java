package org.folio.fql.deserializer;

import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.InCondition;
import org.folio.fql.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerInTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetSimpleFqlWithInCondition() {
    String simpleInConditionJson =
      """
        {"field1": {"$in": ["value1", 11, false ] }}
        """;
    FqlCondition<?> fqlCondition = new InCondition("field1", List.of("value1", 11, false));
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleInConditionJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldThrowExceptionWhenInConditionIsNotArrayNode() {
    String invalidInConditionJson =
      """
        {"field1": {"$in": "value1" }}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidInConditionJson));
  }
}
