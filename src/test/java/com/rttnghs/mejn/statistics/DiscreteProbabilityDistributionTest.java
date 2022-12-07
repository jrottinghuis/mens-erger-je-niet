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
package com.rttnghs.mejn.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DiscreteProbabilityDistributionTest {

	class TestDiscreteProbabilityDistribution extends DiscreteProbabilityDistribution<String> {
		/**
		 * @param fixed always picks this fixed number.
		 */
		TestDiscreteProbabilityDistribution(int fixed) {
			super(() -> fixed);
		}
	}

	@Test
	final void testRegular() {
		DiscreteProbabilityDistribution<String> distribution = new DiscreteProbabilityDistribution<>();
		// Selecting without adding item should always return null;
		assertEquals(null, distribution.select());
		assertEquals(null, distribution.select());

		distribution.add("A", 1);
		assertEquals("A", distribution.select());
		assertEquals("A", distribution.select());
		assertEquals("A", distribution.select());
		assertEquals(1, distribution.frequencySum());

		distribution.add("B", 2);
		assertEquals(3, distribution.frequencySum());
		distribution.add("C", 3);
		assertEquals(6, distribution.frequencySum());
		distribution.add("D", 4);
		assertEquals(10, distribution.frequencySum());

		Map<String, Integer> counts = new HashMap<>();
		for (int i = 0; i < 10000000; i++) {
			counts.merge(distribution.select(), 1, Integer::sum);
			// System.out.println(distribution.select());
		}
		// System.out.println(counts);

		distribution = new DiscreteProbabilityDistribution<>();
		distribution.add("A", 1);
		distribution.add("C", 3);
		distribution.add("B", 2);
		distribution.add("D", 4);

		counts = new HashMap<>();
		for (int i = 0; i < 10000000; i++) {
			counts.merge(distribution.select(), 1, Integer::sum);
			// System.out.println(distribution.select());
		}
		// System.out.println(counts);

	}

	@Test
	final void testDiscreteProbabilityDistribution() {
		// Distribution that always rolls 1 should always return the first item added.
		DiscreteProbabilityDistribution<String> distribution1 = new TestDiscreteProbabilityDistribution(1);
		assertEquals(null, distribution1.select());
		assertEquals(null, distribution1.select());

		distribution1.add("A", 1);
		assertEquals("A", distribution1.select());
		assertEquals("A", distribution1.select());
		assertEquals("A", distribution1.select());
		distribution1.add("B", 2);
		assertEquals("A", distribution1.select());
		assertEquals("A", distribution1.select());
		distribution1.add("C", 3);
		distribution1.add("D", 4);
		assertEquals("A", distribution1.select());

		// Distribution that always rolls 2 should always return the first item added.
		DiscreteProbabilityDistribution<String> distribution2 = new TestDiscreteProbabilityDistribution(2);
		assertEquals(null, distribution2.select());
		assertEquals(null, distribution2.select());

		distribution2.add("A", 1);
		assertEquals("A", distribution2.select());
		assertEquals("A", distribution2.select());
		assertEquals("A", distribution2.select());
		distribution2.add("B", 2);
		assertEquals("B", distribution2.select());
		assertEquals("B", distribution2.select());
		distribution2.add("C", 3);
		distribution2.add("D", 4);
		// Frequency index is [0, 1, 3, 6]
		assertEquals("B", distribution2.select());
		assertEquals("B", distribution2.select());

		// Distribution that always rolls 2 should always return the first item added.
		DiscreteProbabilityDistribution<String> distribution3 = new TestDiscreteProbabilityDistribution(3);
		assertEquals(null, distribution3.select());
		assertEquals(null, distribution3.select());

		distribution3.add("A", 1);
		assertEquals("A", distribution3.select());
		assertEquals("A", distribution3.select());
		assertEquals("A", distribution3.select());
		distribution3.add("B", 2);
		assertEquals("B", distribution3.select());
		assertEquals("B", distribution3.select());
		distribution3.add("C", 3);
		distribution3.add("D", 4);
		assertEquals(10, distribution3.frequencySum());

		// Frequency index is [0, 1, 3, 6]
		assertEquals("B", distribution3.select());
		assertEquals("B", distribution3.select());

		// Distribution that always rolls 2 should always return the first item added.
		DiscreteProbabilityDistribution<String> distribution4 = new TestDiscreteProbabilityDistribution(4);
		distribution4.add("A", 1);
		distribution4.add("B", 2);
		distribution4.add("C", 3);
		distribution4.add("D", 4);
		// Frequency index is [0, 1, 3, 6]
		assertEquals("C", distribution4.select());

		// Distribution that always rolls 2 should always return the first item added.
		DiscreteProbabilityDistribution<String> distribution7 = new TestDiscreteProbabilityDistribution(7);
		distribution7.add("A", 1);
		distribution7.add("B", 2);
		distribution7.add("C", 3);
		distribution7.add("D", 4);
		// Frequency index is [0, 1, 3, 6]
		assertEquals("D", distribution7.select());

		// Adding out of order should work as well.
		distribution7 = new TestDiscreteProbabilityDistribution(7);
		distribution7.add("B", 2);
		distribution7.add("A", 1);
		distribution7.add("D", 4);
		distribution7.add("C", 3);
		// Frequency index is [0, 2, 3, 7]
		assertEquals("D", distribution7.select());
		assertEquals(10, distribution7.frequencySum());

	}

}
