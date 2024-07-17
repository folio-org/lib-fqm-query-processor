package org.folio.fql.deserializer;

import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.EqualsCondition;
import org.folio.fql.model.InCondition;
import org.folio.fql.model.field.FqlField;
import org.folio.fql.model.AndCondition;
import org.folio.fql.service.FqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FqlDeserializerTest {

  private FqlService fqlService;

  @BeforeEach
  void setup() {
    fqlService = new FqlService();
  }

  @Test
  void shouldGetComplexFql() {
    String complexFqlCriteria =
      """
            {
               "$and": [
                       {"field1" : {"$eq": "some value"}},
                       {"field2" : {"$eq": 2}},
                       {
                            "$and": [
                                {"field3" : {"$eq": "another value"}},
                                {"field4" : {"$eq": false}}
                            ]
                       },
                       {"field5" : {"$in": ["value1", 2, false]}},
                       {"field6->nested" : {"$eq": "nested value"}}
               ]
            }
        """;
    List<Object> inValues = List.of("value1", 2, false);
    List<FqlCondition<?>> innerFqlConditions = List.of(
      new EqualsCondition(new FqlField("field3"), "another value"),
      new EqualsCondition(new FqlField("field4"), false)
    );
    AndCondition innerAndCondition = new AndCondition(innerFqlConditions);
    List<FqlCondition<?>> outerFqlConditions = List.of(
      new EqualsCondition(new FqlField("field1"), "some value"),
      new EqualsCondition(new FqlField("field2"), 2),
      innerAndCondition,
      new InCondition(new FqlField("field5"), inValues),
      new EqualsCondition(new FqlField("field6->nested"), "nested value")
    );
    AndCondition outerAndCondition = new AndCondition(outerFqlConditions);
    Fql expectedFql = new Fql("0", outerAndCondition);
    Fql actualFql = fqlService.getFql(complexFqlCriteria);
    assertEquals(expectedFql, actualFql);
  }

  @Test
  void shouldThrowExceptionForInvalidFqlCriteria() {
    String invalidFqlCriteria =
      """
            {
               "$and": [
                       {"field1" : {"$eq": "some value"}},
                       {"field2" : {"$invalidOperator": true}}
               ]
            }
        """;
    assertThrows(FqlParsingException.class, () -> fqlService.getFql(invalidFqlCriteria));
  }
}
