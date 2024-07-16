package org.folio.fql.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;

public class FqlDeserializer extends StdDeserializer<Fql> {

  private static final String VERSION_KEY = "_version";
  private final ObjectMapper mapper;

  public FqlDeserializer(ObjectMapper mapper) {
    super(Fql.class);
    this.mapper = mapper;
  }

  @Override
  public Fql deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);

    int version = node.has(VERSION_KEY) ? node.get(VERSION_KEY).asInt() : 0;
    ((ObjectNode) node).remove(VERSION_KEY);

    FqlCondition<?> fqlCondition = mapper.convertValue(node, FqlCondition.class);

    return new Fql(version, fqlCondition);
  }
}
