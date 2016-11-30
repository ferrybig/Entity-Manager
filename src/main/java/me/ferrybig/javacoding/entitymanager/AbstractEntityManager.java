package me.ferrybig.javacoding.entitymanager;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import me.ferrybig.javacoding.entitymanager.EntityType.FieldInformation;

public abstract class AbstractEntityManager implements EntityManager {

	protected abstract Connection getNewConnection() throws SQLException;

	private static final Set<Class<?>> BASIC_ENTITIES;

	static {
		Set<Class<?>> b = new HashSet<>();
		b.add(Integer.class);
		b.add(Integer.TYPE);
		b.add(Double.class);
		b.add(Double.TYPE);
		b.add(Float.class);
		b.add(Float.TYPE);
		b.add(String.class);
		b.add(byte[].class);
		b.add(Byte.class);
		b.add(Byte.TYPE);
		BASIC_ENTITIES = Collections.unmodifiableSet(b);
	}

	private final Map<Class<?>, EntityType<?>> clazzToEntity = new HashMap<>();
	private final Map<EntityType<?>, Class<?>> entityToClazz = new HashMap<>();
	private final Set<EntityType<?>> registered = new HashSet<>();
	private final Map<EntityType<?>, Map<String, BiExceptionFunction<ResultSet, String, ?>>> loadedTypes = new HashMap<>();

	@Override
	public <T> void register(EntityType<T> type) {
		if(registered.add(type)) {
			clazzToEntity.clear();
			entityToClazz.clear();
		}
	}

	private void bake() {
		if (!clazzToEntity.isEmpty()) {
			return;
		}
		loadedTypes.clear();
		for (EntityType<?> e : registered) {
			clazzToEntity.put(e.getEntityClass(), e);
			entityToClazz.put(e, e.getEntityClass());
		}
		for (EntityType<?> e : registered) {
			loadedTypes.put(e, calculateFieldsForEntity(e));
		}
	}

	private Map<String, BiExceptionFunction<ResultSet, String, ?>> calculateFieldsForEntity(EntityType<?> e) {
		Map<String, BiExceptionFunction<ResultSet, String, ?>> map = new HashMap<>();
		for (Map.Entry<String, FieldInformation> field : e.getEntityFields().entrySet()) {
			BiExceptionFunction<ResultSet, String, ?> f = ResultSet::getObject;
			if (!BASIC_ENTITIES.contains(field.getValue().getType())) {
				if (!clazzToEntity.containsKey(field.getValue().getType())) {
					throw new RuntimeException("Invalid entity detected");
				}
				EntityType<?> other = clazzToEntity.get(field.getValue().getType());
				f = (r, o) -> this.getAllEntitiesBy(other,
						Collections.singletonMap(field.getValue().getJoinedColumn(), r.getObject(o)));
			}
			map.put(field.getKey(), f);
		}
		return map;
	}

	private final ThreadLocal<Connection> threadConnection = new ThreadLocal<Connection>() {
	};

	private Connection getLockedConnection() {
		Connection c = threadConnection.get();
		if (c == null) {
			throw new IllegalStateException("Connection not locked");
		}
		return c;
	}

	private boolean tryConnectionLock() throws SQLException {
		Connection c = threadConnection.get();
		if (c != null) {
			return false;
		}
		c = this.getNewConnection();
		threadConnection.set(c);
		return true;
	}

	@SuppressWarnings("ConvertToTryWithResources")
	private void releaseConnectionLock() throws SQLException {
		Connection c = threadConnection.get();
		if (c == null) {
			throw new IllegalStateException("No connection locked for current thread");
		}
		threadConnection.remove();
		c.close();
	}

	private Map<String, BiExceptionFunction<ResultSet, String, ?>> getFieldsForEntity(EntityType<?> e) {
		bake();
		if (!loadedTypes.containsKey(e)) {
			throw new IllegalArgumentException();
		}
		return loadedTypes.get(e);
	}

	protected <T> T readEntity(EntityType<T> entity, ResultSet s) throws EntityException {
		try {
			Class<T> objClass = entity.getEntityClass();
			T instance = objClass.newInstance();
			for (Map.Entry<String, BiExceptionFunction<ResultSet, String, ?>> field : getFieldsForEntity(entity).entrySet()) {
				Field f = objClass.getDeclaredField(field.getKey());
				f.setAccessible(true);
				Object newVal;
				try {
					newVal = field.getValue().apply(s, field.getKey());
				} catch (Exception e) {
					throw new EntityException("Problem loading " + entity + "." + field.getKey() + ": " + e, e);
				}
				f.set(instance, newVal);
			}
			return instance;
		} catch (InstantiationException | IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException ex) {
			throw new EntityException("Problem loading " + entity + ": " + ex, ex);

		}
	}

	protected abstract PreparedStatement generateInsert(Connection connection, 
			Map<String, Object> set, Map<String, Object> where);

	protected abstract PreparedStatement generateSelect(Connection connection,
			Map<String, Object> where, int from, int limit);

	protected abstract PreparedStatement generateUpdate(Connection connection,
			Map<String, Object> set, Map<String, Object> where);

	protected abstract PreparedStatement generateDelete(Connection connection,
			Map<String, Object> where);

