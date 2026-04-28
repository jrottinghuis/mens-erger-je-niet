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
import com.rttnghs.mejn.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * A BoardState implementation that shifts the positions of the base board state.
 */
public class ShiftingBoardState implements BoardState {
    private final BaseBoardState baseBoardState;
    private final  int playerIndex;
    private final int shift;
    private final int boardSize;
    private final int playerCount;

    /**
     * Constructs a ShiftingBoardState with the specified base board state and shift value.
     *
     * @param baseBoardState the base board state to be shifted
     * @throws IllegalArgumentException if baseBoardState is null
     */
    public ShiftingBoardState(BaseBoardState baseBoardState, int playerIndex) {
        if (baseBoardState == null) {
            throw new IllegalArgumentException("baseBoardState cannot be null");
        }
        this.baseBoardState = baseBoardState;
        this.playerIndex = playerIndex;
        this.shift = playerIndex * baseBoardState.getDotsPerPlayer() * -1;
        this.boardSize = baseBoardState.getBoardSize();
        this.playerCount = baseBoardState.getPlayerCount();
    }

    @Override
    public int getBoardSize() {
        return boardSize;
    }

    @Override
    public int getPawnsPerPlayer() {
        return baseBoardState.getPawnsPerPlayer();
    }

    @Override
    public int getPlayerCount() {
        return playerCount;
    }

    @Override
    public Position getPosition(int shiftedPlayer, int pawn) {
        int player = (shiftedPlayer + playerIndex) % playerCount;
        return baseBoardState.getPosition(player, pawn).move(shift).normalize(boardSize);
    }

    @Override
    public List<Position> getPositions(int shiftedPlayer) {
        int player = (shiftedPlayer + playerIndex) % playerCount;
        List<Position> playerState = baseBoardState.getPositions(player);
        List<Position> shiftedPlayerState = new ArrayList<>(playerState.size());
        for (Position position : playerState) {
            shiftedPlayerState.add(position.move(shift).normalize(boardSize));
        }
        return shiftedPlayerState;
    }

    @Override
    public int getPlayer(Position shiftedPosition) {
        Position position = shiftedPosition.move(-shift).normalize(boardSize);
        int player = baseBoardState.getPlayer(position);
        return (player + playerIndex) % playerCount;
    }

    @Override
    public boolean isFinished(int player) {
        return baseBoardState.isFinished(player);
    }

    @Override
    public BoardState shift(int playerIndex) {
        if (playerIndex == 0) {
            return this;
        }
        // This would be a little silly to have a shifting board state of a shifting board state.
        return new ShiftingBoardState(baseBoardState, this.playerIndex + playerIndex);
    }
}