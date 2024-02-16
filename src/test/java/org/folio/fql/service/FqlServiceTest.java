package org.folio.fql.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.folio.fql.model.*;
import org.folio.fql.model.field.FqlField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FqlServiceTest {

  private FqlService fqlService;

  @BeforeEach
  void setUp() {
    this.fqlService = new FqlService();
  }

  @Test
  void shouldReturnFieldsForValidFqlFieldCondition() {
    EqualsCondition equalsCondition = new EqualsCondition(new FqlField("field1->nested"), "value1");
    Fql fql = new Fql(equalsCondition);
    List<String> expectedList = List.of("field1->nested");
    List<String> actualList = fqlService.getFqlFields(fql).stream().map(FqlField::serialize).toList();
    assertEquals(expectedList, actualList);
  }

  @Test
  void shouldReturnFieldsForValidFqlAndCondition() {
    AndCondition andCondition = new AndCondition(
      List.of(
        new EqualsCondition(new FqlField("field1"), "value1"),
        new NotEqualsCondition(new FqlField("field2"), "value2"),
        new GreaterThanCondition(new FqlField("field3"), false, 10)
      )
    );
    Fql fql = new Fql(andCondition);
    List<String> expectedList = List.of("field1", "field2", "field3");
    List<String> actualList = fqlService.getFqlFields(fql).stream().map(FqlField::serialize).toList();
    assertEquals(expectedList, actualList);
  }
}
