
package me.ferrybig.javacoding.entitymanager;

import java.util.Map;

public interface DualEntityType<T, P1, P2> extends EntityType<T> {
	public Map<String, Object> generateWhereClauseForKey(P1 key1, P2 key2);
}
