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
package com.javafx.mejn;

import javafx.beans.property.IntegerProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

public class PlayerView {

    /**
     * Get the color for the player with the given index.
     *
     * @param playerIndex the index of the player
     * @return the color for the player
     */
    public static Color getColor(int playerIndex) {
        return switch (playerIndex) {
            case 0 -> Color.DODGERBLUE;
            case 1 -> Color.GOLD;
            case 2 -> Color.GREEN;
            case 3 -> Color.RED;
            default -> Color.BLACK;
        };
    }

    /**
     * Get the color for the player with the given index.
     *
     * @param playerIndex the index of the player
     * @return the color for the player
     */
    public static Color getColor(IntegerProperty playerIndex) {
        return getColor(playerIndex.get());
    }

    /**
     * Get a radial gradient for the player with the given index.
     *
     * @param playerIndex the index of the player
     * @return the radial gradient for the player
     */
    public static RadialGradient getGradient(int playerIndex) {
        return new RadialGradient(
                0, // focusAngle
                0.0, // focusDistance
                0.5, // centerX
                0.5, // centerY
                0.4, // radius
                true, // proportional
                CycleMethod.NO_CYCLE, // cycleMethod
                new Stop(0.25, Color.WHITE), // stop at 30% with white color
                new Stop(1.0, getColor(playerIndex)) // stop at 100% with black color
        );
    }

    public static RadialGradient getGradient(IntegerProperty playerIndex) {
        return getGradient(playerIndex.get());
    }
}
