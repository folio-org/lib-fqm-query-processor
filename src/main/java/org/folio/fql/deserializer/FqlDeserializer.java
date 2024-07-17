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
    ObjectNode node = jsonParser.getCodec().readTree(jsonParser);

    String version = node.has(VERSION_KEY) ? node.get(VERSION_KEY).asText() : "0";
    node.remove(VERSION_KEY);

    if (node.isEmpty()) {
      // the query is just _version, no condition
      return new Fql(version, null);
    } else {
      FqlCondition<?> fqlCondition = mapper.convertValue(node, FqlCondition.class);

      return new Fql(version, fqlCondition);
    }
  }
}
