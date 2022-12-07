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

import java.util.Comparator;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Represents a move of a pawn. May be valid or not.
 */
public record Move(Position from, Position to) implements Comparable<Move> {

	/**
	 * Inverse operation of {@link #toString()}
	 * 
	 * @param move string representation of a move.
	 * @return the Move corresponding to the given string representation, or null if
	 *         no such move exists.
	 */
	public static Move of(String move) {
		// Something like "<B1->E1>", so at minimum 8 characters.
		if (move == null || move.length() < 8) {
			return null;
		}
		// Rather than regex matching, we can simply look at the characters.
		if (move.charAt(0) != '<' || move.charAt(move.length() - 1) != '>') {
			return null;
		}
		int arrowIndex = move.indexOf("->");
		// Arrow index must be at at least 3, and no more than 4 from the end to leave
		// space for at least a 2-character from and a 2 character to field.
		// Note that the extra -1 comes from the length starting at 1 and index starting
		// at 0; Arrow index -1 means there is no arrow at all.
		if (arrowIndex < 3 || arrowIndex > (move.length() - 4 - 1)) {
			return null;
		}
		String fromString = move.substring(1, arrowIndex);
		String toString = move.substring(arrowIndex + 2, move.length() - 1);
		Position from = Position.of(fromString);
		if (from == null) {
			return null;
		}
		Position to = Position.of(toString);
		if (to == null) {
			return null;
		}
		return new Move(from, to);
	}

	/**
	 * @param from position
	 * @param to   position
	 * @throws IllegalArgumentException if either argument is null
	 */
	public Move(Position from, Position to) {
		if ((from == null) || (to == null)) {
			throw new IllegalArgumentException("Cannote create a move with a null Position");
		}
		this.from = from;
		this.to = to;
	}

	@Override
	public int compareTo(Move that) {
		return Objects.compare(this, that, Comparator.comparing(Move::from).thenComparing(Move::to));
	}

	@Override
	public String toString() {
		return "<" + from + "->" + to + ">";
	}

	/**
	 * @param spots     how many spots to shift.
	 * @param boardSize used to normalize the Position after shifting.
	 * @return shifted move
	 */
	public Move shift(int spots, int boardSize) {
		return new Move(from.move(spots).normalize(boardSize), to.move(spots).normalize(boardSize));
	}

	/**
	 * Handy in lambdas, for example in stream processing.
	 * 
	 * @param spots
	 * @param boardSize
	 * @return Function that can shift moves.
	 */
	public static UnaryOperator<Move> shifter(int spots, int boardSize) {
		return (move) -> move.shift(spots, boardSize);
	}

}
