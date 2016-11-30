
package me.ferrybig.javacoding.entitymanager;

import java.util.Map;

public interface IntegerEntityType<T> extends EntityType<T> {

	public Map<String, Object> generateWhereClauseForKey(int type);
}
