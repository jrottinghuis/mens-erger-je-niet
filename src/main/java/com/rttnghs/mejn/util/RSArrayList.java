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
package com.rttnghs.mejn.util;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.iterators.ReverseListIterator;

/**
 * Convenience class that wraps {@link ArrayList} and adds a method to get a
 * reverseStream from the list.
 * <p>
 * Sample use:
 * 
 * <pre>
 * 	RSList<Integer> list = new RSArrayList<Integer>(Arrays.asList(4,2,3,5,6,1, 8, 9, 10, 11));
 * 
 * 	Stream<Integer> stream = list.stream();
 * 	stream.forEach(p -> System.out.println(p)); // regular order
 * 
 * 	Stream<Integer> reverseStream = list.reverseStream();
 * 	reverseStream.forEach(p -> System.out.println(p)); // reversed order
 * }
 * </pre>
 * 
 * @param <E>
 */
public class RSArrayList<E> extends ArrayList<E> implements RSList<E> {

	@Serial
	private static final long serialVersionUID = 2100071895313519943L;

	/**
	 * See {@link #ArrayList()}
	 */
	public RSArrayList() {
		super();
	}

	/**
	 * Constructs an empty list with the specified initial capacity.
	 *
	 * @param initialCapacity the initial capacity of the list
	 * @throws IllegalArgumentException if the specified initial capacity is
	 *                                  negative
	 */
	public RSArrayList(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Constructs a list containing the elements of the specified collection, in the
	 * order they are returned by the collection's iterator.
	 *
	 * @param c the collection whose elements are to be placed into this list
	 * @throws NullPointerException if the specified collection is null
	 */
	public RSArrayList(Collection<? extends E> c) {
		super(c);
	}

	@Override
	public Stream<E> reverseStream() {
		ReverseListIterator<E> reverseListIterator = new ReverseListIterator<>(this);
		Iterable<E> reverseIterable = () -> reverseListIterator;
        return StreamSupport.stream(reverseIterable.spliterator(), false);
	}
}
