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
 *   <li>byte position ({@code marc_008_p06}, {@code marc_008_p35_37}, {@code marc_ldr_p06}): control fields
 *       (001-009) and the leader only. {@code position} and {@code endPosition} set (equal for a single
 *       position, inclusive for a range); no subfield or indicators. The tag is {@link #LEADER_TAG} for the
 *       leader.</li>
 * </ul>
 *
 * @param fieldName       the original field name as referenced in the query (name preserved verbatim,
 *                        including any composite source-alias prefix such as {@code marc_bib.})
 * @param source          the composite source-alias prefix (e.g. {@code marc_bib}), or null for a
 *                        non-composite (simple) entity type where the field is un-prefixed
 * @param tag             the three-digit MARC tag, or {@link #LEADER_TAG} for the leader
 * @param subfield        the subfield code (lower-cased), or null when not targeting a subfield
 * @param ind1Value       the fixed value ind1 is constrained to (normalized: {@code blank} -> {@code #}, else
 *                        lower-cased), or null when ind1 is not a constraint
 * @param ind2Value       the fixed value ind2 is constrained to (same normalization), or null when ind2 is not a
 *                        constraint
 * @param targetIndicator "1" or "2" when an indicator is the target (its values are returned/queried); null when
 *                        the target is a subfield value or the whole tag
 * @param position        zero-based start byte position for the byte-position form (0-99), or null otherwise
 * @param endPosition     zero-based inclusive end byte position for the byte-position form (equal to
 *                        {@code position} for a single position, strictly greater for a range), or null
 *                        otherwise
 */
public record MarcFieldName(
  String fieldName,
  String source,
  String tag,
  String subfield,
  String ind1Value,
  String ind2Value,
  String targetIndicator,
  Integer position,
  Integer endPosition
) {

  /** Public token used in field names / labels for a blank indicator (e.g. {@code marc_245_ind1_blank_a}). */
  public static final String BLANK_INDICATOR_TOKEN = "blank";
  /** How a blank indicator is stored in the MARC indexers table. */
  public static final String BLANK_INDICATOR_STORAGE = "#";
  /** Tag value used for the leader (field names use {@code marc_ldr_...}; normalized to lower case). */
  public static final String LEADER_TAG = "ldr";
  private static final String PLACEHOLDER_COLUMN = "marc";

  /** Non-positional forms (tag, subfield, and/or indicator); {@code position}/{@code endPosition} are null. */
  public MarcFieldName(String fieldName, String source, String tag, String subfield, String ind1Value,
                       String ind2Value, String targetIndicator) {
    this(fieldName, source, tag, subfield, ind1Value, ind2Value, targetIndicator, null, null);
  }

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
   * True for the byte-position form ({@code marc_008_p06}, {@code marc_ldr_p00_04}): the target is the substring
   * at {@code position}..{@code endPosition} (inclusive, zero-based) of the control field or leader value.
   */
  public boolean isPositionTarget() {
    return position != null;
  }

  /** True when this field references the leader rather than a numbered tag. */
  public boolean isLeader() {
    return LEADER_TAG.equals(tag);
  }

  /**
   * Human-readable label. Examples: "MARC 245" (tag-only), "MARC 245$a" (subfield), "MARC 245 ind1" (indicator
   * target), "MARC 245 ind1=7 $a" (constrained subfield), "MARC 245 ind1=1 ind2=2 $a" (both constrained),
   * "MARC 245 ind1=1 ind2" (ind1 constrained, ind2 target), "MARC 008/06" (single position), "MARC 008/35-37"
   * (position range), "MARC ldr/06" (leader position). The public {@code blank} token is shown rather than the
   * stored {@code #}.
   */
  public String labelAlias() {
    if (isPositionTarget()) {
      // Cataloging notation: tag/pos or tag/start-end, positions zero-padded to two digits. The tag is shown as
      // normalized (lower case), consistent with subfield/indicator labels.
      String label = "MARC %s/%02d".formatted(tag, position);
      return position.equals(endPosition) ? label : label + "-%02d".formatted(endPosition);
    }

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
