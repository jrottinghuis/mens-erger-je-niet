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

import java.io.Serial;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * In-process {@link ComputeServer} implementation that runs tournament brackets in
 * parallel using dedicated dispatch and chunk executors.
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
     * Shared executor for dispatch loops.  Dispatch work is light but potentially
     * long-lived, so it is kept separate from chunk execution to avoid mixing control-
     * plane and compute-plane work in the same pool.
     */
    private static final ExecutorService DISPATCH_EXECUTOR = Executors.newCachedThreadPool();

    /**
     * Shared work-stealing pool used by all {@code LocalComputeServer} instances for
     * running tournament chunks in parallel.  Chunk tasks are independent and roughly
     * equal in cost, so idle threads can steal queued work from busy ones.
     */
    private static final ExecutorService CHUNK_EXECUTOR = Executors.newWorkStealingPool();

    private final String id;
    private final StrategyFactory strategyFactory;
    private final AtomicInteger batchesStarted = new AtomicInteger();
    private final AtomicInteger batchesCompleted = new AtomicInteger();
    private final AtomicInteger staleSubmissionsIgnored = new AtomicInteger();
    private final AtomicInteger supersededResultsDropped = new AtomicInteger();
    private final AtomicInteger abortedBatches = new AtomicInteger();
    private final AtomicInteger chunkFuturesCanceled = new AtomicInteger();
    private final Object stateLock = new Object();
    private BatchConfig pendingConfig;
    private BatchCallback pendingCallback;
    private boolean workerRunning;
    private int activeGenerationId = Integer.MIN_VALUE;
    private int latestGenerationId = Integer.MIN_VALUE;
    private int activeChunkGenerationId = Integer.MIN_VALUE;
    private List<CompletableFuture<EventCounter<String, Integer>>> activeChunkFutures = List.of();

    private record PendingWork(BatchConfig config, BatchCallback callback) {
    }

    private static final class SupersededGenerationException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;
    }

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
     *
     * <p>A newer generation supersedes any older pending generation on this server.
     * The currently running generation is allowed to finish its <em>current</em> batch,
     * but will not self-reschedule once a newer generation has been submitted.
     */
    @Override
    public void submitBatch(BatchConfig config, BatchCallback callback) {
        synchronized (stateLock) {
            if (config.generationId() < latestGenerationId) {
                staleSubmissionsIgnored.incrementAndGet();
                logger.debug("Server {} ignoring stale generation {} (latest seen: {})",
                        id, config.generationId(), latestGenerationId);
                return;
            }
            boolean newerGeneration = config.generationId() > latestGenerationId;
            latestGenerationId = config.generationId();

            if (newerGeneration) {
                cancelActiveChunkFuturesForOlderGeneration(config.generationId());
            }

            if (workerRunning && activeGenerationId == config.generationId()) {
                logger.debug("Server {} ignoring duplicate submit for active generation {}",
                        id, config.generationId());
                return;
            }

            pendingConfig = config;
            pendingCallback = callback;

            if (!workerRunning) {
                workerRunning = true;
                DISPATCH_EXECUTOR.submit(this::runDispatchLoop);
            }
        }
    }

    /**
     * Dispatch loop: run exactly one generation at a time, while allowing a newer
     * generation to supersede the current one at the next batch boundary.
     */
    private void runDispatchLoop() {
        while (true) {
            PendingWork pendingWork = takePendingWork();
            if (pendingWork == null) {
                logger.debug("Server {} dispatch loop idling", id);
                return;
            }

            logger.debug("Server {} starting generation {}", id, pendingWork.config().generationId());
            runGeneration(pendingWork.config(), pendingWork.callback());
        }
    }

    /**
     * Self-rescheduling loop for one generation: run batches until the coordinator says
     * stop or a newer generation supersedes this one.
     */
    private void runGeneration(BatchConfig config, BatchCallback callback) {
        boolean continueWork = true;
        while (continueWork) {
            Instant startedAt = Instant.now();
            batchesStarted.incrementAndGet();
            Map<String, Double> scores;
            try {
                scores = runOneBatch(config);
            } catch (SupersededGenerationException e) {
                abortedBatches.incrementAndGet();
                logger.debug("Server {} aborting generation {} during batch execution after supersession",
                        id, config.generationId());
                break;
            }
            Instant completedAt = Instant.now();
            batchesCompleted.incrementAndGet();

            long latestSeenGeneration = latestSeenGenerationId();
            if (isSuperseded(config.generationId())) {
                supersededResultsDropped.incrementAndGet();
                logger.debug("Server {} dropping stale result for generation {} (latest seen: {})",
                        id, config.generationId(), latestSeenGeneration);
                break;
            }

            BatchResult result = new BatchResult(id, config.generationId(), scores, startedAt, completedAt);

            boolean callbackRequestedContinue = callback.onBatchComplete(result);
            continueWork = callbackRequestedContinue && !isSuperseded(config.generationId());
        }
        logger.debug("Server {} parked generation {} after {} completed batches",
                id, config.generationId(), batchesCompleted.get());
    }

    private PendingWork takePendingWork() {
        synchronized (stateLock) {
            if (pendingConfig == null) {
                workerRunning = false;
                activeGenerationId = Integer.MIN_VALUE;
                return null;
            }
            BatchConfig config = pendingConfig;
            BatchCallback callback = pendingCallback;
            pendingConfig = null;
            pendingCallback = null;
            activeGenerationId = config.generationId();
            return new PendingWork(config, callback);
        }
    }

    private boolean isSuperseded(int generationId) {
        synchronized (stateLock) {
            return latestGenerationId > generationId;
        }
    }

    private int latestSeenGenerationId() {
        synchronized (stateLock) {
            return latestGenerationId;
        }
    }

    private void registerActiveChunkFutures(int generationId, List<CompletableFuture<EventCounter<String, Integer>>> chunkFutures) {
        synchronized (stateLock) {
            activeChunkGenerationId = generationId;
            activeChunkFutures = new ArrayList<>(chunkFutures);
        }
    }

    private void clearActiveChunkFutures(int generationId) {
        synchronized (stateLock) {
            if (activeChunkGenerationId == generationId) {
                activeChunkGenerationId = Integer.MIN_VALUE;
                activeChunkFutures = List.of();
            }
        }
    }

    private void cancelActiveChunkFuturesForOlderGeneration(int newerGenerationId) {
        List<CompletableFuture<EventCounter<String, Integer>>> futuresToCancel = Collections.emptyList();
        int generationToCancel = Integer.MIN_VALUE;
        synchronized (stateLock) {
            if (activeChunkGenerationId != Integer.MIN_VALUE && activeChunkGenerationId < newerGenerationId) {
                generationToCancel = activeChunkGenerationId;
                futuresToCancel = new ArrayList<>(activeChunkFutures);
            }
        }
        if (!futuresToCancel.isEmpty()) {
            int canceled = 0;
            logger.debug("Server {} canceling {} queued chunk future(s) for superseded generation {} in favor of {}",
                    id, futuresToCancel.size(), generationToCancel, newerGenerationId);
            for (CompletableFuture<EventCounter<String, Integer>> future : futuresToCancel) {
                if (future.cancel(false)) {
                    canceled++;
                }
            }
            chunkFuturesCanceled.addAndGet(canceled);
        }
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
                chunkFutures.add(CompletableFuture.supplyAsync(t::play, CHUNK_EXECUTOR));
            }
        }

        registerActiveChunkFutures(config.generationId(), chunkFutures);
        cancelActiveChunkFuturesForOlderGeneration(latestSeenGenerationId());
        CompletableFuture<Void> allChunks = CompletableFuture.allOf(chunkFutures.toArray(CompletableFuture[]::new));

        try {
            allChunks.join();
            for (CompletableFuture<EventCounter<String, Integer>> f : chunkFutures) {
                batchCounts.add(f.join());
            }
        } catch (CompletionException | CancellationException e) {
            if (isSuperseded(config.generationId())) {
                throw new SupersededGenerationException();
            }
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Batch chunk failed", cause == null ? e : cause);
        } finally {
            clearActiveChunkFutures(config.generationId());
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

    @Override
    public StatsSnapshot statsSnapshot() {
        return new StatsSnapshot(
                id,
                batchesStarted.get(),
                batchesCompleted.get(),
                staleSubmissionsIgnored.get(),
                supersededResultsDropped.get(),
                abortedBatches.get(),
                chunkFuturesCanceled.get());
    }
}






