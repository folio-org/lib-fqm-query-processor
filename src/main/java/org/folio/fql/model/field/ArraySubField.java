package org.folio.fql.model.field;

public record ArraySubField() implements SubField {
  @Override
  public String serialize() {
    return "[*]";
  }
}
