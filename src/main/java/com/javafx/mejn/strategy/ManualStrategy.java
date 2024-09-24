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
package com.javafx.mejn.strategy;

import com.javafx.mejn.MainApp;
import com.rttnghs.mejn.BoardState;
import com.rttnghs.mejn.Move;
import com.rttnghs.mejn.Position;
import com.rttnghs.mejn.strategy.BaseStrategy;
import com.rttnghs.mejn.strategy.Strategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ManualStrategy extends BaseStrategy implements Strategy {

    private static final Logger logger = LogManager.getLogger(ManualStrategy.class);
    private Consumer<CompletableFuture<Position>> choiceHandler;

    public ManualStrategy(String name) {
        super(name, null);
    }

    // Create an argument that takes a lambda function with a Position argument and returns void
    // This will be used to set the choice of the player
    public synchronized void setChoiceHandler(Consumer<CompletableFuture<Position>> handler) {
        // Set the choice handler
        this.choiceHandler = handler;
    }

    /**
     * From Interface.
     */
    @Override
    public Move multiChoose(List<Move> choices, Supplier<BoardState> stateSupplier) {
        // We won't get here, because we will not invoke BaseStrategy.autoChoose
        return null;
    }

    /**
     * From Interface.
     */
    @Override
    public synchronized Move choose(List<Move> choices, Supplier<BoardState> boardStateSupplier) {

        CompletableFuture<Position> choiceFuture = new CompletableFuture<>();
        choiceHandler.accept(choiceFuture);

        while (true) {

            try {
                Position choice = choiceFuture.get();
                // Find the move that corresponds to the choice
                for (Move move : choices) {
                    if (move.to().equals(choice)) {
                        return move;
                    }
                }
                // We did not find a move that corresponds to the choice, so we'll wait again
                choiceFuture = new CompletableFuture<>();
                choiceHandler.accept(choiceFuture);
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for choice", e);
                Thread.currentThread().interrupt();
                // Yield to caller
                return null;
            } catch (ExecutionException e) {
                logger.error("Error while waiting for choice", e);
                throw new RuntimeException(e);
            }


            // Step 1: call pause on the Controller, so we can wait for the UI to make a choice
            // Step 2: Register a handler for the choice
            // Park into a sleep cycle until the choice is made
            // Once choice is made, return the choice
            return null;
        }
        // won't get here
    }

    /**
     * @param position this player finished in. First place = 1, runner up, 2, etc.
     */
    @Override
    public void finalize(int position) {
        // Nothing to do here.
    }
}
