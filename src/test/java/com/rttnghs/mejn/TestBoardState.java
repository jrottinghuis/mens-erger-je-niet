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
package com.rttnghs.mejn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestBoardState extends BoardState {

	/**
	 * Not built for robustness. Used only for test cases.
	 * 
	 * @param boardStateString boardStateString.toString formatted board state.
	 * @return new Test Board State
	 */
	protected static BoardState of(String boardStateString) {
		String regex = "^\\((\\d+)\\)\\[(.*)]";

		Pattern pattern = Pattern.compile(regex); // "a{1,}" matches at least one a.
		Matcher matcher = pattern.matcher(boardStateString);
		// For example:
		// (40)[P0={H0,H1,H2,H3};P1={B4,B4,B4,E25};P2={E16,E19,H22,H23};P3={B24,B24,E34,H33}]

		matcher.matches();

		int boardSize = Integer.parseInt(matcher.group(1));
		int pawnsPerPlayer = 0;
		List<String> playerStates = Arrays.asList(matcher.group(2).split(";"));

		ArrayList<List<Position>> newState = new ArrayList<>(playerStates.size());

		Pattern playerStatePattern = Pattern.compile("P(\\d+)=\\{(.*)}");
		Matcher playterStateMatcher;

		for (String playerStateString : playerStates) {
			System.out.println("playerStateString=" + playerStateString);

			List<Position> playerState = new ArrayList<>();
			playterStateMatcher = playerStatePattern.matcher(playerStateString);

			playterStateMatcher.matches();

			int playerIndex = Integer.parseInt(playterStateMatcher.group(1));
			String[] positionStrings = playterStateMatcher.group(2).split(",");
			pawnsPerPlayer = Math.max(pawnsPerPlayer, positionStrings.length);
			for (String positionString : positionStrings) {
				playerState.add(Position.of(positionString));
			}
			newState.add(playerIndex, playerState);
		}

		System.out.println("Size: " + boardSize);
		System.out.println("newState: " + newState);

		return new BoardState(newState, boardSize, pawnsPerPlayer);
	}

	public TestBoardState(int boardSize, int pawnsPerPlayer, List<Position> beginPositions) {
		super(boardSize, pawnsPerPlayer, beginPositions);
		// Not used.
	}

	/**
	 * Convenience method.
	 */
	public static Move getMove(Layer fromLayer, int fromSpot, Layer toLayer, int toSpot) {
		return new Move(new Position(fromLayer, fromSpot), new Position(toLayer, toSpot));
	}

}
