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
package com.rttnghs.mejn.ml.de;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;


class IndividualTest {

	/**
	 * Test method for {@link com.rttnghs.mejn.ml.de.Individual#Individual(int)}.
	 */
	@Test
	final void testIndividual() {
		Individual individual1 = new Individual(10);
		List<Integer> vector = individual1.getVector();
		assertEquals(10, vector.size());
	}

	/**
	 * Test method for {@link com.rttnghs.mejn.ml.de.Individual#getFitness()}.
	 */
	@Test
	final void testFitness() {
		Individual individual1 = new Individual(10);
	}

	/**
	 * Test method for {@link com.rttnghs.mejn.ml.de.Individual#setFitness(int)}.
	 */
	@Test
	final void testSetFitness() {
		// TODO
	}

	/**
	 * Test method for {@link com.rttnghs.mejn.ml.de.Individual#getGeneration()}.
	 */
	@Test
	final void testGetGeneration() {
		// TODO
	}

	/**
	 * Test method for {@link com.rttnghs.mejn.ml.de.Individual#getName()}.
	 */
	@Test
	final void testGetName() {
		// TODO
	}

	/**
	 * Test method for {@link com.rttnghs.mejn.ml.de.Individual#getScore(com.rttnghs.mejn.ml.de.Individual)}.
	 */
	@Test
	final void testGetScore() {
		// TODO
	}

	/**
	 * Test method for {@link com.rttnghs.mejn.ml.de.Individual#setScore(com.rttnghs.mejn.ml.de.Individual, int)}.
	 */
	@Test
	final void testSetScore() {
		// TODO
	}

	/**
	 * Test method for {@link com.rttnghs.mejn.ml.de.Individual#mutateCurrentToBest(com.rttnghs.mejn.ml.de.Individual, com.rttnghs.mejn.ml.de.Individual, com.rttnghs.mejn.ml.de.Individual, int, int)}.
	 */
	@Test
	final void testMutateCurrentToBest() {
		// TODO
	}

	/**
	 * Test method for {@link com.rttnghs.mejn.ml.de.Individual#getVector()}.
	 */
	@Test
	final void testGetVector() {
		// TODO
	}

	/**
	 * Test method for {@link com.rttnghs.mejn.ml.de.Individual#toString()}.
	 */
	@Test
	final void testToString() {
		// TODO
	}

}
