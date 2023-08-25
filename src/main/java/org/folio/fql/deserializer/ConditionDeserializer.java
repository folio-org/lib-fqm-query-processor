package org.folio.fql.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.FieldCondition;
import org.folio.fql.model.LogicalCondition;

import java.io.IOException;
import java.util.Map;

import org.folio.fql.deserializer.DeserializerFunctions.LogicalPredicates;
import org.folio.fql.deserializer.DeserializerFunctions.FieldPredicates;
import org.folio.fql.deserializer.DeserializerFunctions.LogicalDeserializers;
import org.folio.fql.deserializer.DeserializerFunctions.FieldDeserializers;

import static org.folio.fql.deserializer.DeserializerFunctions.FieldPredicates.IS_REGEX;
import static org.folio.fql.deserializer.DeserializerFunctions.LogicalPredicates.*;
import static org.folio.fql.deserializer.DeserializerFunctions.FieldPredicates.*;
import static org.folio.fql.deserializer.DeserializerFunctions.LogicalDeserializers.*;
import static org.folio.fql.deserializer.DeserializerFunctions.FieldDeserializers.*;

public class ConditionDeserializer extends StdScalarDeserializer<FqlCondition<?>> {

  private static final String INVALID_OPERATOR_MESSAGE = "Condition %s contains an invalid operator";

  private static final Map<LogicalPredicates, LogicalDeserializers> LOGICAL_DESERIALIZERS = Map.of(
    IS_AND, AND_DESERIALIZER
  );

  private static final Map<FieldPredicates, FieldDeserializers> FIELD_DESERIALIZERS = Map.of(
    IS_EQ, EQ_DESERIALIZER,
    IS_NE, NE_DESERIALIZER,
    IS_IN, IN_DESERIALIZER,
    IS_NIN, NIN_DESERIALIZER,
    IS_GT, GT_DESERIALIZER,
    IS_GTE, GTE_DESERIALIZER,
    IS_LT, LT_DESERIALIZER,
    IS_LTE, LTE_DESERIALIZER,
    IS_REGEX, REGEX_DESERIALIZER
  );

  private final ObjectMapper mapper;

  public ConditionDeserializer(ObjectMapper mapper) {
    super(FqlCondition.class);
    this.mapper = mapper;
  }

  @Override
  public FqlCondition<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    JsonNode parentNode = jsonParser.getCodec().readTree(jsonParser);
    Map.Entry<String, JsonNode> fieldAndValue = parentNode.fields().next();
    String fieldName = fieldAndValue.getKey();
    JsonNode childNode = fieldAndValue.getValue();

    if (isLogicalCondition(fieldName)) {
      return getLogicalCondition(fieldName, childNode);
    }

    if (isFieldCondition(childNode)) {
      return getFieldCondition(fieldName, childNode);
    }

    throw new FqlParsingException(fieldName, String.format(INVALID_OPERATOR_MESSAGE, childNode.toString()));
  }

  private boolean isLogicalCondition(String fieldName) {
    return LOGICAL_DESERIALIZERS.keySet()
      .stream()
      .anyMatch(p -> p.predicate.test(fieldName));
  }

  private boolean isFieldCondition(JsonNode node) {
    return FIELD_DESERIALIZERS.keySet()
      .stream()
      .anyMatch(p -> p.predicate.test(node));
  }

  private LogicalCondition getLogicalCondition(String fieldName, JsonNode node) {
    return LOGICAL_DESERIALIZERS.keySet()
      .stream()
      .filter(p -> p.predicate.test(fieldName))
      .map(LOGICAL_DESERIALIZERS::get)
      .map(d -> d.deserializer.apply(node, mapper))
      .findFirst()
      .orElseThrow(() -> new FqlParsingException(fieldName, String.format(INVALID_OPERATOR_MESSAGE, node.toString())));
  }

  private FieldCondition<?> getFieldCondition(String fieldName, JsonNode node) {
    return FIELD_DESERIALIZERS.keySet()
      .stream()
      .filter(p -> p.predicate.test(node))
      .map(FIELD_DESERIALIZERS::get)
      .map(d -> d.deserializer.apply(fieldName, node))
      .findFirst()
      .orElseThrow(() -> new FqlParsingException(fieldName, String.format(INVALID_OPERATOR_MESSAGE, node.toString())));
  }
}
