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
package com.rttnghs.mejn.rmi;

import com.rttnghs.mejn.de.RankingStrategyTournament;
import com.rttnghs.rmi.protocol.TournamentBatch;
import com.rttnghs.rmi.protocol.impl.ChunkedServer;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Physical server that executes tournament batches by splitting them into
 * chunks, running each chunk in parallel, and merging normalized integer scores.
 *
 * <h2>Execution flow</h2>
 * <ol>
 *   <li>Receives a {@link TournamentBatch} with competitors and repetitions.</li>
 *   <li>Splitter divides repetitions into sized chunks (configurable).</li>
 *   <li>Each chunk runs tournaments with {@link RankingStrategyTournament}.</li>
 *   <li>Merger sums placement counts and normalizes to integer scores.</li>
 *   <li>Returns {@code ArrayList<Integer>} of normalized scores by position.</li>
 * </ol>
 *
 * <h2>Scoring</h2>
 * <p>Uses {@code Score.get()} and {@code EventCounter.getNormalizedScores()}
 * with a configurable normalization scale (default 100).
 */
public class TournamentBatchServer extends ChunkedServer<TournamentBatch, ArrayList<Integer>> {

    private static final Logger logger = Logger.getLogger(TournamentBatchServer.class.getName());

    private final RankingStrategyTournament tournament;
    private final int chunkSize;

    /**
     * Constructs the server with default chunk size (50 repetitions).
     *
     * @param maxPendingTasks maximum number of pending tournament batches
     * @throws RemoteException if RMI export fails
     */
    public TournamentBatchServer(int maxPendingTasks) throws RemoteException {
        this(maxPendingTasks, 50);
    }

    /**
     * Constructs the server with custom configuration.
     *
     * @param maxPendingTasks maximum number of pending tournament batches
     * @param chunkSize repetitions per chunk (e.g., 50)
     * @throws RemoteException if RMI export fails
     * @throws IllegalArgumentException if chunkSize <= 0
     */
    public TournamentBatchServer(int maxPendingTasks, int chunkSize)
            throws RemoteException {
        super(
            maxPendingTasks,
            batch -> splitBatch(batch, chunkSize),
            TournamentBatchServer::mergeScores
        );
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be > 0");
        }
        this.tournament = new RankingStrategyTournament();
        this.chunkSize = chunkSize;
        logger.info(() -> "TournamentBatchServer initialized: chunkSize=%d".formatted(chunkSize));
    }

    @Override
    protected boolean validateTask(TournamentBatch batch) {
        if (batch == null) {
            logger.warning("Received null batch");
            return false;
        }
        List<List<Integer>> competitors = batch.competitors();
        if (competitors == null || competitors.isEmpty()) {
            logger.warning("Batch has no competitors");
            return false;
        }
        if (batch.repetitions() <= 0) {
            logger.warning("Batch has non-positive repetitions: %d".formatted(batch.repetitions()));
            return false;
        }

        // Validate each competitor's parameters for SomeRankingStrategy/SomeMoveValuator
        for (int i = 0; i < competitors.size(); i++) {
            final int index = i;
            if (!tournament.isValidCompetitor(competitors.get(i))) {
                logger.warning(() -> "Competitor %d has invalid parameters".formatted(index));
                return false;
            }
        }
        return true;
    }

    /**
     * Splits a tournament batch into smaller chunks with proportionally fewer repetitions.
     *
     * @param batch the batch to split
     * @param targetChunkSize target repetitions per chunk
     * @return list of sub-batches with same competitors, fewer repetitions each
     */
    private static List<TournamentBatch> splitBatch(TournamentBatch batch, int targetChunkSize) {
        int numChunks = Math.max(1, (batch.repetitions() + targetChunkSize - 1) / targetChunkSize);
        int repPerChunk = (batch.repetitions() + numChunks - 1) / numChunks;

        List<TournamentBatch> chunks = new ArrayList<>(numChunks);
        for (int i = 0; i < numChunks; i++) {
            chunks.add(new TournamentBatch(batch.competitors(), repPerChunk));
        }
        logger.fine(() -> "Split batch (%d reps) into %d chunks (%d reps each)"
            .formatted(batch.repetitions(), numChunks, repPerChunk));
        return chunks;
    }

    /**
     * Merges two chunk results by summing scores positionally.
     *
     * @param scores1 first chunk scores
     * @param scores2 second chunk scores
     * @return merged scores (sum of positions)
     */
    private static ArrayList<Integer> mergeScores(ArrayList<Integer> scores1, ArrayList<Integer> scores2) {
        if (scores1.size() != scores2.size()) {
            throw new IllegalArgumentException("Score list sizes don't match: %d vs %d"
                .formatted(scores1.size(), scores2.size()));
        }
        ArrayList<Integer> merged = new ArrayList<>(scores1.size());
        for (int i = 0; i < scores1.size(); i++) {
            merged.add(scores1.get(i) + scores2.get(i));
        }
        return merged;
    }

    /**
     * Execute a single chunk: run tournament and return integer normalized scores.
     *
     * @param chunk the chunk to execute
     * @return integer normalized scores in competitor position order
     */
    @Override
    protected ArrayList<Integer> executeChunk(TournamentBatch chunk) {
        // Run the tournament via RankingStrategyTournament, which returns List<Double>
        List<Double> doubleScores = tournament.runBracket(chunk.competitors(), chunk.repetitions());

        // Convert to ArrayList of integers
        ArrayList<Integer> intScores = new ArrayList<>(doubleScores.size());
        for (Double score : doubleScores) {
            intScores.add(score.intValue());
        }
        return intScores;
    }

    /**
     * Override ForkJoinPool to use a processor-sized pool for isolation and predictability.
     *
     * @return a custom ForkJoinPool sized to available processors
     */
    @Override
    protected ForkJoinPool getForkJoinPool() {
        int processors = Runtime.getRuntime().availableProcessors();
        ForkJoinPool pool = new ForkJoinPool(processors);
        logger.fine(() -> "Using ForkJoinPool with %d threads".formatted(processors));
        return pool;
    }
}
