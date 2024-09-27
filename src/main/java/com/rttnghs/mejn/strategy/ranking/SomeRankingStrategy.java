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
package com.rttnghs.mejn.strategy.ranking;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is an example ranking strategy that leaves the ranking of possible moves to
 * {@link SomeMoveValuator}
 */
public class SomeRankingStrategy extends RankingStrategy {

	private static final Logger logger = LogManager.getLogger(SomeRankingStrategy.class);

	public SomeRankingStrategy(String name, List<Integer> parameters) {
		// Pass a reference to the valuate method, which takes a move and a board state supplier and returns an integer.
		super(new SomeMoveValuator(parameters), name, parameters);
	}

	@Override
	public void finalize(int position) {
		// This particular ranking strategy doesn't care about the history.		
	}
}
