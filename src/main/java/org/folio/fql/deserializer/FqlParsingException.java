package org.folio.fql.deserializer;

public class FqlParsingException extends RuntimeException {
  private static final String EXAMPLE_FQL_COMPLEX = """
    {
        "$and": [
            {"field1": {"$eq": "value1"}},
            {"field2": {"$lt": 10}},
            {"field3": {"gt": "2023-03-28T12:50:33+00:00"}}
        ]
    }
    """;

  private static final String EXAMPLE_FQL_SIMPLE = """
    {"field1": {"$ne": "value1"}}
    """;

  private final String errorField;

  private final String errorMessage;

  public FqlParsingException(String errorField, String errorMessage) {
    super("Invalid FQL. \r\n" +
      "Example simple FQL: " + EXAMPLE_FQL_SIMPLE + "\r\n" +
      "Example complex FQL: " + EXAMPLE_FQL_COMPLEX + "\r\n"
    );
    this.errorField = errorField;
    this.errorMessage = errorMessage;
  }

  public String getErrorField() {
    return errorField;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
