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

public class PlayerView {


    // Create a public static method getColor that returns a Color based on an integer index, with a default value of Color.BLACK
    public static Color getColor(int index) {
        return switch (index) {
            case 0 -> Color.BLUE;
            case 1 -> Color.YELLOW;
            case 2 -> Color.GREEN;
            case 3 -> Color.RED;
            default -> Color.BLACK;
        };
    }

    public static Color getColor(IntegerProperty index) {
        return getColor(index.get());
    }
}
