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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class RSArrayListTest {

	@Test
	final void testReversableStreamArrayList() {
		RSList<Integer> list = new RSArrayList<Integer>(Arrays.asList(4, 2, 3, 5, 6, 1, 8, 9, 10, 11));

		Stream<Integer> stream = list.stream();
		boolean print = false;
		if (print) {
			stream.forEach(p -> System.out.println(p));
		}

		stream = list.reverseStream();
		List<Integer> result = stream.collect(Collectors.toList());
		Collections.reverse(list);

		assertEquals(list, result);

		// Do this again with a different constructor
		list = new RSArrayList<Integer>();
		list.addAll(Arrays.asList(4, 2, 3, 5, 6, 1, 8, 9, 10, 11));
		stream = list.stream();
		stream = list.reverseStream();
		result = stream.collect(Collectors.toList());
		Collections.reverse(list);
		assertEquals(list, result);

		// Do this again with a different constructor
		list = new RSArrayList<Integer>(4);
		list.addAll(Arrays.asList(4, 2, 3, 5, 6, 1, 8, 9, 10, 11));
		stream = list.stream();
		stream = list.reverseStream();
		result = stream.collect(Collectors.toList());
		Collections.reverse(list);
		assertEquals(list, result);

	}

}
