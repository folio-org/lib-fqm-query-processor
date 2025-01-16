package org.folio.fql.deserializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.fql.model.AndCondition;
import org.folio.fql.model.ContainsAllCondition;
import org.folio.fql.model.ContainsAnyCondition;
import org.folio.fql.model.EmptyCondition;
import org.folio.fql.model.EqualsCondition;
import org.folio.fql.model.FieldCondition;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.GreaterThanCondition;
import org.folio.fql.model.InCondition;
import org.folio.fql.model.LessThanCondition;
import org.folio.fql.model.LogicalCondition;
import org.folio.fql.model.NotContainsAllCondition;
import org.folio.fql.model.NotContainsAnyCondition;
import org.folio.fql.model.NotEqualsCondition;
import org.folio.fql.model.NotInCondition;
import org.folio.fql.model.RegexCondition;
import org.folio.fql.model.StartsWithCondition;
import org.folio.fql.model.ContainsCondition;
import org.folio.fql.model.field.FqlField;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.folio.fql.model.ContainsAllCondition.$CONTAINS_ALL;
import static org.folio.fql.model.ContainsAnyCondition.$CONTAINS_ANY;
import static org.folio.fql.model.ContainsCondition.$CONTAINS;
import static org.folio.fql.model.NotContainsAllCondition.$NOT_CONTAINS_ALL;
import static org.folio.fql.model.EmptyCondition.$EMPTY;
import static org.folio.fql.model.EqualsCondition.$EQ;
import static org.folio.fql.model.NotContainsAnyCondition.$NOT_CONTAINS_ANY;
import static org.folio.fql.model.NotEqualsCondition.$NE;
import static org.folio.fql.model.GreaterThanCondition.$GT;
import static org.folio.fql.model.GreaterThanCondition.$GTE;
import static org.folio.fql.model.LessThanCondition.$LT;
import static org.folio.fql.model.LessThanCondition.$LTE;
import static org.folio.fql.model.InCondition.$IN;
import static org.folio.fql.model.NotInCondition.$NIN;
import static org.folio.fql.model.RegexCondition.$REGEX;
import static org.folio.fql.model.AndCondition.$AND;
import static org.folio.fql.model.StartsWithCondition.$STARTS_WITH;

public class DeserializerFunctions {
  enum FieldPredicates {
    IS_EQ(node -> node.has($EQ) && node.get($EQ).isValueNode()),
    IS_NE(node -> node.has($NE) && node.get($NE).isValueNode()),
    IS_IN(node -> node.has($IN) && node.get($IN).isArray()),
    IS_NIN(node -> node.has($NIN) && node.get($NIN).isArray()),
    IS_GT(node -> node.has($GT) && node.get($GT).isValueNode()),
    IS_GTE(node -> node.has($GTE) && node.get($GTE).isValueNode()),
    IS_LT(node -> node.has($LT) && node.get(LessThanCondition.$LT).isValueNode()),
    IS_LTE(node -> node.has($LTE) && node.get($LTE).isValueNode()),
    IS_REGEX(node -> node.has($REGEX) && node.get($REGEX).isTextual()),
    IS_CONTAINS_ALL(node -> node.has($CONTAINS_ALL) && node.get($CONTAINS_ALL).isArray()),
    IS_NOT_CONTAINS_ALL(node -> node.has($NOT_CONTAINS_ALL) && node.get($NOT_CONTAINS_ALL).isArray()),
    IS_CONTAINS_ANY(node -> node.has($CONTAINS_ANY) && node.get($CONTAINS_ANY).isArray()),
    IS_NOT_CONTAINS_ANY(node -> node.has($NOT_CONTAINS_ANY) && node.get($NOT_CONTAINS_ANY).isArray()),
    IS_STARTS_WITH(node -> node.has($STARTS_WITH) && node.get($STARTS_WITH).isTextual()),
    IS_CONTAINS(node -> node.has($CONTAINS) && node.get($CONTAINS).isTextual()),
    IS_EMPTY(node -> node.has($EMPTY) && isBooleanNode(node.get($EMPTY)));

    final Predicate<JsonNode> predicate;

    FieldPredicates(Predicate<JsonNode> predicate) {
      this.predicate = predicate;
    }

