package org.folio.fql;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.fql.config.FqlConfiguration;
import org.folio.fql.service.FqlService;
import org.folio.fql.service.FqlValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = { FqlConfiguration.class })
class SpringContextTest {

  @Autowired
  private FqlService fqlService;

  @Autowired
  private FqlValidationService fqlValidationService;

  @Test
  void testAutowiring() {
    assertNotNull(fqlService, "Autowired components should exist");
    assertNotNull(fqlValidationService, "Autowired components should exist");
  }
}
