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
package com.rttnghs.mejn.internal;

import com.rttnghs.mejn.BoardState;
import com.rttnghs.mejn.Move;
import com.rttnghs.mejn.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.rttnghs.mejn.Layer.*;
import static com.rttnghs.mejn.internal.TestBoardState.getMove;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
class BaseBoardStateTest {

    private static final Logger logger = LogManager.getLogger(BaseBoardStateTest.class);

    /**
     * Test method for
     * {@link com.rttnghs.mejn.BoardState constructor}
     */
    @Test
    final void testBoardState() {

        assertThrows(IllegalStateException.class, () -> new BaseBoardState(40, 10, 4, null));

        List<Position> beginPositionsFour = new ArrayList<>(4);
        assertThrows(IllegalStateException.class, () -> new BaseBoardState(40, 10, 4, beginPositionsFour));

        beginPositionsFour.add(new Position(BEGIN, 34));
        beginPositionsFour.add(new Position(BEGIN, 4));
        beginPositionsFour.add(new Position(BEGIN, 14));
        beginPositionsFour.add(new Position(BEGIN, 24));
        BaseBoardState boardState = new BaseBoardState(40, 10, 4, beginPositionsFour);

        BaseBoardState boardStateTwo = new BaseBoardState(40, 10, 2, beginPositionsFour);
        BaseBoardState boardStateFour = new BaseBoardState(40, 10, 4, beginPositionsFour);

        assertThrows(IllegalStateException.class, () -> new BaseBoardState(40, 10, 0, beginPositionsFour));
        assertThrows(IllegalStateException.class, () -> new BaseBoardState(40, 10, -6, beginPositionsFour));

        List<Position> beginPositionsSix = new ArrayList<>(6);
        beginPositionsSix.add(new Position(BEGIN, 42));
        beginPositionsSix.add(new Position(BEGIN, 2));
        beginPositionsSix.add(new Position(BEGIN, 10));
        beginPositionsSix.add(new Position(BEGIN, 18));
        beginPositionsSix.add(new Position(BEGIN, 26));
        beginPositionsSix.add(new Position(BEGIN, 34));
        BaseBoardState boardStateSix = new BaseBoardState(48, 10, 6, beginPositionsSix);

        // Test the equality
        assertEquals(boardState, boardStateFour);

        assertNotEquals(boardState, boardStateTwo);
        assertNotEquals(boardState, boardStateSix);
        assertNotEquals(boardStateFour, boardStateSix);

        assertNotEquals(boardStateFour, boardStateSix);

        // Confirm that the string representations are same for equal and different for
        // not equal.
        assertEquals(boardState.toString(), boardState.toString());
        assertEquals(boardState.toString(), boardStateFour.toString());
        assertEquals(boardStateSix.toString(), boardStateSix.toString());

        assertNotEquals(boardState.toString(), boardStateTwo.toString());
        assertNotEquals(boardState, boardStateSix);
        assertNotEquals(boardStateFour.toString(), boardStateSix.toString());

        assertNotEquals(boardStateFour.toString(), boardStateSix.toString());

        // For debugging purposes
        logger.trace(boardStateSix);
    }

