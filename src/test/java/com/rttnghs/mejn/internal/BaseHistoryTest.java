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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.rttnghs.mejn.History;

class BaseHistoryTest {

	@Test
	final void testSize() {
		BaseHistory<String> history = new BaseHistory<String>(Arrays.asList("A", "B", "C"));
		assertEquals(3, history.size());
		history.add("D");
		assertTrue(history.add("E"));
		assertFalse(history.add(null));
		assertEquals(5, history.size());
	}

	@Test
	final void testStream() {
		BaseHistory<String> history = new BaseHistory<String>(Arrays.asList("A", "B", "C"));
		history.add("D");
		history.add("E");

		List<String> historyList = history.stream().collect(Collectors.toList());
		List<String> expected = Arrays.asList("A", "B", "C", "D", "E");
		assertEquals(expected, historyList);
	}

	@Test
	final void testReverseStream() {
		BaseHistory<Integer> history = new BaseHistory<Integer>();
		history.addAll(Arrays.asList(1, 2, 3, 4));
		history.add(5);

		List<Integer> historyList = history.reverseStream().collect(Collectors.toList());
		List<Integer> expected = Arrays.asList(5, 4, 3, 2, 1);
		assertEquals(expected, historyList);
	}

	@Test
	final void testGetSupplier() {
		BaseHistory<Integer> history = new BaseHistory<Integer>();
		history.addAll(Arrays.asList(1, 2, 3, 4));
		history.add(5);

		Supplier<History<Integer>> supplier = history.getSupplier(x -> x * x);
		History<Integer> squared = supplier.get();

		assertEquals(5, squared.size());

		List<Integer> squaredList = squared.stream().collect(Collectors.toList());
		List<Integer> expected = Arrays.asList(1, 4, 9, 16, 25);
		assertEquals(expected, squaredList);

		history.add(6);

		supplier = history.getSupplier(x -> x * x);
		squared = supplier.get();

		squaredList = squared.reverseStream().collect(Collectors.toList());
		expected = Arrays.asList(36, 25, 16, 9, 4, 1);
		assertEquals(expected, squaredList);
	}

	@Test
	final void testToString() {
		BaseHistory<Integer> history = new BaseHistory<Integer>();
		history.addAll(Arrays.asList(2, 4, 6, 8));
		history.add(17);
		assertEquals("[2, 4, 6, 8, 17]", history.toString());

		history.add(3);
		Supplier<History<Integer>> supplier = history.getSupplier(x -> x * x);
		assertEquals("[4, 16, 36, 64, 289, 9]", supplier.get().toString());
	}

	@Test
	final void testAddAll() {
		BaseHistory<String> history = new BaseHistory<>(Arrays.asList("A", "B", "C"));
		BaseHistory<String> other = new BaseHistory<>(Arrays.asList("D", "E", "F"));

		// Make a copy before adding more to the original.
		BaseHistory<String> copy = new BaseHistory<String>(1).addAll(history);
		assertEquals(3, copy.size());
		Collection<String> nothing = null;
		assertFalse(copy.addAll(nothing));
		nothing = new ArrayList<>();
		assertFalse(copy.addAll(nothing));

		history.addAll(other);

		// Make sure that copy is not affected
		assertEquals(3, copy.size());

		List<String> expected = Arrays.asList("A", "B", "C", "D", "E", "F");
		assertEquals(expected, history.stream().collect(Collectors.toList()));

		copy.addAll(other);
		assertEquals(expected, history.stream().collect(Collectors.toList()));

	}

}
