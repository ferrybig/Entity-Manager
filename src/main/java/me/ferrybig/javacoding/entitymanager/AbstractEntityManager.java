package me.ferrybig.javacoding.entitymanager;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import me.ferrybig.javacoding.entitymanager.EntityType.FieldInformation;

public abstract class AbstractEntityManager implements EntityManager {

	protected static final Logger LOG = Logger.getLogger(AbstractEntityManager.class.getName());

	public static final Set<Class<?>> BASIC_ENTITIES;

	static {
		Set<Class<?>> b = new HashSet<>();
		b.add(Integer.class);
		b.add(Integer.TYPE);
		b.add(Double.class);
		b.add(Double.TYPE);
		b.add(Float.class);
		b.add(Float.TYPE);
		b.add(Long.class);
		b.add(Long.TYPE);
		b.add(String.class);
		b.add(byte[].class);
		b.add(Byte.class);
		b.add(Byte.TYPE);
		BASIC_ENTITIES = Collections.unmodifiableSet(b);
	}

	private final Map<Class<?>, EntityType<?>> clazzToEntity = new HashMap<>();
	private final Map<EntityType<?>, Class<?>> entityToClazz = new HashMap<>();
	private final Set<EntityType<?>> registered = new HashSet<>();
	private final Map<EntityType<?>, Map<String, ExceptionFunction<ResultSet, ?>>> loadedTypes = new HashMap<>();
	private final Map<EntityType<?>, List<String>> dbFields = new HashMap<>();
	private final ThreadLocal<Connection> threadConnection = new ThreadLocal<Connection>() {
	};

	protected abstract Connection getNewConnection() throws SQLException;

	private void checkType(EntityType<?> type) throws EntityException {
		if (!registered.contains(type)) {
			throw new EntityException("Entity type " + type + " not known to the manager");
		}
		bake();
	}

	@Override
	public <T> EntityType<T> getKnownEntityTypeForObject(T obj) throws EntityException {
		bake();
		@SuppressWarnings("unchecked")
		EntityType<T> t = (EntityType<T>) clazzToEntity.get(obj.getClass());
		if (t == null) {
			throw new EntityException("Entity class " + obj.getClass() + " not known to the manager");
		}
		return t;
	}

	@Override
	public <T> void register(EntityType<T> type) {
		if (registered.add(type)) {
			clazzToEntity.clear();
			entityToClazz.clear();
		}
	}

