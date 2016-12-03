/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.entitymanager;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.LongSupplier;

/**
 *
 * @author admin
 */
public class QueryCache<T> {
	private final LongSupplier ticker;
	private final long maxSize;
	private final Container<T>[] containers;

	private class Container<T> {
		private long lastUpdate;
		Map<Map<String, Object>, Node<T>> internalMap = new LinkedHashMap<Map<String, Object>, Node<T>>() {
			
			private static final long serialVersionUID = 3109256773218160485L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<Map<String, Object>, Node<T>> eldest) {
				return size() > maxSize || eldest.getValue().expires < ticker.getAsLong();
			}
			
		};
	}
	
	private class Node<T> {

		T value;
		long expires;
		
	}
	
	private static final class Key {
		private final String[] keys;
		private final Object[] values;
		private final int hashcode;

		public Key(String[] keys, Object[] values) {
			this.keys = keys;
			this.values = values;
			this.hashcode = hashCode0();
		}

		@Override
		public int hashCode() {
			return hashcode;
		}
		
		private int hashCode0() {
			int hash = 7;
			hash = 53 * hash + Arrays.deepHashCode(this.keys);
			hash = 53 * hash + Arrays.deepHashCode(this.values);
			return hash;
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
			final Key other = (Key) obj;
			if (!Arrays.deepEquals(this.keys, other.keys)) {
				return false;
			}
			if (!Arrays.deepEquals(this.values, other.values)) {
				return false;
			}
			return true;
		}
		
		
		
		public boolean equalsToMap(Map<String, Object> other) {
			
		}
		
		public static Key fromMap(Map<String, Object> other) {
			SortedMap<String, Object> sorted = new TreeMap<>(other);
			int size = other.size();
			String[] keys = new String[size];
			Object[] values = new Object[size];
			
			return new Key(keys, values);
		}
	}
	
	
}
