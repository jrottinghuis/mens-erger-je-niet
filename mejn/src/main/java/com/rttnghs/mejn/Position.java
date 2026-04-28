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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Location on the board, determined by layer and the spot (number indicating
 * which spot).
 */
public record Position(Layer layer, int spot) implements Comparable<Position> {

	private static final Logger logger = LogManager.getLogger(Position.class);

	/**
	 * @param position obtained from {@link #toString()} method.
	 * @return a position of the given string representation or null if no such
	 *         position exists.
	 */
	public static Position of(String position) {
		if (position == null || position.length() < 2) {
			return null;
		}
		Layer layer = Layer.of(position.charAt(0));
		if (layer == null) {
			return null;
		}
		Integer spot = null;
		try {
			spot = Integer.valueOf(position.substring(1));
		} catch (NumberFormatException nfe) {
            logger.debug("Not a valid number in {}", position);
			return null;
		}
		return new Position(layer, spot);
	}

	/**
	 * Normalize the position given the modulo where the board wraps.
	 * 
	 * @param boardSize the number of positions on the board until the board wraps.
	 * @return new position (or the same one) with layer the same and position
	 *         modulo boardSize. Except when position is 0, then layer is always
	 *         EVENT.
	 *         <p>
	 *         Note: This always returns a positive spot position. Also, the layer
	 *         remains unchanged.
	 *         </p>
	 */
	public Position normalize(int boardSize) {
		int normalSpot = spot % boardSize;
		// % is a remainder calculation, not a full modulo arithmetic.
		// So it is possible, normalSpot is now between -boardSize and 0.
		// Let's make it positive, for a positive result.
		// The absolute value is due to the possibility of boardSize being negative.
		if (normalSpot < 0) {
			normalSpot = (normalSpot + Math.abs(boardSize)) % boardSize;
		}
		// If normalized spot is the same as the original position's spot, return
		// original position.
		return (spot == normalSpot) ? this : new Position(layer, normalSpot);
	}

	/**
	 * @param spots indicating the number of spots to move
	 * @return position with spots moved. Layer is not changed.
	 *         <p>
	 *         Note, result is not normalized.
	 */
	public Position move(int spots) {
		return (spots == 0) ? this : new Position(layer, spot + spots);
	}

	/**
	 * Compares the position first on layer, then on spot.
	 */
	@Override
	public int compareTo(Position that) {
		return Objects.compare(this, that, Comparator.comparing(Position::layer).thenComparing(Position::spot));
	}

	/**
	 * @param min the minimum position
	 * @param max the maximum position
	 * @return if min < this <= max, or null if any argument is null.
	 */
	public boolean isBetween(Position min, Position max) {
		if ((min == null) || (max == null)) {
			return false;
		}
		if (this.compareTo(min) <= 0) {
			return false;
		}
        return this.compareTo(max) <= 0;
    }

	@Override
	public String toString() {
		return layer.toChar() + String.valueOf(spot);
	}

	/**
	 * Should be called only for layers before the home layer.
	 * 
	 * @return the position of the same spot in the next layer.
	 */
	public Position nextLayer() {
		return new Position(layer.next(), spot);
	}

}
