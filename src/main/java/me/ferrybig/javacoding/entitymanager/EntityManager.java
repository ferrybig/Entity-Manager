
package me.ferrybig.javacoding.entitymanager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface EntityManager extends AutoCloseable {
	
	public <T> EntityType<T> getKnownEntityTypeForObject(T obj) throws EntityException;
	
	public <T> Optional<T> select(IntegerEntityType<T> clazz, int id) throws EntityException;
	
	public <T, P> Optional<T> select(SimpleEntityType<T, P> clazz, P id) throws EntityException;
	
	public <T, P1, P2> Optional<T> select(DualEntityType<T, P1, P2> clazz, P1 key1, P2 key2) throws EntityException;
	
	public default <T> List<T> select(EntityType<T> clazz) throws EntityException {
		return selectLazy(clazz).collect(Collectors.toList());
	}
	
	public default <T> Stream<T> selectLazy(EntityType<T> clazz) throws EntityException {
		return selectLazy(clazz, Collections.emptyMap());
	}
	
	public default <T> List<T> select(EntityType<T> clazz, Map<String, Object> map) throws EntityException {
		return EntityManager.this.selectLazy(clazz, map).collect(Collectors.toList());
	}
	
	public <T> Stream<T> selectLazy(EntityType<T> clazz, Map<String, Object> map) throws EntityException;

	public <T> void register(EntityType<T> type);

	// TODO refractor out entity types
	public <T> T insert(T entity, EntityType<T> type) throws EntityException;

	public <T> T update(T entity, EntityType<T> type) throws EntityException;
	
	public <T> void delete(T entity, EntityType<T> type) throws EntityException;
	
	public default <T> T insert(T entity) throws EntityException {
		return insert(entity, getKnownEntityTypeForObject(entity));
	}
	
	public default <T> T update(T entity) throws EntityException {
		return update(entity, getKnownEntityTypeForObject(entity));
	}
	
	public default <T> void delete(T entity) throws EntityException {
		delete(entity, getKnownEntityTypeForObject(entity));
	}
	
	@Override
	public default void close() throws EntityException {
	}
}
