package org.folio.fql.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.List;
import org.folio.fql.deserializer.ConditionDeserializer;
import org.folio.fql.deserializer.FqlDeserializer;
import org.folio.fql.deserializer.FqlParsingException;
import org.folio.fql.model.AndCondition;
import org.folio.fql.model.FieldCondition;
import org.folio.fql.model.Fql;
import org.folio.fql.model.FqlCondition;
import org.folio.fql.model.field.FqlField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FqlService {

  private final ObjectMapper mapper;

  @Autowired
  public FqlService() {
    this.mapper = getMapper();
  }

  public Fql getFql(String fqlCriteria) {
    try {
      return mapper.readValue(fqlCriteria, Fql.class);
    } catch (JsonProcessingException e) {
      throw new FqlParsingException(fqlCriteria, e.getMessage());
    }
  }

  public List<FqlField> getFqlFields(Fql fql) {
    FqlCondition<?> fqlCondition = fql.fqlCondition();

    if (fqlCondition instanceof AndCondition andCondition) {
      return andCondition
        .value()
        .stream().map(cnd -> ((FieldCondition<?>) cnd).field()).toList();
    }

    FieldCondition<?> fieldCondition = (FieldCondition<?>) fqlCondition;
    return List.of(fieldCondition.field());
  }

  private static ObjectMapper getMapper() {
    final var mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule()
      .addDeserializer(Fql.class, new FqlDeserializer(mapper))
      .addDeserializer(FqlCondition.class, new ConditionDeserializer(mapper));
    return mapper.registerModule(module);
  }
}
