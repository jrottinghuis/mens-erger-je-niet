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

import com.rttnghs.mejn.Tournament;
import com.rttnghs.mejn.configuration.Config;
import com.rttnghs.mejn.statistics.EventCounter;
import com.rttnghs.mejn.statistics.Score;
import com.rttnghs.mejn.strategy.BaseStrategyFactory;
import com.rttnghs.mejn.strategy.StrategyFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * In-process {@link ComputeServer} implementation that runs tournament brackets in
 * parallel using a shared {@link ExecutorService}.
 *
 * <h2>Self-rescheduling work loop</h2>
 * <p>Once seeded via {@link #submitBatch}, a server worker thread runs batches in a
 * tight loop until {@link BatchCallback#onBatchComplete} returns {@code false}.  This
 * avoids the overhead of re-submitting work from the coordinator thread between batches
 * — exactly the property needed to eliminate an extra RMI round-trip once the remote
 * transport layer is added.
 *
 * <h2>Strategy resolution</h2>
 * <p>Only strategy <em>names</em> cross the API boundary (inside {@link BatchConfig});
 * this server resolves them locally via its own {@link StrategyFactory}.  This keeps
 * the interface serialization-friendly and mirrors the intended RMI contract.
 */
public class LocalComputeServer implements ComputeServer {

    private static final Logger logger = LogManager.getLogger(LocalComputeServer.class);

    /**
     * Shared work-stealing pool used by all {@code LocalComputeServer} instances for
     * running brackets in parallel.  A work-stealing pool matches the use-case well:
     * bracket tasks are independent and roughly equal in cost, so idle threads can
     * steal queued tasks from busy ones, yielding better CPU utilisation than a
     * fixed or cached pool when many brackets run concurrently.
     */
    private static final ExecutorService SHARED_POOL = Executors.newWorkStealingPool();

    private final String id;
    private final StrategyFactory strategyFactory;
    private final AtomicInteger batchesStarted = new AtomicInteger();
    private final AtomicInteger batchesCompleted = new AtomicInteger();

    /**
     * @param id              stable identifier for this server instance
     * @param strategyFactory factory used to resolve strategy names to instances
     */
    public LocalComputeServer(String id, StrategyFactory strategyFactory) {
        this.id = id;
        this.strategyFactory = strategyFactory;
    }

    /**
     * Convenience constructor that uses {@link BaseStrategyFactory}.
     *
     * @param id stable identifier for this server instance
     */
    public LocalComputeServer(String id) {
        this(id, new BaseStrategyFactory());
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Accepts the batch configuration and immediately returns.  A worker thread begins
     * the self-rescheduling loop: it keeps running batches until the coordinator signals
     * stop by returning {@code false} from {@link BatchCallback#onBatchComplete}.
     */
    @Override
    public void submitBatch(BatchConfig config, BatchCallback callback) {
        SHARED_POOL.submit(() -> runLoop(config, callback));
    }

    /**
     * Self-rescheduling loop: run one batch, post the result, and continue as long as the
     * coordinator returns {@code true}.  The loop runs entirely on one worker thread,
     * avoiding thread hand-offs between consecutive batches on the same server.
     */
    private void runLoop(BatchConfig config, BatchCallback callback) {
        boolean continueWork = true;
        while (continueWork) {
            Instant startedAt = Instant.now();
            batchesStarted.incrementAndGet();
            Map<String, Double> scores = runOneBatch(config);
            Instant completedAt = Instant.now();
            batchesCompleted.incrementAndGet();
            BatchResult result = new BatchResult(id, scores, startedAt, completedAt);
            continueWork = callback.onBatchComplete(result);
        }
        logger.debug("Server {} idling after {} completed batches", id, batchesCompleted.get());
    }

    private Map<String, Double> runOneBatch(BatchConfig config) {
        int playerCount = config.brackets().getFirst().size();
        Function<Integer, Integer> scorer = pos -> Score.get(pos, playerCount);
        EventCounter<String, Integer> batchCounts = new EventCounter<>();

        // Split each bracket into chunks so the work-stealing pool has enough independent
        // tasks to keep all available cores busy.  With 6 brackets and many-core machines,
        // 6 tasks would leave most cores idle; chunking provides ~6 × (gamesPerBatch /
        // gamesPerChunk) tasks for the pool to distribute freely.
        int gamesPerChunk = Config.configuration.getInt("powerAnalyzerGamesPerChunk", 500);
        int gamesPerBatch = config.gamesPerBatch();

        List<CompletableFuture<EventCounter<String, Integer>>> chunkFutures = new ArrayList<>();
        for (List<String> bracket : config.brackets()) {
            int remaining = gamesPerBatch;
            while (remaining > 0) {
                int chunkGames = Math.min(gamesPerChunk, remaining);
                remaining -= chunkGames;
                Tournament t = new Tournament(strategyFactory, bracket, chunkGames);
                chunkFutures.add(CompletableFuture.supplyAsync(t::play, SHARED_POOL));
            }
        }

        for (CompletableFuture<EventCounter<String, Integer>> f : chunkFutures) {
            try {
                batchCounts.add(f.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Batch chunk interrupted", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Batch chunk failed", e);
            }
        }

        Map<String, Integer> intScores = EventCounter.getNormalizedScores(batchCounts, scorer, 100);
        Map<String, Double> doubleScores = new TreeMap<>();
        intScores.forEach((k, v) -> doubleScores.put(k, v.doubleValue()));
        return doubleScores;
    }

    @Override
    public int getBatchesStarted() {
        return batchesStarted.get();
    }

    @Override
    public int getBatchesCompleted() {
        return batchesCompleted.get();
    }
}






