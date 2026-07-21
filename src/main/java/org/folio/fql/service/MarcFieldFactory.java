package org.folio.fql.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import org.folio.fql.model.AndCondition;
import org.folio.fql.model.FieldCondition;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.field.MarcFieldName;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.folio.querytool.domain.dto.Field;
import org.folio.querytool.domain.dto.MarcType;

/**
 * Canonical recognition and metadata synthesis for dynamic MARC fields (e.g. {@code marc_245_a}). Shared by
 * every module that validates or builds queries over MARC data, so the grammar lives in exactly one place.
 *
 * <p>This class is deliberately free of SQL/storage concerns: it parses field names, detects the generic
 * {@code marc} capability placeholder, and produces metadata-only {@link EntityTypeColumn}s. The module that
 * executes queries (mod-fqm-manager) is responsible for generating SQL from the parsed {@link MarcFieldName}.
 */
@UtilityClass
public class MarcFieldFactory {

  /**
   * Name of the generic, hidden {@code marc} column that declares an entity type supports dynamic MARC field
   * references. Its presence is the capability signal; it is a correlation placeholder, not a user-facing field.
   */
  public static final String GENERIC_MARC_COLUMN_NAME = "marc";

  // Suffix a composite's source-prefixed placeholder ends with, e.g. "marc_bib.marc" (chains too: "a.b.marc").
  private static final String SOURCE_PLACEHOLDER_SUFFIX = "." + GENERIC_MARC_COLUMN_NAME;

  // Tag-only form (e.g. marc_245). Matches any subfield of the tag, and is the only valid form for control
  // fields. All patterns match case-insensitively (MARC_245 behaves the same as marc_245).
  private static final Pattern TAG_PATTERN =
    Pattern.compile("^marc_(?<tag>\\d{3})$", Pattern.CASE_INSENSITIVE);

  // Subfield form (e.g. marc_245_a). Data-field tags (010+) only; control fields have no subfields.
  private static final Pattern SUBFIELD_PATTERN =
    Pattern.compile("^marc_(?<tag>\\d{3})_(?<subfield>[a-z0-9])$", Pattern.CASE_INSENSITIVE);

  // Indicator form (e.g. marc_245_ind1 / marc_245_ind2). Data-field tags (010+) only; control fields have no
  // indicators.
  private static final Pattern INDICATOR_PATTERN =
    Pattern.compile("^marc_(?<tag>\\d{3})_ind(?<indicator>[12])$", Pattern.CASE_INSENSITIVE);

  // Constrained-subfield form (e.g. marc_245_ind1_7_a, marc_245_ind1_blank_a). A subfield value target with the
  // indicator fixed to a constant matched on the same row. Data-field tags (010+) only.
  private static final Pattern CONSTRAINED_SUBFIELD_PATTERN = Pattern.compile(
    "^marc_(?<tag>\\d{3})_ind(?<indicator>[12])_(?<indValue>blank|[a-z0-9])_(?<subfield>[a-z0-9])$",
    Pattern.CASE_INSENSITIVE);

  // Generic scanner for JSON field-name keys in a raw FQL query. It intentionally does NOT encode the MARC
  // grammar. Every candidate key is validated through parse()/isMarcFieldName, so the grammar lives in one place.
  // The class allows dots so composite-prefixed keys (marc_bib.marc_245_a) are captured whole, not truncated.
  private static final Pattern QUERY_FIELD_KEY_PATTERN = Pattern.compile("\"(?<field>[\\w.]+)\"\\s*:");

  public static boolean isMarcFieldName(String fieldName) {
    return parse(fieldName).isPresent();
  }

