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
import java.util.Arrays;
import java.util.List;

/**
 * Used to assign a value to zero, first, second, third, fourth etc finish
 * place. This is used to assign a single integer value for such finishing
 * order. If two players finish first the same number of time, but one of them
 * never finishes last, then assigning a single score will help differentiate
 * between these players.
 * <p>
 * See the paper <a href=
 * "https://www.sciencedirect.com/science/article/pii/S0893965912004478">The
 * medal points’ incenter for rankings in sport</a>
 * 
 */
public class Score {

	private static final List<Integer> SCORES = new ArrayList<>(
			Arrays.asList(1019, 809, 623, 462, 326, 214, 126, 63, 24, 1));

	/**
	 * Get the score for the given position and the specified number of players.
	 * 
	 * @param finishOrderIndex 0-based index of finish order. The first to finish
	 *                         has finishOrderIndex 0. This cannot be a negative
	 *                         number.
	 * @param playerCount          how many players participated in a game.
	 * @return score. Only the first min(X, #players) finishers will get any score,
	 *         beyond that, all scores are 0;
	 */
	public static int get(int finishOrderIndex, int playerCount) {
		if (finishOrderIndex < 0) {
			return 0;
		}

		// Make sure we don't count back more than the size of SCORES
		int offset = (playerCount <= SCORES.size()) ? playerCount : SCORES.size();
		// Count offset back from the end of SCORES, then add the finishOrder.
		int scoreIndex = SCORES.size() - offset + finishOrderIndex;
		return (scoreIndex >= 0 && scoreIndex < SCORES.size()) ? SCORES.get(scoreIndex) : 0;
	}

	/**
	 * @param players number of players in the game
	 * @param rounds  number of rounds the game is played
	 * @return exp
	 */
	public static int winningScore(int players) {
		if ((players < 1)) {
			return 0;
		}
		int scoreIndex = SCORES.size() - players;
		if (scoreIndex < 0) {
			scoreIndex = 0;
		}
		return SCORES.get(scoreIndex);
	}

}