package org.folio.fql.service;

import java.util.LinkedHashSet;
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

  // Tag-only form (e.g. marc_245). Matches any subfield of the tag, and is the only valid form for control
  // fields. All patterns match case-insensitively (MARC_245 behaves the same as marc_245).
  private static final Pattern TAG_PATTERN =
    Pattern.compile("^marc_(?<tag>\\d{3})$", Pattern.CASE_INSENSITIVE);

  // Subfield form (e.g. marc_245_a). Data-field tags (010+) only; control fields have no indicators.
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
  private static final Pattern QUERY_FIELD_KEY_PATTERN = Pattern.compile("\"(?<field>\\w+)\"\\s*:");

  public static boolean isMarcFieldName(String fieldName) {
    return parse(fieldName).isPresent();
  }

  /** Parse a field name into a {@link MarcFieldName}, or empty if it is not a valid MARC field reference. */
  public static Optional<MarcFieldName> parse(String fieldName) {
    if (fieldName == null) {
      return Optional.empty();
    }

    // Control fields (001-009) have no subfields or indicators, so only the tag-only form is valid for them.
    Matcher subfieldMatcher = SUBFIELD_PATTERN.matcher(fieldName);
    if (subfieldMatcher.matches() && !isControlFieldTag(subfieldMatcher.group("tag"))) {
      // Preserve the original field name; normalize the subfield code to lower case to match storage.
      return Optional.of(new MarcFieldName(
        fieldName,
        subfieldMatcher.group("tag"),
        subfieldMatcher.group("subfield").toLowerCase(),
        null,
        null
      ));
    }

    Matcher constrainedMatcher = CONSTRAINED_SUBFIELD_PATTERN.matcher(fieldName);
    if (constrainedMatcher.matches() && !isControlFieldTag(constrainedMatcher.group("tag"))) {
      return Optional.of(new MarcFieldName(
        fieldName,
        constrainedMatcher.group("tag"),
        constrainedMatcher.group("subfield").toLowerCase(),
        constrainedMatcher.group("indicator"),
        normalizeIndicatorValue(constrainedMatcher.group("indValue"))
      ));
    }

    Matcher indicatorMatcher = INDICATOR_PATTERN.matcher(fieldName);
    if (indicatorMatcher.matches() && !isControlFieldTag(indicatorMatcher.group("tag"))) {
      return Optional.of(new MarcFieldName(
        fieldName,
        indicatorMatcher.group("tag"),
        null,
        indicatorMatcher.group("indicator"),
        null
      ));
    }

    Matcher tagMatcher = TAG_PATTERN.matcher(fieldName);
    if (tagMatcher.matches()) {
      return Optional.of(new MarcFieldName(fieldName, tagMatcher.group("tag"), null, null, null));
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

  /** All field names referenced by a parsed FQL condition tree (MARC or otherwise). */
  public static Set<String> getReferencedFieldNames(FqlCondition<?> condition) {
    if (condition instanceof FieldCondition<?> fieldCondition) {
      return Set.of(fieldCondition.field().getColumnName());
    }
    if (condition instanceof AndCondition andCondition) {
      return andCondition.value().stream()
        .map(MarcFieldFactory::getReferencedFieldNames)
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
   * The generic, hidden {@code marc} capability column that declares an entity type supports dynamic MARC field
   * references. It is a correlation placeholder, not a user-facing field, and should be excluded from field
   * listings.
   */
  public static boolean isGenericMarcPlaceholder(EntityTypeColumn column) {
    return column != null
      && GENERIC_MARC_COLUMN_NAME.equals(column.getName())
      && column.getDataType() instanceof MarcType;
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
   * Resolve a field name to a metadata-only MARC column, but only if it parses as a MARC field and the entity
   * type declares the generic {@code marc} placeholder. Returns empty otherwise, so entity types without MARC
   * support keep rejecting {@code marc_*} references.
   */
  public static Optional<Field> resolveMarcField(String fieldName, EntityType entityType) {
    if (findMarcPlaceholder(entityType).isEmpty()) {
      return Optional.empty();
    }
    return parse(fieldName).<Field>map(MarcFieldFactory::toColumn);
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