  /** Parse a field name into a {@link MarcFieldName}, or empty if it is not a valid MARC field reference. */
  public static Optional<MarcFieldName> parse(String fieldName) {
    if (fieldName == null) {
      return Optional.empty();
    }

    // On composite entity types the field carries a source-alias prefix (marc_bib.marc_245_a), possibly a chain
    // (a.b.marc_245_a). The marc_* core never contains a dot, so the last dot is always the source/core boundary;
    // split there with a plain string op (no regex backtracking). The shape patterns below validate the core.
    String source = null;
    String core = fieldName;
    int lastDot = fieldName.lastIndexOf('.');
    if (lastDot > 0) {
      source = fieldName.substring(0, lastDot);
      core = fieldName.substring(lastDot + 1);
    }

    // Control fields (001-009) have no subfields or indicators, so only the tag-only form is valid for them.
    Matcher subfieldMatcher = SUBFIELD_PATTERN.matcher(core);
    if (subfieldMatcher.matches() && !isControlFieldTag(subfieldMatcher.group("tag"))) {
      // Preserve the original field name; normalize the subfield code to lower case to match storage.
      return Optional.of(new MarcFieldName(
        fieldName,
        source,
        subfieldMatcher.group("tag"),
        subfieldMatcher.group("subfield").toLowerCase(),
        null,
        null
      ));
    }

    Matcher constrainedMatcher = CONSTRAINED_SUBFIELD_PATTERN.matcher(core);
    if (constrainedMatcher.matches() && !isControlFieldTag(constrainedMatcher.group("tag"))) {
      return Optional.of(new MarcFieldName(
        fieldName,
        source,
        constrainedMatcher.group("tag"),
        constrainedMatcher.group("subfield").toLowerCase(),
        constrainedMatcher.group("indicator"),
        normalizeIndicatorValue(constrainedMatcher.group("indValue"))
      ));
    }

    Matcher indicatorMatcher = INDICATOR_PATTERN.matcher(core);
    if (indicatorMatcher.matches() && !isControlFieldTag(indicatorMatcher.group("tag"))) {
      return Optional.of(new MarcFieldName(
        fieldName,
        source,
        indicatorMatcher.group("tag"),
        null,
        indicatorMatcher.group("indicator"),
        null
      ));
    }

    Matcher tagMatcher = TAG_PATTERN.matcher(core);
    if (tagMatcher.matches()) {
      return Optional.of(new MarcFieldName(fieldName, source, tagMatcher.group("tag"), null, null, null));
    }

    return Optional.empty();
  }

  /** MARC field names referenced as {@code "fieldName":} keys in a raw FQL query string. */
  public static Set<String> getReferencedMarcFieldNames(String rawQuery) {
    if (rawQuery == null || rawQuery.isBlank()) {
      return Set.of();
    }

    Set<String> fieldNames = new LinkedHashSet<>();
    Matcher matcher = QUERY_FIELD_KEY_PATTERN.matcher(rawQuery);
    while (matcher.find()) {
      String candidate = matcher.group("field");
      if (isMarcFieldName(candidate)) {
        fieldNames.add(candidate);
      }
    }
    return fieldNames;
  }

  /** MARC field names referenced anywhere in a parsed FQL condition tree. */
  public static Set<String> getReferencedMarcFieldNames(FqlCondition<?> condition) {
    if (condition instanceof FieldCondition<?> fieldCondition) {
      String fieldName = fieldCondition.field().getColumnName();
      return isMarcFieldName(fieldName) ? Set.of(fieldName) : Set.of();
    }
    if (condition instanceof AndCondition andCondition) {
      return andCondition.value().stream()
        .map(MarcFieldFactory::getReferencedMarcFieldNames)
        .collect(LinkedHashSet::new, Set::addAll, Set::addAll);
    }
    return Set.of();
  }

  public static Optional<EntityTypeColumn> findMarcPlaceholder(EntityType entityType) {
    if (entityType == null || entityType.getColumns() == null) {
      return Optional.empty();
    }
    return entityType.getColumns().stream()
      .filter(MarcFieldFactory::isGenericMarcPlaceholder)
      .findFirst();
  }

  /**
   * The specific MARC placeholder a parsed field correlates against, matched by name
   * ({@link MarcFieldName#placeholderName()} — {@code marc}, or {@code <source>.marc} on a composite). A composite
   * may declare more than one MARC source, so the field's own prefix selects the right one.
   */
  public static Optional<EntityTypeColumn> findMarcPlaceholder(EntityType entityType, MarcFieldName marcField) {
    if (entityType == null || entityType.getColumns() == null || marcField == null) {
      return Optional.empty();
    }
    String placeholderName = marcField.placeholderName();
    return entityType.getColumns().stream()
      .filter(column -> column.getDataType() instanceof MarcType && placeholderName.equals(column.getName()))
      .findFirst();
  }

