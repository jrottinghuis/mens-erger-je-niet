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

import java.util.Random;

/**
 *
 */
public class Die {

	/**
	 * How many faces this die has.
	 */
	public final int faces;
	private static final Random number = new Random();

	/**
	 * Die will roll between 1 and faces (including).
	 * 
	 * @param faces upper limit of what this die can roll.
	 * @throws IllegalArgumentException when faces is < 1.
	 */
	public Die(int faces) {
		if (faces < 1) {
			throw new IllegalArgumentException("Cannot have a die with <1 faces");
		}
		this.faces = faces;
	}

	/**
	 * Roll the die. Returns a random value between 1 and faces.
	 */
	public int roll() {
		return number.nextInt(faces) + 1;
	}

}
