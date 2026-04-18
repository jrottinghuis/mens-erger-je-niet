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

import com.rttnghs.mejn.configuration.Config;
import com.rttnghs.mejn.internal.BaseBoardState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.rttnghs.mejn.Layer.BEGIN;
import static com.rttnghs.mejn.Layer.EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleEvaluatorTest {

    @Test
    void testEvaluateReturnsFromStartWhenFromStartAndToStartBothExist() {
        int dotsPerPlayer = Config.value.dotsPerPlayer();
        int dieFaces = Config.value.dieFaces();
        int boardSize = 2 * dotsPerPlayer;

        Position playerZeroBegin = new Position(BEGIN, -dieFaces).normalize(boardSize);
        Position playerOneBegin = new Position(BEGIN, -dieFaces + dotsPerPlayer).normalize(boardSize);
        List<Position> beginPositions = new ArrayList<>(List.of(playerZeroBegin, playerOneBegin));

        BaseBoardState state = new BaseBoardState(boardSize, dotsPerPlayer, 2, beginPositions);
        Position start = new Position(EVENT, 0);
        state.move(new Move(playerZeroBegin, start));

        RuleEvaluator evaluator = new RuleEvaluator(state, start);
        Move toStart = new Move(playerZeroBegin, start);
        Move fromStart = new Move(start, new Position(EVENT, dieFaces));

        // Put to-start first to prove from-start still has precedence.
        List<Move> moves = Arrays.asList(toStart, fromStart);
        List<Move> allowed = evaluator.evaluate(moves, dieFaces);

        assertEquals(List.of(fromStart), allowed);
    }

    @Test
    void testEvaluateReturnsToStartWhenPlayerNotOnStart() {
        int dotsPerPlayer = Config.value.dotsPerPlayer();
        int dieFaces = Config.value.dieFaces();
        int boardSize = 2 * dotsPerPlayer;

        Position playerZeroBegin = new Position(BEGIN, -dieFaces).normalize(boardSize);
        Position playerOneBegin = new Position(BEGIN, -dieFaces + dotsPerPlayer).normalize(boardSize);
        List<Position> beginPositions = new ArrayList<>(List.of(playerZeroBegin, playerOneBegin));

        BaseBoardState state = new BaseBoardState(boardSize, dotsPerPlayer, 1, beginPositions);
        Position start = new Position(EVENT, 0);

        RuleEvaluator evaluator = new RuleEvaluator(state, start);
        Move toStart = new Move(playerZeroBegin, start);
        Move otherLegalMove = new Move(playerZeroBegin, new Position(EVENT, 1));

        List<Move> allowed = evaluator.evaluate(Arrays.asList(toStart, otherLegalMove), dieFaces);

        assertEquals(List.of(toStart), allowed);
    }

    @Test
    void testEvaluateForcedSingleMoveReturnsImmutableList() {
        int dotsPerPlayer = Config.value.dotsPerPlayer();
        int dieFaces = Config.value.dieFaces();
        int boardSize = 2 * dotsPerPlayer;

        Position playerZeroBegin = new Position(BEGIN, -dieFaces).normalize(boardSize);
        Position playerOneBegin = new Position(BEGIN, -dieFaces + dotsPerPlayer).normalize(boardSize);
        List<Position> beginPositions = new ArrayList<>(List.of(playerZeroBegin, playerOneBegin));

        BaseBoardState state = new BaseBoardState(boardSize, dotsPerPlayer, 1, beginPositions);
        Position start = new Position(EVENT, 0);
        RuleEvaluator evaluator = new RuleEvaluator(state, start);
        Move toStart = new Move(playerZeroBegin, start);
        Move otherLegalMove = new Move(playerZeroBegin, new Position(EVENT, 1));

        List<Move> allowed = evaluator.evaluate(Arrays.asList(toStart, otherLegalMove), dieFaces);

        assertEquals(List.of(toStart), allowed);
        assertThrows(UnsupportedOperationException.class, () -> allowed.remove(0));
    }
}