  public static boolean isGenericMarcPlaceholder(EntityTypeColumn column) {
    if (column == null || !(column.getDataType() instanceof MarcType)) {
      return false;
    }
    String name = column.getName();
    return GENERIC_MARC_COLUMN_NAME.equals(name) || name.endsWith(SOURCE_PLACEHOLDER_SUFFIX);
  }

  /**
   * Metadata-only synthetic column for a parsed MARC field: name, label, and {@code marcType}. It carries no
   * value getters — the SQL for retrieving MARC values is generated by the query-executing module.
   */
  public static EntityTypeColumn toColumn(MarcFieldName marcField) {
    return new EntityTypeColumn()
      .name(marcField.fieldName())
      .labelAlias(marcField.labelAlias())
      .dataType(new MarcType().dataType("marcType"))
      .queryable(true)
      .visibleByDefault(false)
      .essential(false);
  }

  /**
   * Return {@code entityType} augmented with metadata-only synthetic columns for the MARC field names in
   * {@code fieldNames}. Only names that parse as MARC fields are added, and only when the entity type declares
   * the generic {@code marc} placeholder; other names and already-present columns are skipped. The original
   * entity type is not mutated. The synthesized columns carry no SQL — see {@link #toColumn(MarcFieldName)}.
   */
  public static EntityType addSyntheticColumns(EntityType entityType, Collection<String> fieldNames) {
    if (fieldNames == null || fieldNames.isEmpty() || entityType == null || entityType.getColumns() == null
      || findMarcPlaceholder(entityType).isEmpty()) {
      return entityType;
    }

    List<EntityTypeColumn> updatedColumns = new ArrayList<>(entityType.getColumns());
    Set<String> existingFieldNames = updatedColumns.stream()
      .map(Field::getName)
      .collect(LinkedHashSet::new, Set::add, Set::addAll);

    for (String fieldName : fieldNames) {
      if (fieldName == null || existingFieldNames.contains(fieldName)) {
        continue;
      }
      // Synthesize only when the name parses AND the source's placeholder is present (so a composite correlates
      // against the right source). Non-MARC names, and fields whose placeholder is absent, are skipped.
      parse(fieldName)
        .filter(field -> findMarcPlaceholder(entityType, field).isPresent())
        .ifPresent(field -> {
          updatedColumns.add(toColumn(field));
          existingFieldNames.add(fieldName);
        });
    }

    return entityType.toBuilder().columns(updatedColumns).build();
  }

  /**
   * Resolve a field name to a metadata-only MARC column, but only if it parses as a MARC field and the entity
   * type declares the generic {@code marc} placeholder. Returns empty otherwise, so entity types without MARC
   * support keep rejecting {@code marc_*} references.
   */
  public static Optional<Field> resolveMarcField(String fieldName, EntityType entityType) {
    Optional<MarcFieldName> parsed = parse(fieldName);
    if (parsed.isEmpty() || findMarcPlaceholder(entityType, parsed.get()).isEmpty()) {
      return Optional.empty();
    }
    return Optional.<Field>of(toColumn(parsed.get()));
  }

  // MARC control fields are tags 001-009 (the only valid tags starting with "00"); they have no indicators or
  // subfields, just a single string value. Tags 010+ are data fields.
  private static boolean isControlFieldTag(String tag) {
    return tag.startsWith("00");
  }

  // The public token "blank" maps to the stored '#'; other (single alphanumeric) values are lower-cased so the
  // constraint matches case-insensitively.
  private static String normalizeIndicatorValue(String rawValue) {
    return MarcFieldName.BLANK_INDICATOR_TOKEN.equalsIgnoreCase(rawValue)
      ? MarcFieldName.BLANK_INDICATOR_STORAGE
      : rawValue.toLowerCase();
  }
}
