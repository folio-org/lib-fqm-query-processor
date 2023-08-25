package org.folio.fqm.lib.util;

import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.EntityTypeDefaultSort;
import org.folio.querytool.domain.dto.StringType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.folio.fqm.lib.repository.MetaDataRepository.ID_FIELD_NAME;

public class TestDataFixture {
  private static final String derivedTableName = "testTable";

  private static final List<Map<String, Object>> entityContents = List.of(
    Map.of(ID_FIELD_NAME, UUID.randomUUID(), "key1", "value1", "key2", "value2"),
    Map.of(ID_FIELD_NAME, UUID.randomUUID(), "key1", "value3", "key2", "value4"),
    Map.of(ID_FIELD_NAME, UUID.randomUUID(), "key1", "value5", "key2", "value6")
  );

  private static final EntityTypeColumn col1 = new EntityTypeColumn()
    .name(ID_FIELD_NAME)
    .dataType(new StringType().dataType("stringType"))
    .labelAlias("column_001_label")
    .visibleByDefault(false);

  private static final EntityTypeColumn col2 = new EntityTypeColumn()
    .name("key1")
    .dataType(new StringType().dataType("stringType"))
    .labelAlias("key1_label")
    .visibleByDefault(false);

  private static final EntityTypeColumn col3 = new EntityTypeColumn()
    .name("key2")
    .dataType(new StringType().dataType("stringType"))
    .labelAlias("key2_label")
    .visibleByDefault(false);

  private static final EntityTypeDefaultSort sort = new EntityTypeDefaultSort().columnName(col1.getName());

  private static final EntityType mockDefinition = new EntityType()
    .id(UUID.randomUUID().toString())
    .name(derivedTableName)
    .labelAlias("derived_table_alias_01")
    .columns(List.of(col1, col2, col3))
    .defaultSort(List.of(sort));

  public static List<Map<String, Object>> getEntityContents() {
    return entityContents;
  }

  public static EntityType getEntityDefinition() {
    return mockDefinition;
  }

  public static EntityType getEntityTypeDefinition(String name,
                                                   List<EntityTypeColumn> columns,
                                                   List<EntityTypeDefaultSort> sorts,
                                                   List<UUID> subEntityTypes) {
    return new EntityType()
      .id(UUID.randomUUID().toString())
      .name(name)
      .labelAlias(name)
      .columns(columns)
      .defaultSort(sorts)
      .subEntityTypeIds(subEntityTypes);
  }

  public static EntityTypeColumn column1() {
    return col1;
  }

  public static EntityTypeColumn column2() {
    return col2;
  }

  public static EntityTypeColumn column3() {
    return col3;
  }
}
