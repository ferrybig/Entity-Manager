
package me.ferrybig.javacoding.entitymanager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface EntityManager {
	public <T> Optional<T> getEntity(IntegerEntityType<T> clazz, int id) throws EntityException;
	
	public <T, P> Optional<T> getEntity(SimpleEntityType<T, P> clazz, P id) throws EntityException;
	
	public <T, P1, P2> Optional<T> getEntity(DualEntityType<T, P1, P2> clazz, P1 key1, P2 key2) throws EntityException;
	
	public default <T> List<T> getAllEntities(EntityType<T> clazz) throws EntityException {
		return getAllEntitiesLazy(clazz).collect(Collectors.toList());
	}
	
	public <T> Stream<T> getAllEntitiesLazy(EntityType<T> clazz) throws EntityException;
	
	public default <T> List<T> getAllEntitiesBy(EntityType<T> clazz, Map<String, Object> map) throws EntityException {
		return getAllEntitiesByLazy(clazz, map).collect(Collectors.toList());
	}
	
	public <T> Stream<T> getAllEntitiesByLazy(EntityType<T> clazz, Map<String, Object> map) throws EntityException;

	public <T> void register(EntityType<T> type);

	public <T> T insert(T entity, EntityType<T> type) throws EntityException;

	public <T> T update(T entity, EntityType<T> type) throws EntityException;
}