	private void bake() throws EntityException {
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
			List<String> fields = new ArrayList<>();
			for (Map.Entry<String, FieldInformation> entry : e.getEntityFields().entrySet()) {
				if (entry.getValue().isList()) {
					continue;
				}
				fields.add(entry.getValue().getColumn());
			}
			fields.sort(null);
			dbFields.put(e, fields);
		}
	}

	private Map<String, ExceptionFunction<ResultSet, ?>> calculateFieldsForEntity(EntityType<?> e) throws EntityException {
		Map<String, ExceptionFunction<ResultSet, ?>> map = new HashMap<>();
		for (Map.Entry<String, FieldInformation> field : e.getEntityFields().entrySet()) {
			FieldInformation fieldInfo = field.getValue();
			String column = fieldInfo.getColumn();
			ExceptionFunction<ResultSet, ?> f = r -> r.getObject(column);
			if (!BASIC_ENTITIES.contains(fieldInfo.getType())) {
				Class<?> innerType = fieldInfo.getType();
				EntityType<?> other = clazzToEntity.get(innerType);
				if (Enum.class.isAssignableFrom(innerType)) {
					f = (r) -> {
						int index = r.getInt(column);
						if(r.wasNull())
							return null;
						return innerType.getEnumConstants()[index];
					};
				} else if (other == null) {
					EntityException ex = new EntityException("Invalid entity detected, tried to resolve "
							+ e + "." + field.getKey() + " but coulnd't find " + innerType);
					if (fieldInfo.isList()) {
						f = (r) -> new LazyList<>(() -> {
							throw (EntityException) ex.fillInStackTrace();
						});
					} else {
						throw ex;
					}
				} else {
					if (fieldInfo.isList()) {
						f = (r) -> {
							Object master = r.getObject(column);
							return new LazyList<>(() -> this.select(other, Collections.singletonMap(fieldInfo.getJoinedColumn(), master)));
						};
					} else {
						f = (r) -> this.select(other,
								Collections.singletonMap(fieldInfo.getJoinedColumn(), r.getObject(column)))
								.stream().findAny().orElse(null);
					}
				}
			}

			ExceptionFunction<ResultSet, ?> finalFunction = f;
			map.put(field.getKey(), finalFunction);
		}
		return map;
	}

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
		threadConnection.remove(); // remove in case the above line added it back
		if (c == null) {
			throw new IllegalStateException("No connection locked for current thread");
		}
		c.close();
	}

	private Map<String, Object> replaceEntityReferences(Map<String, Object> orginal) throws EntityException {
		for (Map.Entry<String, Object> entry : orginal.entrySet()) {
			Object value = entry.getValue();
			if (value == null) {
				continue;
			}
			if (this.clazzToEntity.containsKey(value.getClass())) {
				@SuppressWarnings("unchecked")
				EntityType<Object> type = (EntityType<Object>) this.clazzToEntity.get(value.getClass());
				Map<String, Object> orginalWhere = type.generateWhereClause(value);
				if (orginalWhere.size() != 1) {
					throw new EntityException("No suiteable primary key found for reference in " + type);
				}
				entry.setValue(orginalWhere.values().iterator().next());
			} else if (value instanceof Enum<?>) {
				entry.setValue(((Enum<?>)value).ordinal());
			}
		}
		return orginal;
	}

	private Map<String, ExceptionFunction<ResultSet, ?>> getFieldsForEntity(EntityType<?> e) throws EntityException {
		bake();
		if (!loadedTypes.containsKey(e)) {
			throw new IllegalArgumentException();
		}
		return loadedTypes.get(e);
	}

	protected <T> T readEntity(EntityType<T> entity, ResultSet s) throws EntityException {
		try {
			Map<String, Object> data = new HashMap<>();
			for (Map.Entry<String, ExceptionFunction<ResultSet, ?>> field : getFieldsForEntity(entity).entrySet()) {
				try {
					data.put(field.getKey(), field.getValue().apply(s));
				} catch (Exception e) {
					throw new EntityException("Problem parsing " + entity + "." + field.getKey(), e);
				}
			}
			return entity.newInstance(data);
		} catch (SecurityException | IllegalArgumentException ex) {
			throw new EntityException("Problem loading " + entity + ": " + ex, ex);

		}
	}

	protected abstract PreparedStatement generateInsert(Connection connection,
			String table, Map<String, Object> set)
			throws SQLException;

	protected abstract PreparedStatement generateSelect(Connection connection,
			String table, List<String> fields, Map<String, Object> where, int from, int limit)
			throws SQLException;

	protected abstract PreparedStatement generateUpdate(Connection connection,
			String table, Map<String, Object> set, Map<String, Object> where)
			throws SQLException;

	protected abstract PreparedStatement generateDelete(Connection connection,
			String table, Map<String, Object> where)
			throws SQLException;

	protected <T> Optional<T> getEntity(EntityType<T> clazz, Map<String, Object> where) throws EntityException {
		checkType(clazz);
		try {
			boolean conLock = tryConnectionLock();
			try {
				Connection c = getLockedConnection();
				try (PreparedStatement p = generateSelect(c, clazz.getTableName(),
						this.dbFields.get(clazz), replaceEntityReferences(where), 0, 1)) {
					try (ResultSet r = p.executeQuery()) {
						if (r.next()) {
							Optional<T> entity = Optional.of(readEntity(clazz, r));
							if (r.next()) {
								throw new EntityException("query " + where + " on " + clazz + " retunred multiple entities");
							}
							return entity;
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
	public <T> Optional<T> select(IntegerEntityType<T> clazz, int id) throws EntityException {
		return getEntity(clazz, clazz.generateWhereClauseForKey(id));
	}

	@Override
	public <T, P> Optional<T> select(SimpleEntityType<T, P> clazz, P id) throws EntityException {
		return getEntity(clazz, clazz.generateWhereClauseForKey(id));
	}

	@Override
	public <T, P1, P2> Optional<T> select(DualEntityType<T, P1, P2> clazz, P1 key1, P2 key2) throws EntityException {
		return getEntity(clazz, clazz.generateWhereClauseForKey(key1, key2));
	}

	@Override
	public <T> List<T> select(EntityType<T> clazz) throws EntityException {
		return select(clazz, Collections.emptyMap());
	}

	@Override
	public <T> Stream<T> selectLazy(EntityType<T> clazz) throws EntityException {
		return select(clazz).stream();
	}

	@Override
	public <T> Stream<T> selectLazy(EntityType<T> clazz, Map<String, Object> map) throws EntityException {
		return select(clazz, map).stream();
	}

	@Override
	public <T> List<T> select(EntityType<T> clazz, Map<String, Object> map) throws EntityException {
		checkType(clazz);
		try {
			boolean conLock = tryConnectionLock();
			try {
				Connection c = getLockedConnection();
				try (PreparedStatement p = generateSelect(c, clazz.getTableName(),
						this.dbFields.get(clazz), replaceEntityReferences(map), 0, Integer.MAX_VALUE)) {
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
		checkType(type);
		try {
			boolean conLock = tryConnectionLock();
			try {
				Connection c = getLockedConnection();
				Map<String, Object> where = type.generateWhereClause(entity);
				Map<String, Object> data = type.generateSetClause(entity);
				if (type.isPrimaryKeyAutogenerated()) {
					data.keySet().removeAll(where.keySet());
				}
				try (PreparedStatement p = generateInsert(c, type.getTableName(), replaceEntityReferences(data))) {
					int affectedRows = p.executeUpdate();

					if (affectedRows == 0) {
						throw new EntityException("Inserting entity failed, no rows affected.");
					}

					try (ResultSet generatedKeys = p.getGeneratedKeys()) {

						if (generatedKeys.next()) {
							ResultSetMetaData meta = generatedKeys.getMetaData();
							int size = meta.getColumnCount();
							Iterator<Map.Entry<String, Object>> itr
									= new ArrayList<>(where.entrySet()).iterator();
							for (int i = 1; i <= size || itr.hasNext(); i++) {
								where.put(itr.next().getKey(), generatedKeys.getObject(i));
							}
							// assert i <= size && itr.hasNext();
							return getEntity(type, where).orElseThrow(
									() -> new EntityException("Entity failed to save correctly, cannot find it using " + where));
						} else {
							// No keys generated for this entity
							return entity;
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
				try (PreparedStatement p = generateUpdate(c, type.getTableName(),
						replaceEntityReferences(type.generateSetClause(entity)),
						replaceEntityReferences(type.generateWhereClause(entity)))) {
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
		return entity;
	}

	@Override
	public <T> void delete(T entity, EntityType<T> type) throws EntityException {
		try {
			boolean conLock = tryConnectionLock();
			try {
				Connection c = getLockedConnection();
				try (PreparedStatement p = generateDelete(c, type.getTableName(),
						replaceEntityReferences(type.generateWhereClause(entity)))) {
					int affectedRows = p.executeUpdate();
					if (affectedRows == 0) {
						throw new EntityException("Deleting entity failed, no rows affected.");
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

	private interface ExceptionFunction<A, R> {

		public R apply(A a) throws Exception;
	}
}
