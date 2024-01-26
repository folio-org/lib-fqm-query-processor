package org.folio.fql.deserializer;

import org.folio.fql.model.EmptyCondition;
import org.folio.fql.model.EqualsCondition;
import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerEmptyTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetSimpleEmptyCondition() {
    String simpleStringJson =
      """
         {"field1": {"$empty": true}}
        """;
    FqlCondition<?> fqlCondition = new EmptyCondition("field1", true);
    Fql expectedFql = new Fql(fqlCondition);Fql actualFql = fqlService.getFql(simpleStringJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldGetSimpleNotEmptyCondition() {
    String simpleStringJson =
      """
         {"field1": {"$empty": false}}
        """;
    FqlCondition<?> fqlCondition = new EmptyCondition("field1", false);
    Fql expectedFql = new Fql(fqlCondition);Fql actualFql = fqlService.getFql(simpleStringJson);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldThrowExceptionWhenEmptyConditionIsNotBoolean() {
    String invalidEqualsConditionJson =
      """
        {"field1": {"$empty": "maybe"}}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidEqualsConditionJson));
  }
}
