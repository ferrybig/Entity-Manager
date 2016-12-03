/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.ferrybig.javacoding.entitymanager;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 *
 * @author admin
 */
public class LazyList<T> extends AbstractList<T> implements RandomAccess {
	private List<T> upper = new UnlockedList();
	private Callable<List<T>> function;

	public LazyList(Callable<List<T>> function) {
		this.function = function;
	}
			
	private void loadUpper() {
		if(function == null)
			return;
		try {
			upper = function.call();
		} catch(Exception e) {
			upper = Collections.emptyList();
			throw new IllegalStateException("LazyList has problems accessing upper layer", e);
		} finally {
			function = null;
		}
	}

	@Override
	public int size() {
		return upper.size();
	}

	@Override
	public boolean isEmpty() {
		return upper.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return upper.contains(o);
	}

	@Override
	public Iterator<T> iterator() {
		return upper.iterator();
	}

	@Override
	public Object[] toArray() {
		return upper.toArray();
	}

	@Override
	public <E> E[] toArray(E[] a) {
		return upper.toArray(a);
	}

	@Override
	public boolean add(T e) {
		return upper.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return upper.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return upper.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		return upper.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		return upper.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return upper.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return upper.retainAll(c);
	}

	@Override
	public void replaceAll(UnaryOperator<T> operator) {
		upper.replaceAll(operator);
	}

	@Override
	public void sort(Comparator<? super T> c) {
		upper.sort(c);
	}

	@Override
	public void clear() {
		upper.clear();
	}

	@Override
	public boolean equals(Object o) {
		return upper.equals(o);
	}

	@Override
	public int hashCode() {
		return upper.hashCode();
	}

	@Override
	public T get(int index) {
		return upper.get(index);
	}

	@Override
	public T set(int index, T element) {
		return upper.set(index, element);
	}

	@Override
	public void add(int index, T element) {
		upper.add(index, element);
	}

	@Override
	public T remove(int index) {
		return upper.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return upper.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return upper.lastIndexOf(o);
	}

	@Override
	public ListIterator<T> listIterator() {
		return upper.listIterator();
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		return upper.listIterator(index);
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return upper.subList(fromIndex, toIndex);
	}

	@Override
	public Spliterator<T> spliterator() {
		return upper.spliterator();
	}

	@Override
	public boolean removeIf(Predicate<? super T> filter) {
		return upper.removeIf(filter);
	}

	@Override
	public Stream<T> stream() {
		return upper.stream();
	}

	@Override
	public Stream<T> parallelStream() {
		return upper.parallelStream();
	}

	@Override
	public void forEach(Consumer<? super T> action) {
		upper.forEach(action);
	}

	@Override
	public String toString() {
		if(function != null) {
			return "UnloadedList";
		}
		return upper.toString();
	}

	private class UnlockedList extends AbstractList<T> {

		public UnlockedList() {
		}

		@Override
		public T get(int index) {
			loadUpper();
			return LazyList.this.upper.get(index);
		}

		@Override
		public int size() {
			loadUpper();
			return LazyList.this.upper.size();
		}

		@Override
		public Spliterator spliterator() {
			loadUpper();
			return LazyList.this.upper.spliterator();
		}

		@Override
		public ListIterator listIterator(int index) {
			loadUpper();
			return LazyList.this.upper.listIterator(index);
		}

		@Override
		public ListIterator listIterator() {
			loadUpper();
			return LazyList.this.upper.listIterator();
		}

		@Override
		public Iterator iterator() {
			loadUpper();
			return LazyList.this.upper.iterator();
		}
	}
	
	
}
