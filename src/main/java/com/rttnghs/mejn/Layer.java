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

/**
 * Dividing a board up in layers helps keeping the normal numbering and avoiding
 * other players being able to strike your pawns in the lower or upper layers.
 * <p>
 * In terms of naming, the lowest (bottom) layer seems to make sense to call
 * BEGIN as that is where the pawns begin from. After rolling 6 (or the number
 * of die faces if that is configured differently) you are allowed to go to the
 * start position. I'm assuming that is marked with "A" on the board due to the
 * Germain word "Anfang" which means start. The last layer is referred to as the
 * HOME layer. Once pawns are HOME they cannot be struck anymore. This leaves
 * the middle, or normal layer. Given that E is in between B and H, I'll choose
 * to call this the EVENT layer. That is the main layer. Initially I was
 * considering calling this the EVENT layer, but O doesn't lexically sort properly
 * between B and H layers. Also, O is easily confused with 0;
 */
public enum Layer {
	BEGIN, EVENT, HOME;

	/**
	 * Indicator of the layer. Starting layer is -1, regular out field layer is 0,
	 * and the home layer is 1.
	 */
	public final int number;

	/**
	 * Constructor for the layer.
	 */
	private Layer() {
		this.number = this.ordinal() - 1;
	}

	public Layer next() {
        return switch (this) {
            case BEGIN -> EVENT;
            case EVENT -> HOME;
            default -> throw new IllegalStateException("No layer after home.");
        };
	}

	/**
	 * @return a one-character representation of this layer.
	 */
	public char toChar() {
		return name().charAt(0);
	}

	/**
	 * Inverse operation of {@link #toChar()}.
	 * 
	 * @param character one character representation of the Layer.
	 * @return the corresponding layer, or null if no such layer exists.
	 */
	public static Layer of(char character) {
		return switch (character) {
		case 'B' -> Layer.BEGIN;
		case 'E' -> Layer.EVENT;
		case 'H' -> Layer.HOME;
		default -> null;
		};
	}
}
