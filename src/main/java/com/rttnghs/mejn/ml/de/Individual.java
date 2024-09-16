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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import com.rttnghs.mejn.Die;
import com.rttnghs.mejn.configuration.Config;

/**
 * Individual represents a candidate solution in Differential Evolution is
 * characterized by a vector (list) of <i>D</i> variables with a max limit of
 * these values, and possibly parameters for the crossover rate <i>Cr</i> and
 * mutation scaling factor <i>F</i>.
 */
public class Individual {

	// TODO: read this from a config
	public static final int maxValue = Config.configuration.getInt("individualVariableMax");
	private final ArrayList<Integer> vector;

	/**
	 * String representing the list of values in this individual, therefore uniquely
	 * identify this individual
	 */
	private final String name;

	// Determines the fitness score of this individual, used to rank individuals in
	// the population
	private int fitness = 0;

	// How many generations this individual has survived.
	private int generation = 0;

	private final Map<Individual, Integer> scoreCache = new WeakHashMap<>();

	/**
	 * Create a new individual with given dimension
	 * 
	 * @param dimension number of variables in the vector for this individual. Must
	 *                  be larger than 1.
	 */
	public Individual(int dimension) {
		if (dimension < 1) {
			throw new IllegalArgumentException("Dimension: " + dimension + " must be > 1");
		}
		vector = new ArrayList<>(dimension);
		// Die is 1-based, we need a zero based random number
		Die die = new Die(maxValue * 2);
		for (int i = 0; i < dimension; i++) {
			// Shift the random number back to [-maxValue, maxValue)
			int randomInRange = (die.roll() - (maxValue + 1));
			vector.add(i, randomInRange);
		}
		name = vector.stream().toList().toString();
	}

	/**
	 * Create new individual with the given vector values.
	 * 
	 * @param vector non-null vector containing the variables that this individual
	 *               should be created with
	 */
	private Individual(List<Integer> vector) {
		this.vector = new ArrayList<>(vector);
		name = vector.stream().toList().toString();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the fitness
	 */
	public int getFitness() {
		return fitness;
	}

	/**
	 * @param fitness the fitness to set. Setting a fitness will increase the
	 *                generation count.
	 */
	public void setFitness(int fitness) {
		this.fitness = fitness;
		generation++;
	}

	/**
	 * @return how many generations this individual survived, determined by number
	 *         of calls to {@link #setFitness(int)}
	 */
	public int getGeneration() {
		return generation;
	}

	/**
	 * Return the previous score against the provided opponent.
	 *
	 * @param opponent against which this individual may have previously competed.
	 * @return score How much this individual scored against provided opponent, or
	 *         null if they previous score is not found.
	 */
	public Integer getScore(Individual opponent) {
		return scoreCache.get(opponent);
	}

	/**
	 * @param opponent against whom this individual competed.
	 * @param score    how much this individual scored against the provided
	 *                 opponent.
	 */
	public void setScore(Individual opponent, int score) {
		scoreCache.put(opponent, score);
	}

	/**
	 * @param best                 non-null best individual in the population, must
	 *                             have same dimension as this individual.
	 * @param random1              random individual from population, must have same
	 *                             dimension as this individual.
	 * @param random2              random individual from population, must have same
	 *                             dimension as this individual.
	 * @param scalingFactorPercent number between 0-100 to scale the vectors
	 * @param crossOverRatePercent percentage indicating crossover rate used as
	 *                             variable for each Bernoulli experiment to
	 *                             determine crossover if variable is from parent or
	 *                             from mutation vector
	 * @return new individual
	 *         <p>
	 *         The constraint handling for this new individual is pretty simple. The
	 *         values are cut off at the lower and upper bound, 0 and
	 *         {@link #maxValue}
	 */
	public Individual mutateCurrentToBest(Individual best, Individual random1, Individual random2,
			int scalingFactorPercent, int crossOverRatePercent) {

		if (this.vector.size() != best.vector.size()) {
			throw new IllegalArgumentException(
					"best individual dimension " + best.vector.size() + " does not match " + vector.size());
		}
		if (this.vector.size() != random1.vector.size()) {
			throw new IllegalArgumentException(
					"randmon1 individual dimension " + random1.vector.size() + " does not match " + vector.size());
		}
		if (this.vector.size() != random2.vector.size()) {
			throw new IllegalArgumentException(
					"random2 individual dimension " + random1.vector.size() + " does not match " + vector.size());
		}

		ArrayList<Integer> mutation = new ArrayList<>(vector.size());
		ArrayList<Integer> trial = new ArrayList<>(vector.size());
		Die crossoverRandomizer = new Die(100);

		// Generate mutation vector
		for (int i = 0; i < vector.size(); i++) {
			mutation.add(i, vector.get(i) + (scalingFactorPercent * (best.vector.get(i)) - vector.get(i)) / 100
					+ (scalingFactorPercent * (random1.vector.get(i)) - random2.vector.get(i)));
		}

		// Do crossover
		for (int i = 0; i < vector.size(); i++) {
			if (crossoverRandomizer.roll() <= crossOverRatePercent) {
				trial.add(i, mutation.get(i));
			} else {
				trial.add(i, vector.get(i));
			}
		}

		// Pick at least one mutation, to ensure a different individual if best !=
		// random1, != random 2
		int randomIndex = new Die(mutation.size()).roll() - 1;
		trial.add(randomIndex, mutation.get(randomIndex));

		return new Individual(mutation);
	}

	/**
	 * @return unmodifiable vector of variables characterizing this individual
	 */
	public List<Integer> getVector() {
		return Collections.unmodifiableList(vector);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Individual other)) {
			return false;
		}
        return Objects.equals(name, other.name);
	}
}
