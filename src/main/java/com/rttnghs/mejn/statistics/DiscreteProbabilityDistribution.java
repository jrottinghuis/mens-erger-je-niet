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

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

import com.rttnghs.mejn.Die;

/**
 * Used to have a collection of events that can be selected with a given
 * discrete probability.
 * 
 * @param <E> the type of elements that need to be added to and selected from in
 *            this distribution.
 */
public class DiscreteProbabilityDistribution<E> {

	private int frequencySum = 0;
	private Die die = new Die(1);

	private final List<E> elements = new ArrayList<>();
	private final List<Integer> frequencyIndex = new ArrayList<>();

	/**
	 * Returning a random int between 1 and {@link #frequencySum}
	 */
	private IntSupplier randomIntSupplier;
	private IntSupplier externalRandomIntSupplier = null;

	/**
	 * Default constructor
	 */
	public DiscreteProbabilityDistribution() {
		randomIntSupplier = die::roll;
	}

	/**
	 * Mainly for testing purposes to change the selection. Don't use unless you
	 * know what you're doing.
	 * 
	 * @param randomIntSupplier returning a random int between 1 and
	 *                          {@link #frequencySum}
	 */
	protected DiscreteProbabilityDistribution(IntSupplier randomIntSupplier) {
		this.externalRandomIntSupplier = randomIntSupplier;
	}

	/**
	 * @param element   to be selected with
	 * @param frequency equal to this element's frequency / sum of all
	 *                  probabilities.
	 */
	public void add(E element, int frequency) {
		frequencyIndex.add(frequencySum);
		elements.add(element);
		frequencySum += frequency;
		die = new Die(frequencySum);
		randomIntSupplier = die::roll;
	}

	/**
	 * @return what the sum of all frequencies is that we have seen so far.
	 */
	int frequencySum() {
		return this.frequencySum;
	}

	/**
	 * @return one of the elements added to this frequency with a chance of
	 *         frequency/sum(frequencies of all added items).
	 *         <p>
	 *         Will return null if nothing has been added to this frequency
	 */
	public E select() {
		if (frequencyIndex.size() == 0) {
			return null;
		}
		int pick = (externalRandomIntSupplier == null) ? randomIntSupplier.getAsInt()
				: externalRandomIntSupplier.getAsInt();
		for (int j = frequencyIndex.size() - 1; j >= 0; j--) {
			if (pick > frequencyIndex.get(j)) {
				return elements.get(j);
			}
		}
		// This shall not happen
		return null;
	}

}
