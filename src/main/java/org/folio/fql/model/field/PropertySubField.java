package org.folio.fql.model.field;

public record PropertySubField(String propertyName) implements SubField {
  @Override
  public String serialize() {
    return "->" + propertyName;
  }
}
