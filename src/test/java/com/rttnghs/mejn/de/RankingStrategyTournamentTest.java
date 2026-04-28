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
package com.rttnghs.mejn.de;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RankingStrategyTournamentTest {

    private final RankingStrategyTournament runner = new RankingStrategyTournament();

    // Minimal genome: a single-element parameter list (weight = 1).
    private static List<Integer> genome(int... values) {
        List<Integer> list = new java.util.ArrayList<>();
        for (int v : values) list.add(v);
        return list;
    }

    @Test
    void runBracket_returnsSizeMatchingGenomeCount() {
        List<List<Integer>> bracket = List.of(genome(1), genome(2), genome(3), genome(4));
        List<Double> scores = runner.runBracket(bracket, 10);
        assertEquals(4, scores.size());
    }

    @Test
    void runBracket_allScoresNonNegative() {
        List<List<Integer>> bracket = List.of(genome(1), genome(2), genome(3), genome(4));
        List<Double> scores = runner.runBracket(bracket, 20);
        scores.forEach(s -> assertTrue(s >= 0.0, "Score should be non-negative: " + s));
    }

    @Test
    void runBracket_atLeastOneNonZeroScore() {
        // With 4 players and 20 games someone will have scored.
        List<List<Integer>> bracket = List.of(genome(5), genome(3), genome(7), genome(2));
        List<Double> scores = runner.runBracket(bracket, 20);
        boolean anyNonZero = scores.stream().anyMatch(s -> s > 0.0);
        assertTrue(anyNonZero, "At least one genome should have a non-zero score");
    }

    @Test
    void runBracket_twoBracketPlayers() {
        // Minimum valid bracket: 2 players.
        // How many players can play a MEJN game depends on Game/Tournament internals,
        // but 4 players is the classic setup; this tests a 2-player scenario if supported.
        // If not supported by the game engine this test documents the constraint.
        List<List<Integer>> bracket = List.of(genome(1, 2), genome(3, 4));
        assertDoesNotThrow(() -> runner.runBracket(bracket, 5));
    }

    @Test
    void runBracket_throwsOnEmptyBracket() {
        assertThrows(IllegalArgumentException.class,
                () -> runner.runBracket(List.of(), 10));
    }

    @Test
    void runBracket_throwsOnNullBracket() {
        assertThrows(IllegalArgumentException.class,
                () -> runner.runBracket(null, 10));
    }

    @Test
    void runBracket_throwsOnZeroGames() {
        List<List<Integer>> bracket = List.of(genome(1), genome(2), genome(3), genome(4));
        assertThrows(IllegalArgumentException.class,
                () -> runner.runBracket(bracket, 0));
    }

    @Test
    void runBracket_throwsOnNegativeGames() {
        List<List<Integer>> bracket = List.of(genome(1), genome(2), genome(3), genome(4));
        assertThrows(IllegalArgumentException.class,
                () -> runner.runBracket(bracket, -1));
    }

    @Test
    void runBracket_reflectionContractMethodExists() throws NoSuchMethodException {
        // Verify the structural contract that LocalComputeServer relies on.
        var method = RankingStrategyTournament.class.getMethod("runBracket", List.class, int.class);
        assertEquals(List.class, method.getReturnType());
        assertEquals(2, method.getParameterCount());
    }
}

