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
package com.rttnghs.mejn.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.rttnghs.mejn.BoardState;
import com.rttnghs.mejn.History;
import com.rttnghs.mejn.Move;

/**
 * Helps strategies implement some common behavior, such as dealing with empty,
 * or single option choices. Helps provide a mechanism to hang on to parameters
 */
public abstract class BaseStrategy implements Strategy {

	private final String name;
	protected Supplier<History<Move>> historySupplier = null;
	/**
	 * Parameters to be used for the strategy. Could be empty if so configured in
	 * the config file.
	 */
	protected final List<Integer> parameters = new ArrayList<>();

	public BaseStrategy(String name, Collection<Integer> parameters) {
		this.name = name;
		if (parameters != null) {
			this.parameters.addAll(parameters);
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Strategy initialize(Supplier<History<Move>> historySupplier) {
		this.historySupplier = historySupplier;
		return this;
	}

	/**
	 * Strategies that extend this class can opt to defer the
	 * {@link #choose(List, Supplier)} method to this method. It will automatically
	 * deal with a no-choice or single move choice situation, in other words, when
	 * there really isn't anything to choose. In cases when there is something to
	 * choose, this method will defer to the implementor's
	 * {@link #multiChoose(List, Supplier) method.
	 * <p>
	 * @param choices
	 * @return null for no choice, return the one choice in that case, or
	 *         multiChoose() if there are more choices.
	 */
	protected Move autoChoose(List<Move> choices, Supplier<BoardState> stateSupplier) {
		if (choices == null) {
			// This should not happen, we should not get called with null choice;
			throw new IllegalArgumentException();
		}
		if (choices.isEmpty()) {
			return null;
		}
		if (choices.size() == 1) {
			return choices.getFirst();
		}
		return multiChoose(choices, stateSupplier);
	}

	/**
	 * Called iff there is more than one choice to make and the implementing class
	 * defers the {@link #choose(List, Supplier)} method to the
	 * {@link #autoChoose(List, Supplier) method.
	 * @param choices       Two or more choices to choose from.
	 * @param stateSupplier to get to the state of the board.
	 * @return
	 */
	public abstract Move multiChoose(List<Move> choices, Supplier<BoardState> stateSupplier);

}
