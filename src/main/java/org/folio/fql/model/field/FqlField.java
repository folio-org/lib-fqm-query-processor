package org.folio.fql.model.field;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;
import org.folio.fql.deserializer.FqlParsingException;

@Value
public class FqlField {

  private final String columnName;
  private final List<SubField> subFields;

  public FqlField(String field) {
    int firstSubFieldIndex = getIndexOfSubfield(field);

    this.subFields = new ArrayList<>();

    if (firstSubFieldIndex == -1) {
      this.columnName = field;
    } else {
      this.columnName = field.substring(0, firstSubFieldIndex);
      parseSubFields(field, field.substring(firstSubFieldIndex));
    }
  }

  /** Finds the first [*] or -> in the string; if none, it returns -1 */
  private static int getIndexOfSubfield(String str) {
    int firstArrayIndex = str.indexOf("[*]");
    int firstPropertyIndex = str.indexOf("->");

    int firstSubFieldIndex = Math.min(firstArrayIndex, firstPropertyIndex);
    // if one of the indexes is -1 (nonexistent), then we always use the other one
    // if both are -1 (no subfields), this will keep -1
    if (firstSubFieldIndex == -1) {
      firstSubFieldIndex = Math.max(firstArrayIndex, firstPropertyIndex);
    }

    return firstSubFieldIndex;
  }

  private void parseSubFields(String field, String substring) {
    if (substring.startsWith("->")) {
      String remaining = substring.substring(2);
      int nestedIndex = getIndexOfSubfield(remaining);
      if (nestedIndex == -1) {
        subFields.add(new PropertySubField(remaining));
      } else {
        subFields.add(new PropertySubField(remaining.substring(0, nestedIndex)));
        parseSubFields(field, remaining.substring(nestedIndex));
      }
    } else if (substring.startsWith("[*]")) {
      String remaining = substring.substring(3);
      int nestedIndex = getIndexOfSubfield(remaining);
      if (nestedIndex == -1) {
        throw new FqlParsingException(field, "Array subfields [*] must be followed by a property subfield");
      } else {
        subFields.add(new ArraySubField());
        parseSubFields(field, remaining.substring(nestedIndex));
      }
    } else {
      throw new IllegalArgumentException("Unrecognized subfield did not start with -> or [*]: " + substring);
    }
  }

  /** Get the field name as the String that would be used in the FQL query */
  public String serialize() {
    return columnName + subFields.stream().map(SubField::serialize).collect(Collectors.joining());
  }
}
