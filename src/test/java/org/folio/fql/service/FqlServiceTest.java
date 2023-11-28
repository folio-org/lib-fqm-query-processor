package org.folio.fql.service;

import org.folio.fql.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FqlServiceTest {
  private FqlService fqlService;

  @BeforeEach
  void setUp() {
    this.fqlService = new FqlService();
  }

  @Test
  void shouldReturnFieldsForValidFqlFieldCondition() {
    EqualsCondition equalsCondition = new EqualsCondition("field1", "value1");
    Fql fql = new Fql(equalsCondition);
    List<String> expectedList = List.of("field1");
    List<String> actualList = fqlService.getFqlFields(fql);
    assertEquals(expectedList, actualList);
  }

  @Test
  void shouldReturnFieldsForValidFqlAndCondition() {
    AndCondition andCondition = new AndCondition(List.of(
      new EqualsCondition("field1", "value1"),
      new NotEqualsCondition("field2", "value2"),
      new GreaterThanCondition("field3", false, 10)
    ));
    Fql fql = new Fql(andCondition);
    List<String> expectedList = List.of("field1", "field2", "field3");
    List<String> actualList = fqlService.getFqlFields(fql);
    assertEquals(expectedList, actualList);
  }
}