    @Test
    final void testGetters() {
        List<Position> beginPositionsFour = new ArrayList<>(4);
        beginPositionsFour.add(new Position(BEGIN, 34));
        beginPositionsFour.add(new Position(BEGIN, 4));
        BaseBoardState boardStateTwo = new BaseBoardState(40, 10, 2, beginPositionsFour);

        beginPositionsFour.add(new Position(BEGIN, 14));
        beginPositionsFour.add(new Position(BEGIN, 24));
        BaseBoardState boardState = new BaseBoardState(40, 10, 4, beginPositionsFour);

        assertEquals(4, boardState.getPawnsPerPlayer());
        assertEquals(4, boardState.getPlayerCount());
        assertEquals(40, boardState.getBoardSize());

        // Test that if we mess with the original input list, that doesn't affect the
        // previously created state. Ignote the incorrect begin position for this test.
        beginPositionsFour.add(new Position(BEGIN, 44));
        BaseBoardState boardStateFive = new BaseBoardState(40, 10, 7, beginPositionsFour);
        assertEquals(4, boardState.getPawnsPerPlayer());
        assertEquals(4, boardState.getPlayerCount());
        assertEquals(7, boardStateFive.getPawnsPerPlayer());
        assertEquals(5, boardStateFive.getPlayerCount());

        assertEquals(2, boardStateTwo.getPawnsPerPlayer());
        assertEquals(2, boardStateTwo.getPlayerCount());
        assertEquals(40, boardStateTwo.getBoardSize());

        beginPositionsFour.add(new Position(BEGIN, 54));
        beginPositionsFour.add(new Position(BEGIN, 64));
        BaseBoardState boardStateSeven = new BaseBoardState(40, 10, 3, beginPositionsFour);
        assertEquals(3, boardStateSeven.getPawnsPerPlayer());
        assertEquals(7, boardStateSeven.getPlayerCount());
        assertEquals(40, boardStateSeven.getBoardSize());

        List<Position> beginPositionsSix = new ArrayList<>(6);
        beginPositionsSix.add(new Position(BEGIN, 42));
        beginPositionsSix.add(new Position(BEGIN, 2));
        beginPositionsSix.add(new Position(BEGIN, 10));
        beginPositionsSix.add(new Position(BEGIN, 18));
        beginPositionsSix.add(new Position(BEGIN, 26));
        beginPositionsSix.add(new Position(BEGIN, 34));
        BaseBoardState boardStateSix = new BaseBoardState(48, 10, 9, beginPositionsSix);
        assertEquals(9, boardStateSix.getPawnsPerPlayer());
        assertEquals(6, boardStateSix.getPlayerCount());
        assertEquals(48, boardStateSix.getBoardSize());

    }

