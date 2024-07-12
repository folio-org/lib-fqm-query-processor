package org.folio.fql.deserializer;

import org.folio.fql.model.EmptyCondition;
import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerEmptyTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  static List<Arguments> emptyConditionsSource() {
    return List.of(
      Arguments.of(
        "empty with boolean value",
        """
        {"field1": {"$empty": true}}""",
        new Fql(0, new EmptyCondition(new FqlField("field1"), true))
      ),
      Arguments.of(
        "not empty with boolean value",
        """
        {"field1": {"$empty": false}}""",
        new Fql(0, new EmptyCondition(new FqlField("field1"), false))
      ),
      Arguments.of(
        "empty with string value",
        """
        {"field1": {"$empty": "true"}}""",
        new Fql(0, new EmptyCondition(new FqlField("field1"), true))
      ),
      Arguments.of(
        "not empty with string value",
          """
          {"field1": {"$empty": "false"}}""",
        new Fql(0, new EmptyCondition(new FqlField("field1"), false))
      )
    );
  }

  @ParameterizedTest
  @MethodSource("emptyConditionsSource")
  void shouldGetEmptyConditionForFqlString(String label, String fqlString, Fql expectedFql) {
    Fql actualFql = fqlService.getFql(fqlString);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldThrowExceptionWhenEmptyConditionIsNotBoolean() {
    String invalidEqualsConditionJson =
      """
        {"field1": {"$empty": "cat"}}
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidEqualsConditionJson));
  }
}
