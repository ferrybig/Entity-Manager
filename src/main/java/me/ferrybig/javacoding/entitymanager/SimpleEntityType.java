
package me.ferrybig.javacoding.entitymanager;

import java.util.Map;

public interface SimpleEntityType<T, P> extends EntityType<T> {

	public Map<String, Object> generateWhereClauseForKey(P type);

}
