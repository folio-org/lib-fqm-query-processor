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
    // fieldName,                    tag, subfield, ind1, ind2, target, labelAlias
    "marc_245,                       245, ,        ,     ,     ,       MARC 245",
    "marc_008,                       008, ,        ,     ,     ,       MARC 008",
    "marc_245_a,                     245, a,       ,     ,     ,       MARC 245$a",
    "MARC_245_A,                     245, a,       ,     ,     ,       MARC 245$a",
    "marc_245_ind1,                  245, ,        ,     ,     1,      MARC 245 ind1",
    "marc_245_ind2,                  245, ,        ,     ,     2,      MARC 245 ind2",
    "marc_245_ind1_7_a,              245, a,       7,    ,     ,       MARC 245 ind1=7 $a",
    "marc_245_ind1_blank_a,          245, a,       #,    ,     ,       MARC 245 ind1=blank $a",
    "marc_245_ind2_X_b,              245, b,       ,     x,    ,       MARC 245 ind2=x $b",
    // multi-indicator: both indicators constrained + subfield target
    "marc_245_ind1_1_ind2_2_a,       245, a,       1,    2,    ,       MARC 245 ind1=1 ind2=2 $a",
    "marc_650_ind1_1_ind2_0_x,       650, x,       1,    0,    ,       MARC 650 ind1=1 ind2=0 $x",
    "marc_650_ind1_blank_ind2_2_x,   650, x,       #,    2,    ,       MARC 650 ind1=blank ind2=2 $x",
    "marc_041_ind1_1_ind2_7_2,       041, 2,       1,    7,    ,       MARC 041 ind1=1 ind2=7 $2",
    // multi-indicator: one indicator constrained, the other is the target
    "marc_245_ind1_1_ind2,           245, ,        1,    ,     2,      MARC 245 ind1=1 ind2",
    "marc_245_ind2_1_ind1,           245, ,        ,     1,    1,      MARC 245 ind2=1 ind1",
    // whole-field value with indicator constraint(s), no subfield
    "marc_245_ind1_0,                245, ,        0,    ,     ,       MARC 245 ind1=0",
    "marc_245_ind2_1,                245, ,        ,     1,    ,       MARC 245 ind2=1",
    "marc_245_ind1_blank,            245, ,        #,    ,     ,       MARC 245 ind1=blank",
    "marc_245_ind1_1_ind2_2,         245, ,        1,    2,    ,       MARC 245 ind1=1 ind2=2"
  })
  void shouldParseSupportedForms(String fieldName, String tag, String subfield, String ind1, String ind2,
                                 String target, String labelAlias) {
    MarcFieldName parsed = MarcFieldFactory.parse(fieldName).orElseThrow();
    assertEquals(fieldName, parsed.fieldName());
    assertEquals(tag, parsed.tag());
    assertEquals(emptyToNull(subfield), parsed.subfield());
    assertEquals(emptyToNull(ind1), parsed.ind1Value());
    assertEquals(emptyToNull(ind2), parsed.ind2Value());
    assertEquals(emptyToNull(target), parsed.targetIndicator());
    assertEquals(labelAlias, parsed.labelAlias());
  }

  @ParameterizedTest
  @CsvSource({
    // fieldName,         source,   tag, position, endPosition, labelAlias
    "marc_008_p06,        ,         008, 6,        6,           MARC 008/06",
    "marc_008_p35_37,     ,         008, 35,       37,          MARC 008/35-37",
    "marc_006_p00,        ,         006, 0,        0,           MARC 006/00",
    "marc_007_p01,        ,         007, 1,        1,           MARC 007/01",
    "marc_ldr_p06,        ,         ldr, 6,        6,           MARC LDR/06",
    "marc_ldr_p00_04,     ,         ldr, 0,        4,           MARC LDR/00-04",
    "MARC_LDR_P06,        ,         ldr, 6,        6,           MARC LDR/06",
    "marc_005_p00_03,     ,         005, 0,        3,           MARC 005/00-03",
    // well-formed but beyond the 008 spec (40 chars): accepted so malformed records can be found
    "marc_008_p45,        ,         008, 45,       45,          MARC 008/45",
    "marc_008_p00_99,     ,         008, 0,        99,          MARC 008/00-99",
    // source-prefixed
    "marc_bib.marc_008_p06,    marc_bib, 008, 6,   6,           MARC 008/06",
    "marc_bib.marc_ldr_p00_04, marc_bib, ldr, 0,   4,           MARC LDR/00-04"
  })
  void shouldParsePositionForms(String fieldName, String source, String tag, Integer position, Integer endPosition,
                                String labelAlias) {
    MarcFieldName parsed = MarcFieldFactory.parse(fieldName).orElseThrow();
    assertEquals(fieldName, parsed.fieldName());
    assertEquals(emptyToNull(source), parsed.source());
    assertEquals(tag, parsed.tag());
    assertEquals(position, parsed.position());
    assertEquals(endPosition, parsed.endPosition());
    assertEquals(labelAlias, parsed.labelAlias());
    assertTrue(parsed.isPositionTarget());
    assertEquals("ldr".equals(tag), parsed.isLeader());
    // A byte-position target carries no subfield or indicator parts.
    assertNull(parsed.subfield());
    assertNull(parsed.ind1Value());
    assertNull(parsed.ind2Value());
    assertNull(parsed.targetIndicator());
    assertFalse(parsed.isIndicatorTarget());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "marc_008_p6",         // position not zero-padded
    "marc_008_p006",       // position too long
    "marc_008_pXX",        // non-numeric position
    "marc_008_p",          // no position
    "marc_008_p10_05",     // reversed range
    "marc_008_p06_06",     // degenerate range: the single-position spelling is marc_008_p06
    "marc_ldr_p00_00",     // degenerate range (leader)
    "marc_008_p35_7",      // range end not zero-padded
    "marc_008_p35_",       // dangling range separator
    "marc_008_p35_37_39",  // too many positions
    "marc_008_06",         // missing 'p' prefix
    "marc_008_p06_a",      // position with subfield
    "marc_245_p06",        // data field has no byte positions
    "marc_245_p00_04",     // data field has no byte positions (range)
    "marc_ldr",            // leader is position-only
    "marc_ldr_a",          // leader has no subfields
    "marc_ldr_ind1",       // leader has no indicators
    "marc_bib.marc_245_p06" // source-prefixed data field position
  })
  void shouldRejectMalformedPositionForms(String fieldName) {
    assertTrue(MarcFieldFactory.parse(fieldName).isEmpty(), fieldName);
  }

  @ParameterizedTest
  @ValueSource(strings = {"marc_245", "marc_008", "marc_245_a", "marc_245_ind1", "marc_245_ind1_7_a",
    "marc_245_ind1_0", "marc_245_p"})
  void nonPositionFormsHaveNoPosition(String fieldName) {
    MarcFieldName parsed = MarcFieldFactory.parse(fieldName).orElseThrow();
    assertFalse(parsed.isPositionTarget());
    assertFalse(parsed.isLeader());
    assertNull(parsed.position());
    assertNull(parsed.endPosition());
  }

  @Test
  void toColumnUsesPositionLabel() {
    EntityTypeColumn column = MarcFieldFactory.toColumn(MarcFieldFactory.parse("marc_008_p35_37").orElseThrow());
    assertEquals("marc_008_p35_37", column.getName());
    assertEquals("MARC 008/35-37", column.getLabelAlias());
    assertTrue(column.getDataType() instanceof MarcType);
  }

  @Test
  void getReferencedMarcFieldNamesScansPositionKeys() {
    String rawQuery = """
      {
        "$and": [
          { "marc_008_p35_37": { "$eq": "eng" } },
          { "marc_ldr_p06": { "$eq": "a" } },
          { "marc_bib.marc_008_p06": { "$eq": "s" } },
          { "marc_245_p06": { "$eq": "x" } }
        ]
      }
      """;
    assertEquals(List.of("marc_008_p35_37", "marc_ldr_p06", "marc_bib.marc_008_p06"),
      List.copyOf(MarcFieldFactory.getReferencedMarcFieldNames(rawQuery)));
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
    "marc_245_ind1_1_ind1",       // multi-indicator: constraint and target are the same indicator
    "marc_245_ind2_1_ind2",       // multi-indicator: same indicator (ind2/ind2)
    "marc_245_ind2_2_ind1_1_a",   // multi-indicator: non-canonical order (ind2 before ind1) for both constraints
    "marc_245_ind1_1_ind2_ab_a",  // multi-indicator: multi-char ind2 value
    "marc_008_ind1_7_a",          // control field cannot use a constrained subfield
    "marc_008_ind1_1_ind2_2_a",   // control field cannot use a dual-indicator subfield
    "marc_008_ind1_1_ind2",       // control field cannot use a constrained indicator-target
    "marc_245_ind1_ab",           // constrained field: multi-char indicator value
    "marc_008_ind1_0",            // control field cannot use a constrained field
    "marc_008_ind1_1_ind2_2",     // control field cannot use a dual-indicator constrained field
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
    "marc_245_ind1, true",           // indicator-only targets the indicator
    "marc_245_ind1_1_ind2, true",    // one constrained, the other indicator is the target
    "marc_245_ind2_1_ind1, true",    // mirror
    "marc_245_ind1_7_a, false",      // constrained subfield targets the subfield
    "marc_245_ind1_1_ind2_2_a, false", // both constrained, subfield is the target
    "marc_245_ind1_0, false",        // constrained field, no target
    "marc_245_ind1_1_ind2_2, false", // dual constrained field, no target
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
