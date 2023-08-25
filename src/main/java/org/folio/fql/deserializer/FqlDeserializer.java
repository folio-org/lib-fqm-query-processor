package org.folio.fql.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.Fql;

import java.io.IOException;

public class FqlDeserializer extends StdDeserializer<Fql> {
  private final ObjectMapper mapper;

  public FqlDeserializer(ObjectMapper mapper) {
    super(Fql.class);
    this.mapper = mapper;
  }

  @Override
  public Fql deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    return new Fql(mapper.convertValue(node, FqlCondition.class));
  }
}
