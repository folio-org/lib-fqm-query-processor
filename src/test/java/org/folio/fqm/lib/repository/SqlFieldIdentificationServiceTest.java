package org.folio.fqm.lib.repository;

import org.folio.querytool.domain.dto.ColumnValueGetter;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jooq.impl.DSL.field;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlFieldIdentificationServiceTest {

  private SqlFieldIdentificationService fieldIdentificationService;

  @BeforeEach
  void setup() {
    this.fieldIdentificationService = new SqlFieldIdentificationService();
  }

  @Test
  void testRmbJsonbColumn() {
    ColumnValueGetter columnValueGetter = new ColumnValueGetter()
      .type(ColumnValueGetter.TypeEnum.RMB_JSONB)
      .param("name");

    EntityTypeColumn entityTypeColumn = new EntityTypeColumn()
      .name("derived_column_name_01")
      .valueGetter(columnValueGetter);
    var actualField = fieldIdentificationService.getSqlField(entityTypeColumn);
    var expectedField = field("jsonb ->> 'name'");
    assertEquals(expectedField, actualField);
  }

  @Test
  void testSqlFieldWhenValueGetterMissing() {
    EntityTypeColumn entityTypeColumn = new EntityTypeColumn()
      .name("derived_column_name_01");
    var actualField = fieldIdentificationService.getSqlField(entityTypeColumn);
    var expectedField = field(entityTypeColumn.getName());
    assertEquals(expectedField, actualField);
  }
}
