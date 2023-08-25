package org.folio.fqm.lib.repository;

import org.folio.querytool.domain.dto.ColumnValueGetter.TypeEnum;
import org.folio.querytool.domain.dto.EntityTypeColumn;
import org.jooq.Field;

import java.util.Map;
import java.util.function.UnaryOperator;

import static org.jooq.impl.DSL.field;

/**
 * Class responsible for identifying the SQL field, corresponding to an entity type column
 */
public class SqlFieldIdentificationService {
  private static final Map<TypeEnum, UnaryOperator<String>> MAPPERS = Map.of(
    TypeEnum.RMB_JSONB, param -> "jsonb ->> '" + param + "'"
  );

  /**
   * Returns the SQL field corresponding to an entity type column
   */
  public Field<Object> getSqlField(EntityTypeColumn entityTypeColumn) {
    if (entityTypeColumn.getValueGetter() == null) {
      // No value getter is defined for this column. Use column name as the SQL field name.
      return field(entityTypeColumn.getName());
    }
    var valueGetter = entityTypeColumn.getValueGetter();
    UnaryOperator<String> mapperFunction = MAPPERS.get(valueGetter.getType());
    String sqlFieldName = mapperFunction.apply(valueGetter.getParam());
    return field(sqlFieldName);
  }
}
