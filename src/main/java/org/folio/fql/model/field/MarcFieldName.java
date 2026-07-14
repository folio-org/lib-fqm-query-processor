package org.folio.fql.model.field;

/**
 * A parsed dynamic MARC field reference. MARC fields are not declared columns; they are referenced by name
 * (e.g. {@code marc_245_a}) and recognized at query/validation time. This record is the canonical parsed
 * representation shared across modules; it carries no SQL or storage concerns (those live in the module that
 * generates SQL from it).
 *
 * <p>Supported forms:</p>
 * <ul>
 *   <li>tag-only: {@code subfield}, {@code indicatorNumber}, {@code indicatorValue} all null</li>
 *   <li>subfield: {@code subfield} set</li>
 *   <li>indicator-only: {@code indicatorNumber} ("1"/"2") set, {@code indicatorValue} null (targets the indicator)</li>
 *   <li>constrained-subfield: {@code indicatorNumber} + {@code indicatorValue} (fixed) + {@code subfield}</li>
 * </ul>
 *
 * @param fieldName       the original field name as referenced in the query (name preserved verbatim)
 * @param tag             the three-digit MARC tag
 * @param subfield        the subfield code (lower-cased), or null when not targeting a subfield
 * @param indicatorNumber "1" or "2" when an indicator is involved, otherwise null
 * @param indicatorValue  the fixed indicator value (normalized: {@code blank} -> {@code #}, else lower-cased)
 *                        for the constrained-subfield form; null otherwise
 */
public record MarcFieldName(
  String fieldName,
  String tag,
  String subfield,
  String indicatorNumber,
  String indicatorValue
) {

  /** Public token used in field names / labels for a blank indicator (e.g. {@code marc_245_ind1_blank_a}). */
  public static final String BLANK_INDICATOR_TOKEN = "blank";
  /** How a blank indicator is stored in the MARC indexers table. */
  public static final String BLANK_INDICATOR_STORAGE = "#";

  /**
   * True only for the indicator-only form, where the query targets the indicator itself. When a fixed
   * {@code indicatorValue} is present the indicator is a constraint and the subfield is the target.
   */
  public boolean isIndicatorTarget() {
    return indicatorNumber != null && indicatorValue == null;
  }

  /**
   * Human-readable label, e.g. "MARC 245" (tag-only), "MARC 245$a" (subfield), "MARC 245 ind1" (indicator),
   * "MARC 245 ind1=blank $a" (constrained subfield). The public {@code blank} token is shown rather than the
   * stored {@code #}.
   */
  public String labelAlias() {
    if (isIndicatorTarget()) {
      return "MARC %s ind%s".formatted(tag, indicatorNumber);
    }
    if (indicatorValue != null) {
      String displayValue = BLANK_INDICATOR_STORAGE.equals(indicatorValue) ? BLANK_INDICATOR_TOKEN : indicatorValue;
      return "MARC %s ind%s=%s $%s".formatted(tag, indicatorNumber, displayValue, subfield);
    }
    return subfield == null ? "MARC %s".formatted(tag) : "MARC %s$%s".formatted(tag, subfield);
  }
}