	@Override
	public <T> Optional<T> getEntity(IntegerEntityType<T> clazz, int id) throws EntityException {
		try {
			boolean conLock = tryConnectionLock();
			try {
				Connection c = getLockedConnection();
				try (PreparedStatement p = generateSelect(c, clazz.generateWhereClauseForKey(id), 0, 1)) {
					try (ResultSet r = p.executeQuery()) {
						if (r.next()) {
							return Optional.of(readEntity(clazz, r));
						} else {
							return Optional.empty();
						}
					}
				}

			} finally {
				if (conLock) {
					releaseConnectionLock();
				}
			}
		} catch (SQLException ex) {
			throw new EntityException("Cannot connect to database: " + ex, ex);
		}
	}

	@Override
	public <T, P> Optional<T> getEntity(SimpleEntityType<T, P> clazz, P id) throws EntityException {
		try {
			boolean conLock = tryConnectionLock();
			try {
				Connection c = getLockedConnection();
				try (PreparedStatement p = generateSelect(c, clazz.generateWhereClauseForKey(id), 0, 1)) {
					try (ResultSet r = p.executeQuery()) {
						if (r.next()) {
							return Optional.of(readEntity(clazz, r));
						} else {
							return Optional.empty();
						}
					}
				}

			} finally {
				if (conLock) {
					releaseConnectionLock();
				}
			}
		} catch (SQLException ex) {
			throw new EntityException("Cannot connect to database: " + ex, ex);
		}
	}

	@Override
	public <T, P1, P2> Optional<T> getEntity(DualEntityType<T, P1, P2> clazz, P1 key1, P2 key2) throws EntityException {
		try {
			boolean conLock = tryConnectionLock();
			try {
				Connection c = getLockedConnection();
				try (PreparedStatement p = generateSelect(c, clazz.generateWhereClauseForKey(key1, key2), 0, 1)) {
					try (ResultSet r = p.executeQuery()) {
						if (r.next()) {
							return Optional.of(readEntity(clazz, r));
						} else {
							return Optional.empty();
						}
					}
				}

			} finally {
				if (conLock) {
					releaseConnectionLock();
				}
			}
		} catch (SQLException ex) {
			throw new EntityException("Cannot connect to database: " + ex, ex);
		}
	}

	@Override
	public <T> List<T> getAllEntities(EntityType<T> clazz) throws EntityException {
		try {
			boolean conLock = tryConnectionLock();
			try {
				Connection c = getLockedConnection();
				try (PreparedStatement p = generateSelect(c, Collections.emptyMap(), 0, Integer.MAX_VALUE)) {
					try (ResultSet r = p.executeQuery()) {
						List<T> entities = new ArrayList<>();
						while (r.next()) {
							entities.add(readEntity(clazz, r));
						}
						return entities;
					}
				}

			} finally {
				if (conLock) {
					releaseConnectionLock();
				}
			}
		} catch (SQLException ex) {
			throw new EntityException("Cannot connect to database: " + ex, ex);
		}
	}

	@Override
	public <T> Stream<T> getAllEntitiesLazy(EntityType<T> clazz) throws EntityException {
		return getAllEntities(clazz).stream();
	}

	@Override
	public <T> Stream<T> getAllEntitiesByLazy(EntityType<T> clazz, Map<String, Object> map) throws EntityException {
		return getAllEntitiesBy(clazz, map).stream();
	}

	@Override
	public <T> List<T> getAllEntitiesBy(EntityType<T> clazz, Map<String, Object> map) throws EntityException {
		try {
			boolean conLock = tryConnectionLock();
			try {
				Connection c = getLockedConnection();
				try (PreparedStatement p = generateSelect(c, map, 0, Integer.MAX_VALUE)) {
					p.execute();
					try (ResultSet r = p.executeQuery()) {
						List<T> entities = new ArrayList<>();
						while (r.next()) {
							entities.add(readEntity(clazz, r));
						}
						return entities;
					}
				}
			} finally {
				if (conLock) {
					releaseConnectionLock();
				}
			}
		} catch (SQLException ex) {
			throw new EntityException("Cannot connect to database: " + ex, ex);
		}
	}

	@Override
	public <T> T insert(T entity, EntityType<T> type) throws EntityException {
		try {
			boolean conLock = tryConnectionLock();
			try {
				Connection c = getLockedConnection();
				try (PreparedStatement p = generateUpdate(c, type.generateSetClause(entity), type.generateWhereClause(entity))) {
					int affectedRows = p.executeUpdate();

					if (affectedRows == 0) {
						throw new EntityException("Updating entity failed, no rows affected.");
					}

					try (ResultSet generatedKeys = p.getGeneratedKeys()) {
						if (generatedKeys.next()) {
							generatedKeys.
						}
						else {
							throw new SQLException("Creating user failed, no ID obtained.");
						}
					}
				}

			} finally {
				if (conLock) {
					releaseConnectionLock();
				}
			}
		} catch (SQLException ex) {
			throw new EntityException("Cannot connect to database: " + ex, ex);
		}
	}

	@Override
	public <T> T update(T entity, EntityType<T> type) throws EntityException {
		try {
			boolean conLock = tryConnectionLock();
			try {
				Connection c = getLockedConnection();
				try (PreparedStatement p = generateUpdate(c, type.generateSetClause(entity), type.generateWhereClause(entity))) {
					int affectedRows = p.executeUpdate();
					if (affectedRows == 0) {
						throw new EntityException("Updating entity failed, no rows affected.");
					}
				}
			} finally {
				if (conLock) {
					releaseConnectionLock();
				}
			}
		} catch (SQLException ex) {
			throw new EntityException("Cannot connect to database: " + ex, ex);
		}
	}



	private interface BiExceptionFunction<A1, A2, R> {

		public R apply(A1 a1, A2 a2) throws Exception;
	}
}
