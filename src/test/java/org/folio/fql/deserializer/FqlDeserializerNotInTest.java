package org.folio.fql.deserializer;

import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.NotInCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerNotInTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetSimpleFqlWithNotInCondition() {
    String simpleNotInConditionJson =
      """
        {"field1": {"$nin": ["value1", 11, false ] }}
        """;
    FqlCondition<?> fqlCondition = new NotInCondition(new FqlField("field1"), List.of("value1", 11, false));
    Fql expectedFql = new Fql(fqlCondition);
    Fql actualFql = fqlService.getFql(simpleNotInConditionJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldThrowExceptionWhenNotInConditionIsNotArrayNode() {
    String invalidNotInConditionJson =
      """
        {"field1": {"$nin": "value1" }}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidNotInConditionJson));
  }
}
