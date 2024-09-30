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
import java.util.Comparator;
import java.util.List;

import com.rttnghs.mejn.Die;

/**
 * Used to keep track of a population of individuals.
 */
public class Population {

	// TODO: make this a separate de configuration.
	// TODO: round up to the next power of two.
	// Should be a power of two
	private final int populationSize = 64;
	private final List<Individual> population = new ArrayList<>(populationSize);

	private final int halfPopulationSize = populationSize / 2;

	/**
	 * Number of competitors that each individual should compete against.
	 */
	// TODO: Make configurable, and ensure this is <= populationSize.
	private final int competitionSize = 8;

	// TODO: Javadoc and make configurable
	private final int scalingFactorPercent = 50;
	private final int crossOverRatePercent = 50;

	/**
	 * Pick a random population number; Subtract 1 for a random population index.
	 */
	private final Die populationDie = new Die(populationSize);

	/**
	 * @param dimension the dimension of {@link Individual}s in this population.
	 */
	public Population(int dimension) {
		// initialize population with random individuals
		for (int i = 0; i < populationSize; i++) {
			population.add(new Individual(dimension));
		}
	}

	/**
	 * @param generations number of generations to run
	 * @return the vector of the fittest individual
	 */
	public List<Integer> train(int generations) {
		// Do the thing.
		// TODO: Test to confirm nothing happens if generations <1
		for (int generation = 0; generation < generations; generation++) {
			compete();
			// Sort population by fitness, descending order
			population.sort(Comparator.comparing(Individual::getFitness, Comparator.reverseOrder()));
			mutate();

		}
		return population.getFirst().getVector();
	}

	private void compete() {
		// TODO create a lambda to pick a random.
		// For each individual, pick random competitors and have them compete.
		for (int i = 0; i < populationSize; i++) {
			// Exclude all selected random individuals to compete against selected so far,
			// plus current individual.
			Individual[] excluded = new Individual[competitionSize + 1];
			excluded[0] = population.get(i);
			int cumulativeScore = 0;
			for (int c = 0; c < competitionSize; c++) {
				// Select a random individual
				Individual opponent = selectRandomIndividual(excluded);
				cumulativeScore += getScore(population.get(i), opponent);
				excluded[c + 1] = opponent;
			}
			// Set the fitness to be the average score against the randomly chosen opponents
			population.get(i).setFitness(cumulativeScore / competitionSize);
		}
	}

	/**
	 * Get the score of an individual against an opponent.
	 * 
	 * @param individual for whom we need to get the score
	 * @param opponent   against whom the individual should compete
	 * @return how much an individual scored against the given opponent
	 */
	private int getScore(Individual individual, Individual opponent) {
		Integer score = individual.getScore(opponent);
		if (score == null) {
			// TODO: Actually have the individuals compete.
			score = 1;
			int score2 = 2;
			individual.setScore(opponent, score);
			opponent.setScore(individual, score2);
		}
		return score;
	}

	private void mutate() {
		Individual bestIndividual = population.getFirst();
		// Walk through the first half of the population, find
		for (int i = 0; i < halfPopulationSize; i++) {
			Individual random1 = selectRandomIndividual(population.get(i), bestIndividual);
			Individual random2 = selectRandomIndividual(population.get(i), bestIndividual, random1);
			Individual mutatedIndividual = population.get(i).mutateCurrentToBest(bestIndividual, random1, random2,
					scalingFactorPercent, crossOverRatePercent);

			Individual bottomHalfIndividual = population.get(i + halfPopulationSize);

			// TODO: Make the mutated and the bottom half individuals compete
			// if mutatedIndividualScore > bottomHalfIndividualScore {
			//
			// population.set(i + halfPopulationSize, mutatedIndividual);
			// }
			// TODO: In documentation, describe that recently mutated individuals that
			// competed only against one bottom half individual, which has now been replaced
			// could be selected for further mutation further driving exploration.

		}
	}

	/**
	 * @return a random individual from the population, excluding all of the listed
	 *         individuals
	 */
	private Individual selectRandomIndividual(Individual... excluded) {

		Individual selected = null;
		while (selected == null) {
			selected = population.get(populationDie.roll() - 1);
			// Check if the selected individual is in the excluded list
            for (Individual individual : excluded) {
                if (selected == individual) {
                    // Discard this individual
                    selected = null;
                    break; // for loop
                }
            }
		}
		return selected;
	}

}
