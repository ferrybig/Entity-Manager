/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.entitymanager;

import java.beans.ConstructorProperties;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class DefaultEntityType<T> implements EntityType<T> {

	private final Class<T> clazz;
	private Map<String, FieldInformation> entityFields = null;
	private static final Logger LOG = Logger.getLogger(DefaultEntityType.class.getName());

	public DefaultEntityType(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public Class<T> getEntityClass() {
		return clazz;
	}

	@Override
	public String getTableName() {
		return clazz.getAnnotation(Entity.class).table();
	}

	@Override
	public T newInstance(Map<String, Object> fields) throws EntityException {
		assert fields.keySet().containsAll(getEntityFields().keySet()) && fields.size() == getEntityFields().size();
		try {
			Class<T> objClass = getEntityClass();
			T instance = makeCleanEntity(fields);
			for (Map.Entry<String, ?> field : fields.entrySet()) {
				Field f = objClass.getDeclaredField(field.getKey());
				if (!Modifier.isPublic(f.getModifiers())
						|| !Modifier.isPublic(f.getDeclaringClass().getModifiers())) {
					f.setAccessible(true);
				}
				try {
					f.set(instance, field.getValue());
				} catch (IllegalAccessException | IllegalArgumentException e) {
					throw new EntityException("Problem loading " + this + "." + field.getKey(), e);
				}
			}
			return instance;
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException ex) {
			throw new EntityException("Cannot save values of " + this + ": " + ex, ex);
		}
	}

	protected T makeCleanEntity(Map<String, Object> fields) throws
			IllegalAccessException, InstantiationException, IllegalArgumentException,
			InvocationTargetException, SecurityException, EntityException {
		Class<T> objClass = getEntityClass();
		Constructor<T> bestConstructor = null;
		Set<String> unneededKeys = new HashSet<>();
		Object[] constructorArguments = new Object[0];
		for (Constructor<?> c : objClass.getDeclaredConstructors()) {
			ConstructorProperties prop = c.getAnnotation(ConstructorProperties.class);
			boolean isValid;
			int parameterCount = c.getParameterCount();
			if (parameterCount == 0) {
				isValid = true;
			} else if (prop == null) {
				isValid = false;
			} else {
				if (fields.keySet().containsAll(Arrays.asList(prop.value()))) {
					isValid = true;
				} else {
					isValid = false;
				}
			}
			if (!isValid) {
				continue;
			}
			if (parameterCount > constructorArguments.length || bestConstructor == null) {
				{
					@SuppressWarnings("unchecked")
					// Needs to be unchecked, compiler will optimalize this block away
					Constructor<T> casted = (Constructor<T>) c;
					bestConstructor = casted;
				}
				if (parameterCount == 0) {
					continue;
				}
				@SuppressWarnings("null") // Not null, guarded by the isValid && parameterCount check
				String[] args = prop.value();
				constructorArguments = new Object[args.length];
				for (int i = 0; i < args.length; i++) {
					constructorArguments[i] = fields.get(args[i]);
					unneededKeys.add(args[i]);
				}
			}
		}
		if (bestConstructor == null) {
			throw new EntityException("No constructor found for " + objClass + " that are suitable."
					+ "Did you forgot @ConstructorProperties ?");
		}
		if (!Modifier.isPublic(bestConstructor.getModifiers())
				|| !Modifier.isPublic(bestConstructor.getDeclaringClass().getModifiers())) {
			bestConstructor.setAccessible(true);
		}
		fields.keySet().removeAll(unneededKeys);
		LOG.log(Level.FINEST, "Using constructor {0}", bestConstructor);
		return bestConstructor.newInstance(constructorArguments);
	}

	@Override
	public Map<String, FieldInformation> getEntityFields() {
		if (entityFields == null) {
			entityFields = getEntityFields0();
		}
		return entityFields;
	}

	private Map<String, FieldInformation> getEntityFields0() {
		Map<String, FieldInformation> info = new TreeMap<>();
		for (Field f : clazz.getDeclaredFields()) {
			Column column = f.getAnnotation(Column.class);
			if (column == null) {
				continue;
			}
			JoinedColumn joinedColumn = f.getAnnotation(JoinedColumn.class);
			Class<?> type = f.getType();
			boolean hasList = Collection.class.isAssignableFrom(f.getType());
			//todo properly detect last parameter
			if (hasList) {
				ParameterizedType stringListType = (ParameterizedType) f.getGenericType();
				type = (Class<?>) stringListType.getActualTypeArguments()[0];
			}
			info.put(f.getName(), new FieldInformation(column.column(), type,
					joinedColumn == null ? null : joinedColumn.joinedField(), hasList));
		}
		return info;
	}

	@Override
	public Map<String, Object> generateSetClause(T object) {
		Map<String, Object> info = new HashMap<>();
		for (Field f : clazz.getDeclaredFields()) {
			try {
				Column column = f.getAnnotation(Column.class);
				f.setAccessible(true);
				if (column == null) {
					continue;
				}
				if (Collection.class.isAssignableFrom(f.getType())) {
					continue;
				}
				info.put(column.column(), f.get(object));
			} catch (IllegalArgumentException | IllegalAccessException ex) {
				throw new RuntimeException(ex);
			}
		}
		return info;
	}

	@Override
	public Map<String, Object> generateWhereClause(T object) {
		Map<String, Object> info = new HashMap<>();
		for (Field f : clazz.getDeclaredFields()) {
			try {
				Column column = f.getAnnotation(Column.class);
				f.setAccessible(true);
				if (column == null) {
					continue;
				}
				if (!column.primaryKey()) {
					continue;
				}
				if (Collection.class.isAssignableFrom(f.getType())) {
					continue;
				}
				info.put(column.column(), f.get(object));
			} catch (IllegalArgumentException | IllegalAccessException ex) {
				throw new RuntimeException(ex);
			}
		}
		return info;
	}

	@Override
	public Map<String, Object> generateWhereClauseByMatchedFields(Map<String, Object> fields) {
		return fields; // Unsure what the purpose of this method was...
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 53 * hash + Objects.hashCode(this.clazz);
		return hash;
	}

	@Override
	public String toString() {
		return "DefaultEntityType{" + "clazz=" + clazz + '}';
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final DefaultEntityType<?> other = (DefaultEntityType<?>) obj;
		if (!Objects.equals(this.clazz, other.clazz)) {
			return false;
		}
		return true;
	}

}
