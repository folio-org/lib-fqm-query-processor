package org.folio.fql.model.field;

/**
 * A parsed dynamic MARC field reference. MARC fields are not declared columns; they are referenced by name
 * (e.g. {@code marc_245_a}) and recognized at query/validation time. This record is the canonical parsed
 * representation shared across modules; it carries no SQL or storage concerns (those live in the module that
 * generates SQL from it).
 *
 * <p>An indicator may play one of two roles: a constraint (fixed to a value that is matched on the same
 * row — {@code ind1Value}/{@code ind2Value}) or the target ({@code targetIndicator}, the indicator whose
 * values are returned/queried). At most one indicator is the target; the other, if present, is a constraint.
 *
 * <p>Supported forms:
 * <ul>
 *   <li>tag-only ({@code marc_245}): no constraints, no target subfield/indicator</li>
 *   <li>subfield ({@code marc_245_a}): {@code subfield} set, no constraints</li>
 *   <li>indicator target ({@code marc_245_ind1}): {@code targetIndicator} set, no constraints</li>
 *   <li>constrained subfield, one indicator ({@code marc_245_ind1_7_a}): one of {@code ind1Value}/{@code
 *       ind2Value} + {@code subfield}</li>
 *   <li>constrained subfield, both indicators ({@code marc_245_ind1_1_ind2_2_a}): {@code ind1Value} +
 *       {@code ind2Value} + {@code subfield}</li>
 *   <li>indicator target with the other constrained ({@code marc_245_ind1_1_ind2} /
 *       {@code marc_245_ind2_1_ind1}): one indicator value + {@code targetIndicator} = the other indicator</li>
 *   <li>constrained field, one indicator ({@code marc_245_ind1_0}): one of {@code ind1Value}/{@code ind2Value},
 *       no {@code subfield} and no {@code targetIndicator} — the whole field value narrowed to occurrences whose
 *       indicator matches (e.g. "field 245 exists with ind1 = 0")</li>
 *   <li>constrained field, both indicators ({@code marc_245_ind1_1_ind2_2}): {@code ind1Value} +
 *       {@code ind2Value}, no {@code subfield} and no {@code targetIndicator}</li>
 * </ul>
 *
 * @param fieldName       the original field name as referenced in the query (name preserved verbatim,
 *                        including any composite source-alias prefix such as {@code marc_bib.})
 * @param source          the composite source-alias prefix (e.g. {@code marc_bib}), or null for a
 *                        non-composite (simple) entity type where the field is un-prefixed
 * @param tag             the three-digit MARC tag
 * @param subfield        the subfield code (lower-cased), or null when not targeting a subfield
 * @param ind1Value       the fixed value ind1 is constrained to (normalized: {@code blank} -> {@code #}, else
 *                        lower-cased), or null when ind1 is not a constraint
 * @param ind2Value       the fixed value ind2 is constrained to (same normalization), or null when ind2 is not a
 *                        constraint
 * @param targetIndicator "1" or "2" when an indicator is the target (its values are returned/queried); null when
 *                        the target is a subfield value or the whole tag
 */
public record MarcFieldName(
  String fieldName,
  String source,
  String tag,
  String subfield,
  String ind1Value,
  String ind2Value,
  String targetIndicator
) {

  /** Public token used in field names / labels for a blank indicator (e.g. {@code marc_245_ind1_blank_a}). */
  public static final String BLANK_INDICATOR_TOKEN = "blank";
  /** How a blank indicator is stored in the MARC indexers table. */
  public static final String BLANK_INDICATOR_STORAGE = "#";
  private static final String PLACEHOLDER_COLUMN = "marc";

  /**
   * Name of the generic MARC placeholder column this field correlates against: {@code marc} on a simple entity
   * type, or {@code <source>.marc} on a composite (where {@code source} may itself be a chain of aliases, e.g.
   * {@code a.b.marc} for a composite of a composite).
   */
  public String placeholderName() {
    return source == null ? PLACEHOLDER_COLUMN : source + "." + PLACEHOLDER_COLUMN;
  }

  /**
   * True when an indicator is the target (the query returns/matches that indicator's values). When both indicators
   * are only constraints, or the target is a subfield/tag value, this is false.
   */
  public boolean isIndicatorTarget() {
    return targetIndicator != null;
  }

  /**
   * Human-readable label. Examples: "MARC 245" (tag-only), "MARC 245$a" (subfield), "MARC 245 ind1" (indicator
   * target), "MARC 245 ind1=7 $a" (constrained subfield), "MARC 245 ind1=1 ind2=2 $a" (both constrained),
   * "MARC 245 ind1=1 ind2" (ind1 constrained, ind2 target). The public {@code blank} token is shown rather than
   * the stored {@code #}.
   */
  public String labelAlias() {
    StringBuilder label = new StringBuilder("MARC ").append(tag);
    boolean hasConstraint = ind1Value != null || ind2Value != null;
    if (ind1Value != null) {
      label.append(" ind1=").append(displayIndicatorValue(ind1Value));
    }
    if (ind2Value != null) {
      label.append(" ind2=").append(displayIndicatorValue(ind2Value));
    }
    if (targetIndicator != null) {
      label.append(" ind").append(targetIndicator);
    } else if (subfield != null) {
      // Attached ("MARC 245$a") when unconstrained; spaced ("... $a") when following a constraint.
      label.append(hasConstraint ? " $" : "$").append(subfield);
    }
    return label.toString();
  }

  private static String displayIndicatorValue(String storedValue) {
    return BLANK_INDICATOR_STORAGE.equals(storedValue) ? BLANK_INDICATOR_TOKEN : storedValue;
  }
}
