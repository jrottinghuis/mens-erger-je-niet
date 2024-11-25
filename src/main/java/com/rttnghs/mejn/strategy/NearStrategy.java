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

import java.util.List;
import java.util.function.Supplier;

import com.rttnghs.mejn.BoardState;
import com.rttnghs.mejn.Move;

/**
 * Always selects the pawn that is nearest to the start, the least far
 * along.
 * <p>
 * Note that when players are allowed to strike themselves, this is a terrible
 * strategy.
 */
public class NearStrategy extends BaseStrategy implements Strategy {

	public NearStrategy(String name) {
		super(name, null);
	}

	/**
	 * From Interface.
	 */
	@Override
	public Move choose(List<Move> choices, BoardState boardState) {
		return autoChoose(choices, boardState);
	}

	@Override
	public Move multiChoose(List<Move> choices, BoardState boardState) {
		return choices.getFirst();
	}

	@Override
	public void finalize(int position) {
		// Nothing to do here.
	}

}
