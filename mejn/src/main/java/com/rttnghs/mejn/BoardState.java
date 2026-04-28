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

import java.util.List;

public interface BoardState {

    /**
     * @return the total number of spots in the EVENT layer of the board.
     */
    int getBoardSize();

    /**
     * @return the pawnsPerPlayer
     */
    int getPawnsPerPlayer();

    /**
     * @return the playerCount
     */
    int getPlayerCount();

    /**
     * @param player index of player in the state
     * @param pawn   index of the pawn in the state
     * @return the position of the given pawn for the given player or null if there
     *         is no such pawn.
     */
    Position getPosition(int player, int pawn);

    /**
     * Return (unmodifiable) list of positions for the given player.
     *
     * @param player index of player in the state
     * @return the list of positions for the given player, or null if there is no
     *         such player.
     */
    List<Position> getPositions(int player);

    /**
     * @param position to locate in the state lists.
     * @return the zero based index of the player that is at the given position for
     *         the state or -1 if there is no such player.
     */
    int getPlayer(Position position);

    /**
     * @param playerIndex index of the player to shift the perspective for.
     * @return shifted perspective of the board.
     */
    BoardState shift(int playerIndex);

    /**
     * @param player non-null collection with positions.
     * @return true if all Positions in the collection are in the HOME layer or the player has no pawns. Non-existing players are also considered finished.
     */
    boolean isFinished(int player);
}
