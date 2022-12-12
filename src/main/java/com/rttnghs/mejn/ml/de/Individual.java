package com.rttnghs.mejn.ml.de;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.rttnghs.mejn.Die;

/**
 * Individual represents a candidate solution in Differential Evolution is
 * characterized by a vector (list) of <i>D</i> variables with a max limit of
 * these values, and possibly parameters for the crossover rate <i>Cr</i> and
 * mutation scaling factor <i>F</i>.
 * <p>
 * The constraing handling for this individual is pretty simple. The values are
 * cut off at the lower and uppper bound, 0 and {@link #maxValue}
 */
public class Individual {

	// TODO: read this from a config
	// fileConfig.configuration.getInt("individualVariableMax");
	public static int maxValue = 199;
	private final ArrayList<Integer> vector;

	/**
	 * String representing the list of values in this individual, therefore uniquely
	 * identify this individual
	 */
	private final String name;

	/**
	 * Create a new individual with given dimension
	 * 
	 * @param dimension number of variables in the vector for this individual.
	 */
	public Individual(int dimension) {
		vector = new ArrayList<>(dimension);
		// Die is 1-based, we need a zero based random number
		Die die = new Die(maxValue + 1);
		for (int i = 0; i < vector.size(); i++) {
			vector.add(i, die.roll() - 1);
		}
		name = vector.stream().collect(Collectors.toList()).toString();
	}

	/**
	 * Create new individual with the given vector values.
	 * 
	 * @param vector non-null vector containing the variables that this individual
	 *               should be created with
	 */
	public Individual(ArrayList<Integer> vector) {
		this.vector = new ArrayList<>(vector);
		name = vector.stream().collect(Collectors.toList()).toString();
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
	 *                             variable for each bernoulli experiment to
	 *                             determine crossover if variable is from parent or
	 *                             from mutation vectore
	 * @return new individual
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
		
		// Pick at least one mutation, to ensure a different individual if best != random1, != random 2
		int randomIndex = new Die(mutation.size()).roll() -1;
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
		if (!(obj instanceof Individual)) {
			return false;
		}
		Individual other = (Individual) obj;
		return Objects.equals(name, other.name);
	}

	// TODO: remove this temporary code.
	public static void main(String[] args) {
		ArrayList<Integer> mutation = new ArrayList<>(3);
		mutation.add(0);
		mutation.add(0);
		mutation.add(0);
		mutation.add(99);
		mutation.add(0);
		mutation.add(0);
		mutation.add(0);
		mutation.add(99);
		mutation.add(-109);
		mutation.add(2);
		Individual ind = new Individual(mutation);
		System.out.println("hashCode=" + ind.hashCode());
		System.out.println("hashCode=" + Objects.hash(ind.getVector()));
		System.out.println("Mutation=" + ind.toString());

	}

}
