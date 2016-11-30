
package me.ferrybig.javacoding.entitymanager;

import java.util.Map;

public interface EntityType<T> {

	public Class<T> getEntityClass();

	public T createEntity(Map<String, Object> row);

	public Map<String, FieldInformation> getEntityFields();

	public Map<String, Object> generateSetClause(T object);

	public Map<String, Object> generateWhereClause(T object);

	public Map<String, Object> generateWhereClauseByMatchedFields(Map<String, Object> fields);

	public class FieldInformation {
		private final String column;
		private final Class<?> type;
		private final String joinedColumn;
		private final boolean list;

		public FieldInformation(String column, Class<?> type) {
			this(column, type, null);
		}

		public FieldInformation(String column, Class<?> type, String joinedColumn) {
			this(column, type, null, false);
		}

		public FieldInformation(String column, Class<?> type, String joinedColumn, boolean list) {
			this.column = column;
			this.type = type;
			this.joinedColumn = joinedColumn;
			this.list = list;
		}

		public String getColumn() {
			return column;
		}

		public Class<?> getType() {
			return type;
		}

		public String getJoinedColumn() {
			return joinedColumn;
		}

	}
	
}