    @Test
    final void testEquals() {

        List<Position> beginPositionsFour = new ArrayList<>(4);
        beginPositionsFour.add(new Position(BEGIN, 34));
        beginPositionsFour.add(new Position(BEGIN, 4));
        beginPositionsFour.add(new Position(BEGIN, 14));
        beginPositionsFour.add(new Position(BEGIN, 24));
        BaseBoardState boardState = new BaseBoardState(40, 10, 4, beginPositionsFour);

        BaseBoardState anotherBaseBoardState = new BaseBoardState(40, 10, 4, beginPositionsFour);

        assertEquals(boardState, anotherBaseBoardState);
        assertEquals(anotherBaseBoardState, boardState);

        assertEquals(boardState, anotherBaseBoardState);

        assertEquals(boardState.hashCode(), anotherBaseBoardState.hashCode());

        List<Position> beginPositionsSix = new ArrayList<>(6);
        beginPositionsSix.add(new Position(BEGIN, 42));
        beginPositionsSix.add(new Position(BEGIN, 2));
        beginPositionsSix.add(new Position(BEGIN, 10));
        beginPositionsSix.add(new Position(BEGIN, 18));
        beginPositionsSix.add(new Position(BEGIN, 26));
        beginPositionsSix.add(new Position(BEGIN, 34));
        BaseBoardState boardStateSix = new BaseBoardState(48, 10, 6, beginPositionsSix);

        assertNotEquals(boardState, boardStateSix);
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    final void testEqualsNull() {

        List<Position> beginPositionsFour = new ArrayList<>(4);
        beginPositionsFour.add(new Position(BEGIN, 34));
        beginPositionsFour.add(new Position(BEGIN, 4));
        beginPositionsFour.add(new Position(BEGIN, 14));
        beginPositionsFour.add(new Position(BEGIN, 24));
        BaseBoardState boardState = new BaseBoardState(40, 10, 4, beginPositionsFour);

        // Test nulls
        assertNotEquals(null, boardState);
        assertNotEquals(this, boardState); // Different type on purpose.
    }

    @Test
    final void testGetPosition() {
        List<Position> beginPositionsFour = new ArrayList<>(4);
        beginPositionsFour.add(new Position(BEGIN, 34));
        beginPositionsFour.add(new Position(BEGIN, 4));
        beginPositionsFour.add(new Position(BEGIN, 14));
        beginPositionsFour.add(new Position(BEGIN, 24));
        BaseBoardState boardState = new BaseBoardState(40, 10, 4, beginPositionsFour);

        Position pZeroZero = boardState.getPosition(0, 0);
        Position pTwoTwo = boardState.getPosition(2, 2);
        Position pThreeOne = boardState.getPosition(3, 1);

        assertEquals(new Position(BEGIN, 34), pZeroZero);
        assertNotEquals(pZeroZero, pTwoTwo);
        assertNotEquals(pThreeOne, pTwoTwo);
        assertEquals(new Position(BEGIN, 14), pTwoTwo);
        assertEquals(boardState.getPosition(1, 1), boardState.getPosition(1, 3));

        assertNull(boardState.getPosition(-1, 1));
        assertNull(boardState.getPosition(-3, 5));
        assertNull(boardState.getPosition(0, 4));
        assertNull(boardState.getPosition(1, 4));
        assertNull(boardState.getPosition(-1, -1));
        assertNull(boardState.getPosition(0, -1));
        assertNull(boardState.getPosition(1, -1));

        logger.trace(boardState);
    }

    @Test
    final void testUnmodifiable() {
        List<Position> beginPositionsFour = new ArrayList<>(4);
        beginPositionsFour.add(new Position(BEGIN, 34));
        beginPositionsFour.add(new Position(BEGIN, 4));
        beginPositionsFour.add(new Position(BEGIN, 14));
        beginPositionsFour.add(new Position(BEGIN, 24));
        BaseBoardState boardState = new BaseBoardState(40, 10, 4, beginPositionsFour);
        List<Position> positions = boardState.getPositions(0);

        assertThrows(UnsupportedOperationException.class, () -> positions.add(new Position(HOME, 0)));

        assertThrows(UnsupportedOperationException.class, () -> positions.addFirst(new Position(HOME, 0)));
    }

    @Test
    final void testGetPlayer() {
        List<Position> beginPositionsFour = new ArrayList<>(4);
        beginPositionsFour.add(new Position(BEGIN, 34));
        beginPositionsFour.add(new Position(BEGIN, 4));
        beginPositionsFour.add(new Position(BEGIN, 14));
        beginPositionsFour.add(new Position(BEGIN, 24));
        BaseBoardState boardState = new BaseBoardState(40, 10, 4, beginPositionsFour);

        assertEquals(2, boardState.getPlayer(new Position(BEGIN, 14)));
        assertEquals(1, boardState.getPlayer(new Position(BEGIN, 4)));
        assertEquals(3, boardState.getPlayer(new Position(BEGIN, 24)));
        assertEquals(0, boardState.getPlayer(new Position(BEGIN, 34)));

        // Test the negative cases
        assertEquals(-1, boardState.getPlayer(null));
        assertEquals(-1, boardState.getPlayer(new Position(BEGIN, 17)));
        assertEquals(-1, boardState.getPlayer(new Position(EVENT, 13)));
        assertEquals(-1, boardState.getPlayer(new Position(HOME, 11)));
    }

    @Test
    final void testMove() {
        List<Position> beginPositionsTwo = new ArrayList<>(2);
        beginPositionsTwo.add(new Position(BEGIN, 14));
        beginPositionsTwo.add(new Position(BEGIN, 4));
        BaseBoardState boardState = new BaseBoardState(40, 10, 3, beginPositionsTwo);
        BaseBoardState boardStateCopy = new BaseBoardState(40, 10, 3, beginPositionsTwo);
        assertEquals(boardState, boardStateCopy);

        boardStateCopy.move(null);
        assertEquals(boardState, boardStateCopy);

        // Move from a position that is not in the state should result in identical
        // state.
        boardStateCopy.move(new Move(new Position(HOME, 7), new Position(HOME, 13)));
        assertEquals(boardState, boardStateCopy);

        Position from = boardState.getPosition(0, 0);
        Move inPlace = new Move(from, from);
        boardStateCopy.move(inPlace);
        assertEquals(boardState, boardStateCopy);

        Position start = new Position(EVENT, 0);
        Move move = new Move(from, start);
        boardStateCopy.move(move);

        assertNotEquals(boardState, boardStateCopy);
        boardState.move(move);

        assertEquals(start, boardState.getPosition(0, 2));
        assertEquals(start, boardState.shift(0).getPosition(0, 2));
        assertEquals(new Position(BEGIN, 4), boardState.getPosition(1, 0));
        BoardState shiftedState = boardState.shift(1);
        assertEquals(new Position(BEGIN, 34), shiftedState.getPosition(0, 2));
        assertEquals(new Position(BEGIN, 34), shiftedState.getPositions(0).get(2));

        // Same move applied to both states should result in identical states.
        assertEquals(boardState, boardStateCopy);


        boardState.move(getMove(BEGIN, 4, EVENT, 10));
        assertEquals(new Position(EVENT, 10), boardState.getPosition(1, 2));
        String expected = "(40)[P0={B14,B14,E0};P1={B4,B4,E10}]";
        assertEquals(expected, boardState.toString());
        assertEquals(new Position(EVENT, 10), boardState.getPosition(1,2));
        shiftedState = boardState.shift(1);
        assertEquals(new Position(EVENT, 0), shiftedState.getPosition(0, 2));
        assertEquals(new Position(EVENT, 0), shiftedState.getPositions(0).get(2));

        boardState.move(getMove(EVENT, 10, EVENT, 11));
        assertEquals(new Position(EVENT, 11), boardState.getPosition(1, 2));
        expected = "(40)[P0={B14,B14,E0};P1={B4,B4,E11}]";
        assertEquals(expected, boardState.toString());

        boardState.move(getMove(EVENT, 11, HOME, 1));
        assertEquals(new Position(BEGIN, 14), boardState.getPosition(0, 0));
        assertEquals(new Position(BEGIN, 4), boardState.getPosition(1, 0));
        assertEquals(new Position(HOME, 1), boardState.getPosition(1, 2));
        expected = "(40)[P0={B14,B14,E0};P1={B4,B4,H1}]";
        assertEquals(expected, boardState.toString());

        // Should not do anything, nothing on E2
        boardState.move(getMove(EVENT, 2, EVENT, 3));
        expected = "(40)[P0={B14,B14,E0};P1={B4,B4,H1}]";
        assertEquals(expected, boardState.toString());

        boardState.move(getMove(EVENT, 0, EVENT, 1));
        expected = "(40)[P0={B14,B14,E1};P1={B4,B4,H1}]";
        assertEquals(expected, boardState.toString());

        boardState.move(getMove(BEGIN, 14, EVENT, 0));
        expected = "(40)[P0={B14,E0,E1};P1={B4,B4,H1}]";
        assertEquals(expected, boardState.toString());

        // Now have player 1 move past player 2 and ensure correct ordering
        boardState.move(getMove(EVENT, 0, EVENT, 3));
        expected = "(40)[P0={B14,E1,E3};P1={B4,B4,H1}]";
        assertEquals(expected, boardState.toString());
    }

    @Test
    final void testOf() {
        BoardState bs = TestBoardState
                .of("(40)[P0={H0,H1,H2,H3};P1={B4,B4,B4,E25};P2={E16,E19,H22,H23};P3={B24,B24,E34,H33}]", 10);
        assertEquals(40, bs.getBoardSize());
        assertEquals(4, bs.getPawnsPerPlayer());
        assertEquals(4, bs.getPlayerCount());

        bs = TestBoardState.of("(20)[P0={B14,E1,E3};P1={B4,E11,H1}]", 10);
        assertEquals(20, bs.getBoardSize());
        assertEquals(3, bs.getPawnsPerPlayer());
        assertEquals(2, bs.getPlayerCount());

        bs = TestBoardState.of("(7)[P0={B2}]", 10);
        assertEquals(7, bs.getBoardSize());
        assertEquals(1, bs.getPawnsPerPlayer());
        assertEquals(1, bs.getPlayerCount());
    }

    // Create a test for boardState.getPositions that tries to modify the returned list to confirm it throws  a UnsupportedOperationException exception.
    @Test
    final void testGetPositions() {
        List<Position> beginPositionsFour = new ArrayList<>(4);
        beginPositionsFour.add(new Position(BEGIN, 34));
        beginPositionsFour.add(new Position(BEGIN, 4));
        beginPositionsFour.add(new Position(BEGIN, 14));
        beginPositionsFour.add(new Position(BEGIN, 24));
        BaseBoardState boardState = new BaseBoardState(40, 10, 4, beginPositionsFour);

        List<Position> positions = boardState.getPositions(0);
        assertThrows(UnsupportedOperationException.class, () -> positions.add(new Position(HOME, 0)));

        List<Position> morePositions = boardState.getPositions(1);
        assertThrows(UnsupportedOperationException.class, () -> morePositions.addFirst(new Position(HOME, 0)));
    }

}
