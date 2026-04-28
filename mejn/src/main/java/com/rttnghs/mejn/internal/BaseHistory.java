/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rttnghs.mejn.internal;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.rttnghs.mejn.History;
import com.rttnghs.mejn.util.RSArrayList;

/**
 * Means to keep track of history, and to be able to pass that history along
 * through a supplier, without the need to actually get the user relying on the
 * {@code History} interface to be able to modify it.
 * 
 * @param <E>
 */
public class BaseHistory<E> implements HistorySupplier<E>, History<E> {

	private final RSArrayList<E> history;

	public BaseHistory() {
		history = new RSArrayList<>();
	}

	public BaseHistory(int initialCapacity) {
		history = new RSArrayList<>(initialCapacity);
	}

	public BaseHistory(Collection<? extends E> c) {
		history = new RSArrayList<>(c);
	}

	@Override
	public int size() {
		return history.size();
	}

	@Override
	public Stream<E> stream() {
		return history.stream();
	}

	@Override
	public Stream<E> reverseStream() {
		return history.reverseStream();
	}

	@Override
	public String toString() {
		return history.toString();
	}

	/**
	 * Appends the specified element to the end of the history.
	 * 
	 * @param element to be appended to the history
	 * @return Returns {@code true} if this history changed as a result of the call.
	 *         <p>
	 *         Returns {@code false} if element is null.
	 */
	public boolean add(E element) {
		if (element == null) {
			return false;
		}
		return history.add(element);
	}

	/**
	 * Appends all of the elements in the specified collection to the end of this
	 * history, in the order that they are returned by the specified collection's
	 * Iterator. The behavior of this operation is undefined if the specified
	 * collection is modified while the operation is in progress. (This implies that
	 * the behavior of this call is undefined if the specified collection is this
	 * list, and this list is nonempty.)
	 * 
	 * @param c collection containing elements to be added to this history
	 * @return Returns {@code true} if this history changed as a result of the call.
	 */
	public boolean addAll(Collection<? extends E> c) {
		if (c == null) {
			return false;
		}
		return history.addAll(c);
	}
	
	/**
	 * @param other other history to add to this history.
	 * @return reference to self for chaining.
	 */
	public BaseHistory<E> addAll(History<? extends E> other) {
		if ((other != null) && (other.size() > 0)) {
			other.stream().forEachOrdered(this::add);
		}
		return this;
	}

	@Override
	public Supplier<History<E>> getSupplier(UnaryOperator<E> operator) {
		BaseHistory<E> wrapped = this;
		History<E> history = new History<>() {

			@Override
			public int size() {
				return wrapped.size();
			}

			@Override
			public Stream<E> stream() {
				return wrapped.stream().map(operator);
			}

			@Override
			public Stream<E> reverseStream() {
				return wrapped.reverseStream().map(operator);
			}

			@Override
			public String toString() {
				return this.stream().toList().toString();
			}
		};
		return () -> history;
	}

}