    private static boolean isBooleanNode(JsonNode node) {
      return node.isBoolean() || "true".equals(node.textValue()) || "false".equals(node.textValue());
    }
  }

  enum LogicalPredicates {
    IS_AND(fieldName -> fieldName.equals($AND));

    final Predicate<String> predicate;

    LogicalPredicates(Predicate<String> predicate) {
      this.predicate = predicate;
    }
  }

  enum FieldDeserializers {
    EQ_DESERIALIZER((field, node) -> new EqualsCondition(field, convertValue(node.get($EQ)))),
    NE_DESERIALIZER((field, node) -> new NotEqualsCondition(field, convertValue(node.get($NE)))),
    IN_DESERIALIZER((field, node) -> new InCondition(field, getValues(node.get($IN).elements(), FieldDeserializers::convertValue))),
    NIN_DESERIALIZER((field, node) -> new NotInCondition(field, getValues(node.get($NIN).elements(), FieldDeserializers::convertValue))),
    GT_DESERIALIZER((field, node) -> new GreaterThanCondition(field, false, convertValue(node.get($GT)))),
    GTE_DESERIALIZER((field, node) -> new GreaterThanCondition(field, true, convertValue(node.get($GTE)))),
    LT_DESERIALIZER((field, node) -> new LessThanCondition(field, false, convertValue(node.get($LT)))),
    LTE_DESERIALIZER((field, node) -> new LessThanCondition(field, true, convertValue(node.get($LTE)))),
    REGEX_DESERIALIZER((field, node) -> new RegexCondition(field, node.get($REGEX).textValue())),
    CONTAINS_ALL_DESERIALIZER((field, node) -> new ContainsAllCondition(field, getValues(node.get($CONTAINS_ALL).elements(), FieldDeserializers::convertValue))),
    NOT_CONTAINS_ALL_DESERIALIZER((field, node) -> new NotContainsAllCondition(field, getValues(node.get($NOT_CONTAINS_ALL).elements(), FieldDeserializers::convertValue))),
    CONTAINS_ANY_DESERIALIZER((field, node) -> new ContainsAnyCondition(field, getValues(node.get($CONTAINS_ANY).elements(), FieldDeserializers::convertValue))),
    NOT_CONTAINS_ANY_DESERIALIZER((field, node) -> new NotContainsAnyCondition(field, getValues(node.get($NOT_CONTAINS_ANY).elements(), FieldDeserializers::convertValue))),
    STARTS_WITH_DESERIALIZER((field, node) -> new StartsWithCondition(field, node.get($STARTS_WITH).textValue())),
    CONTAINS_DESERIALIZER((field, node) -> new ContainsCondition(field, node.get($CONTAINS).textValue())),
    EMPTY_DESERIALIZER((field, node) -> {
      var value = convertValue(node.get($EMPTY));
      if (value instanceof String valueString) {
        return new EmptyCondition(field, Boolean.parseBoolean(valueString));
      }
      return new EmptyCondition(field, Boolean.TRUE.equals(value));
    });

    final BiFunction<FqlField, JsonNode, FieldCondition<?>> deserializer;

    FieldDeserializers(BiFunction<FqlField, JsonNode, FieldCondition<?>> deserializer) {
      this.deserializer = deserializer;
    }

    private static Object convertValue(JsonNode valueNode) {
      if (valueNode.isBoolean()) {
        return valueNode.booleanValue();
      }
      return valueNode.isNumber() ? valueNode.numberValue() : valueNode.textValue();
    }
  }

  enum LogicalDeserializers {
    AND_DESERIALIZER((node, mapper) -> new AndCondition(getValues(node.elements(), n -> mapper.convertValue(n, FqlCondition.class))));

    final BiFunction<JsonNode, ObjectMapper, LogicalCondition> deserializer;

    LogicalDeserializers(BiFunction<JsonNode, ObjectMapper, LogicalCondition> deserializer) {
      this.deserializer = deserializer;
    }
  }

  private static <T> List<T> getValues(Iterator<JsonNode> iterator, Function<JsonNode, T> valueConverter) {
    List<T> values = new ArrayList<>();
    iterator.forEachRemaining(c -> values.add(valueConverter.apply(c)));
    return values;
  }
}
