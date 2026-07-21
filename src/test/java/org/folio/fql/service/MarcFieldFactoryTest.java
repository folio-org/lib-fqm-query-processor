package org.folio.fql.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
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

  private static EntityType compositeEntityType() {
    return new EntityType()
      .name("composite_entity_type")
      .columns(List.of(
        new EntityTypeColumn().name("marc_bib.field1").dataType(new StringType()),
        new EntityTypeColumn().name("marc_bib.marc").dataType(new MarcType().dataType("marcType"))
      ));
  }

  // Composite declaring two MARC sources; the field's own prefix must select the matching placeholder.
  private static EntityType multiSourceCompositeEntityType() {
    return new EntityType()
      .name("multi_source_composite")
      .columns(List.of(
        new EntityTypeColumn().name("marc_bib.marc").dataType(new MarcType().dataType("marcType")),
        new EntityTypeColumn().name("marc_authority.marc").dataType(new MarcType().dataType("marcType"))
      ));
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
  @CsvSource({
    // fieldName,                     source,          tag, subfield, placeholderName
    "marc_bib.marc_245,               marc_bib,        245, ,         marc_bib.marc",
    "marc_bib.marc_245_a,             marc_bib,        245, a,        marc_bib.marc",
    "marc_authority.marc_245_a,       marc_authority,  245, a,        marc_authority.marc",
    "MARC_BIB.MARC_245_A,             MARC_BIB,        245, a,        MARC_BIB.marc",
    "a.b.marc_245_a,                  a.b,             245, a,        a.b.marc"
  })
  void shouldParseSourcePrefixedForms(String fieldName, String source, String tag, String subfield,
                                      String placeholderName) {
    MarcFieldName parsed = MarcFieldFactory.parse(fieldName).orElseThrow();
    // The original name is preserved verbatim (prefix included), while source/tag/subfield are split out.
    assertEquals(fieldName, parsed.fieldName());
    assertEquals(source, parsed.source());
    assertEquals(tag, parsed.tag());
    assertEquals(emptyToNull(subfield), parsed.subfield());
    assertEquals(placeholderName, parsed.placeholderName());
  }

  @Test
  void shouldParseUnprefixedFieldWithNullSource() {
    MarcFieldName parsed = MarcFieldFactory.parse("marc_245_a").orElseThrow();
    assertNull(parsed.source());
    assertEquals("marc", parsed.placeholderName());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "marc_24",             // tag too short
    "marc_2451",           // tag too long
    "marc_001_a",          // control field with subfield
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
  void addSyntheticColumnsAppendsMarcColumnsAndSkipsOthers() {
    EntityType original = marcEntityType(); // columns: field1, marc (placeholder)
    EntityType result = MarcFieldFactory.addSyntheticColumns(
      original, Arrays.asList("marc", null, "field1", "not_a_marc_field", "marc_245_a"));

    // Only marc_245_a is appended: "marc"/"field1" already present, null skipped, non-MARC name skipped.
    assertEquals(List.of("field1", "marc", "marc_245_a"),
      result.getColumns().stream().map(EntityTypeColumn::getName).toList());

    // The appended column is the lib's metadata-only MARC column.
    EntityTypeColumn added = result.getColumns().stream()
      .filter(c -> "marc_245_a".equals(c.getName())).findFirst().orElseThrow();
    assertTrue(added.getDataType() instanceof MarcType);
    assertEquals("MARC 245$a", added.getLabelAlias());

    // The original entity type is not mutated.
    assertEquals(List.of("field1", "marc"),
      original.getColumns().stream().map(EntityTypeColumn::getName).toList());
  }

  @Test
  void addSyntheticColumnsReturnsSameInstanceWithoutPlaceholder() {
    // No marc placeholder -> the entity type doesn't support MARC, so it's returned unchanged (same instance).
    EntityType original = nonMarcEntityType();
    assertSame(original, MarcFieldFactory.addSyntheticColumns(original, List.of("marc_245_a")));
  }

  @Test
  void addSyntheticColumnsReturnsSameInstanceForNullOrEmptyFieldNames() {
    EntityType original = marcEntityType();
    assertSame(original, MarcFieldFactory.addSyntheticColumns(original, null));
    assertSame(original, MarcFieldFactory.addSyntheticColumns(original, List.of()));
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
  void isGenericMarcPlaceholderAcceptsSourcePrefixedName() {
    // Composite placeholder: <source>.marc with a chain is also accepted.
    assertTrue(MarcFieldFactory.isGenericMarcPlaceholder(
      new EntityTypeColumn().name("marc_bib.marc").dataType(new MarcType().dataType("marcType"))));
    assertTrue(MarcFieldFactory.isGenericMarcPlaceholder(
      new EntityTypeColumn().name("a.b.marc").dataType(new MarcType().dataType("marcType"))));
    // A prefixed non-placeholder column is not the placeholder.
    assertFalse(MarcFieldFactory.isGenericMarcPlaceholder(
      new EntityTypeColumn().name("marc_bib.not_marc").dataType(new MarcType().dataType("marcType"))));
  }

  @Test
  void findMarcPlaceholderByFieldSelectsMatchingSource() {
    EntityType composite = multiSourceCompositeEntityType();

    MarcFieldName bibField = MarcFieldFactory.parse("marc_bib.marc_245_a").orElseThrow();
    assertEquals("marc_bib.marc",
      MarcFieldFactory.findMarcPlaceholder(composite, bibField).orElseThrow().getName());

    MarcFieldName authorityField = MarcFieldFactory.parse("marc_authority.marc_100_a").orElseThrow();
    assertEquals("marc_authority.marc",
      MarcFieldFactory.findMarcPlaceholder(composite, authorityField).orElseThrow().getName());

    // A field whose source has no matching placeholder resolves to nothing.
    MarcFieldName unknownSource = MarcFieldFactory.parse("marc_holdings.marc_245_a").orElseThrow();
    assertTrue(MarcFieldFactory.findMarcPlaceholder(composite, unknownSource).isEmpty());
  }

  @Test
  void findMarcPlaceholderByFieldReturnsEmptyForNullArgs() {
    MarcFieldName field = MarcFieldFactory.parse("marc_245_a").orElseThrow();
    assertTrue(MarcFieldFactory.findMarcPlaceholder(null, field).isEmpty());
    assertTrue(MarcFieldFactory.findMarcPlaceholder(marcEntityType(), null).isEmpty());
  }

  @Test
  void resolveMarcFieldResolvesSourcePrefixedField() {
    Optional<Field> resolved = MarcFieldFactory.resolveMarcField("marc_bib.marc_245_a", compositeEntityType());
    assertTrue(resolved.isPresent());
    // The synthetic column keeps the fully-qualified (prefixed) name.
    assertEquals("marc_bib.marc_245_a", ((EntityTypeColumn) resolved.get()).getName());

    // A valid MARC shape but a source the composite doesn't declare is not resolvable.
    assertTrue(MarcFieldFactory.resolveMarcField("marc_authority.marc_245_a", compositeEntityType()).isEmpty());
  }

  @Test
  void addSyntheticColumnsAppendsSourcePrefixedColumns() {
    EntityType result = MarcFieldFactory.addSyntheticColumns(
      multiSourceCompositeEntityType(),
      Arrays.asList("marc_bib.marc_245_a", "marc_authority.marc_100_a", "marc_holdings.marc_245_a", "marc_245_a"));

    // Both declared sources are synthesized; the undeclared source and the un-prefixed name are skipped.
    assertEquals(List.of("marc_bib.marc", "marc_authority.marc", "marc_bib.marc_245_a", "marc_authority.marc_100_a"),
      result.getColumns().stream().map(EntityTypeColumn::getName).toList());
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
  void getReferencedMarcFieldNamesScansSourcePrefixedRawQueryKeys() {
    String rawQuery = """
      {
        "$and": [
          { "marc_bib.marc_245_a": { "$eq": "x" } },
          { "marc_bib.external_hrid": { "$eq": "y" } },
          { "marc_authority.marc_100": { "$empty": false } }
        ]
      }
      """;
    // Prefixed MARC keys are captured whole (not truncated at the dot); the non-MARC prefixed key is excluded.
    assertEquals(List.of("marc_bib.marc_245_a", "marc_authority.marc_100"),
      List.copyOf(MarcFieldFactory.getReferencedMarcFieldNames(rawQuery)));
  }

  @Test
  void getReferencedMarcFieldNamesFromRawQueryHandlesBlankInput() {
    // Cast disambiguates the String overload from the FqlCondition one.
    assertTrue(MarcFieldFactory.getReferencedMarcFieldNames((String) null).isEmpty());
    assertTrue(MarcFieldFactory.getReferencedMarcFieldNames("   ").isEmpty());
  }

  @Test
  void getReferencedMarcFieldNamesFromConditionReturnsSingleField() {
    FqlService fqlService = new FqlService();
    var condition = fqlService.getFql("{ \"marc_245_a\": { \"$eq\": \"x\" } }").fqlCondition();
    assertEquals(Set.of("marc_245_a"), MarcFieldFactory.getReferencedMarcFieldNames(condition));
  }

  @Test
  void getReferencedMarcFieldNamesFromConditionFiltersNonMarcAndRecurses() {
    FqlService fqlService = new FqlService();
    // Only MARC fields are collected; the non-MARC field1 is excluded, and the nested $and is traversed.
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
    assertEquals(Set.of("marc_245_a", "marc_008"),
      MarcFieldFactory.getReferencedMarcFieldNames(condition));
  }

  private static String emptyToNull(String value) {
    return value == null || value.isEmpty() ? null : value;
  }
}
