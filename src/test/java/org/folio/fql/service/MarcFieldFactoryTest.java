package org.folio.fql.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.folio.fql.model.field.MarcFieldName;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.Field;
import org.folio.querytool.domain.dto.MarcType;
import org.folio.querytool.domain.dto.StringType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MarcFieldFactoryTest {

  private static EntityType marcEntityType() {
    return new EntityType()
      .name("marc_entity_type")
      .columns(List.of(
        new EntityTypeColumn().name("field1").dataType(new StringType()),
        new EntityTypeColumn().name("marc").dataType(new MarcType().dataType("marcType"))
      ));
  }

  private static EntityType nonMarcEntityType() {
    return new EntityType()
      .name("plain_entity_type")
      .columns(List.of(new EntityTypeColumn().name("field1").dataType(new StringType())));
  }

  @ParameterizedTest
  @CsvSource({
    // fieldName,             tag, subfield, indNumber, indValue, labelAlias
    "marc_245,               245, ,         ,          ,         MARC 245",
    "marc_008,               008, ,         ,          ,         MARC 008",
    "marc_245_a,             245, a,        ,          ,         MARC 245$a",
    "MARC_245_A,             245, a,        ,          ,         MARC 245$a",
    "marc_245_ind1,          245, ,         1,         ,         MARC 245 ind1",
    "marc_245_ind2,          245, ,         2,         ,         MARC 245 ind2",
    "marc_245_ind1_7_a,      245, a,        1,         7,        MARC 245 ind1=7 $a",
    "marc_245_ind1_blank_a,  245, a,        1,         #,        MARC 245 ind1=blank $a",
    "marc_245_ind2_X_b,      245, b,        2,         x,        MARC 245 ind2=x $b"
  })
  void shouldParseSupportedForms(String fieldName, String tag, String subfield, String indNumber,
                                 String indValue, String labelAlias) {
    MarcFieldName parsed = MarcFieldFactory.parse(fieldName).orElseThrow();
    assertEquals(fieldName, parsed.fieldName());
    assertEquals(tag, parsed.tag());
    assertEquals(emptyToNull(subfield), parsed.subfield());
    assertEquals(emptyToNull(indNumber), parsed.indicatorNumber());
    assertEquals(emptyToNull(indValue), parsed.indicatorValue());
    assertEquals(labelAlias, parsed.labelAlias());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "marc_24",             // tag too short
    "marc_2451",           // tag too long
    "marc_001_a",          // control field with subfield
    "marc_009_b",          // control field with subfield
    "marc_008_ind1",       // control field with indicator
    "marc_245_ind3",       // invalid indicator number
    "marc_245_ind1_ab_a",  // multi-char indicator value
    "marc_245_ind1_7_ab",  // multi-char subfield
    "marc_",               // no tag
    "not_a_marc_field"
  })
  void shouldRejectMalformedFieldNames(String fieldName) {
    assertTrue(MarcFieldFactory.parse(fieldName).isEmpty());
    assertFalse(MarcFieldFactory.isMarcFieldName(fieldName));
  }

  @Test
  void shouldTreatNullAsNonMarc() {
    assertTrue(MarcFieldFactory.parse(null).isEmpty());
    assertFalse(MarcFieldFactory.isMarcFieldName(null));
  }

  @ParameterizedTest
  @CsvSource({
    "marc_245_ind1, true",   // indicator-only targets the indicator
    "marc_245_ind1_7_a, false", // constrained subfield targets the subfield
    "marc_245_a, false",
    "marc_245, false"
  })
  void shouldIdentifyIndicatorTarget(String fieldName, boolean isIndicatorTarget) {
    assertEquals(isIndicatorTarget, MarcFieldFactory.parse(fieldName).orElseThrow().isIndicatorTarget());
  }

  @Test
  void toColumnProducesMetadataOnlyMarcColumn() {
    MarcFieldName parsed = MarcFieldFactory.parse("marc_245_a").orElseThrow();
    EntityTypeColumn column = MarcFieldFactory.toColumn(parsed);

    assertEquals("marc_245_a", column.getName());
    assertEquals("MARC 245$a", column.getLabelAlias());
    assertTrue(column.getDataType() instanceof MarcType);
    assertEquals(Boolean.TRUE, column.getQueryable());
    assertEquals(Boolean.FALSE, column.getVisibleByDefault());
    assertEquals(Boolean.FALSE, column.getEssential());
    // Metadata only: no SQL value getters are produced by the lib.
    assertNull(column.getValueGetter());
    assertNull(column.getFilterValueGetter());
  }

  @Test
  void findMarcPlaceholderDetectsGenericColumn() {
    assertTrue(MarcFieldFactory.findMarcPlaceholder(marcEntityType()).isPresent());
    assertTrue(MarcFieldFactory.findMarcPlaceholder(nonMarcEntityType()).isEmpty());
  }

  @Test
  void findMarcPlaceholderReturnsEmptyForNullEntityTypeOrColumns() {
    assertTrue(MarcFieldFactory.findMarcPlaceholder(null).isEmpty());
    assertTrue(MarcFieldFactory.findMarcPlaceholder(new EntityType().columns(null)).isEmpty());
  }

  @Test
  void isGenericMarcPlaceholderRequiresNameAndMarcType() {
    assertTrue(MarcFieldFactory.isGenericMarcPlaceholder(
      new EntityTypeColumn().name("marc").dataType(new MarcType().dataType("marcType"))));
    // Right name, wrong type.
    assertFalse(MarcFieldFactory.isGenericMarcPlaceholder(
      new EntityTypeColumn().name("marc").dataType(new StringType())));
    // Right type, wrong name.
    assertFalse(MarcFieldFactory.isGenericMarcPlaceholder(
      new EntityTypeColumn().name("notmarc").dataType(new MarcType().dataType("marcType"))));
    assertFalse(MarcFieldFactory.isGenericMarcPlaceholder(null));
  }

  @Test
  void resolveMarcFieldRequiresPlaceholderAndValidName() {
    // Valid MARC name + placeholder present -> resolves to a synthetic column.
    Optional<Field> resolved = MarcFieldFactory.resolveMarcField("marc_245_a", marcEntityType());
    assertTrue(resolved.isPresent());
    assertEquals("marc_245_a", ((EntityTypeColumn) resolved.get()).getName());

    // No placeholder -> not resolvable, even for a valid MARC name.
    assertTrue(MarcFieldFactory.resolveMarcField("marc_245_a", nonMarcEntityType()).isEmpty());

    // Placeholder present but malformed name -> not resolvable.
    assertTrue(MarcFieldFactory.resolveMarcField("marc_24", marcEntityType()).isEmpty());
  }

  @Test
  void getReferencedMarcFieldNamesScansRawQueryKeys() {
    String rawQuery = """
      {
        "$and": [
          { "marc_245_a": { "$eq": "x" } },
          { "field1": { "$eq": "y" } },
          { "marc_008": { "$empty": false } }
        ]
      }
      """;
    assertEquals(List.of("marc_245_a", "marc_008"),
      List.copyOf(MarcFieldFactory.getReferencedMarcFieldNames(rawQuery)));
  }

  @Test
  void getReferencedMarcFieldNamesHandlesBlankInput() {
    assertTrue(MarcFieldFactory.getReferencedMarcFieldNames(null).isEmpty());
    assertTrue(MarcFieldFactory.getReferencedMarcFieldNames("   ").isEmpty());
  }

  @Test
  void getReferencedFieldNamesReturnsSingleFieldCondition() {
    FqlService fqlService = new FqlService();
    var condition = fqlService.getFql("{ \"marc_245_a\": { \"$eq\": \"x\" } }").fqlCondition();
    assertEquals(Set.of("marc_245_a"), MarcFieldFactory.getReferencedFieldNames(condition));
  }

  @Test
  void getReferencedFieldNamesRecursesIntoAndCondition() {
    FqlService fqlService = new FqlService();
    // Collects every referenced field, MARC or not, across the nested $and tree.
    String andQuery = """
      {
        "$and": [
          { "marc_245_a": { "$eq": "x" } },
          { "field1": { "$eq": "y" } },
          { "$and": [ { "marc_008": { "$empty": false } } ] }
        ]
      }
      """;
    var condition = fqlService.getFql(andQuery).fqlCondition();
    assertEquals(Set.of("marc_245_a", "field1", "marc_008"),
      MarcFieldFactory.getReferencedFieldNames(condition));
  }

  private static String emptyToNull(String value) {
    return value == null || value.isEmpty() ? null : value;
  }
}
