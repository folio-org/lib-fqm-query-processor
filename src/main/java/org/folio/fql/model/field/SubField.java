package org.folio.fql.model.field;

public sealed interface SubField permits ArraySubField, PropertySubField {
  /** Get the field name as the String that would be used in the FQL query */
  public String serialize();
}
