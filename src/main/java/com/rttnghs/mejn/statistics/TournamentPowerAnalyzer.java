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
package com.rttnghs.mejn.statistics;

import com.rttnghs.mejn.Tournament;
import com.rttnghs.mejn.configuration.Config;
import com.rttnghs.mejn.rmi.BatchCallback;
import com.rttnghs.mejn.rmi.BatchConfig;
import com.rttnghs.mejn.rmi.BatchResult;
import com.rttnghs.mejn.rmi.ComputeServer;
import com.rttnghs.mejn.rmi.LocalComputeServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Sequential tournament runner with adaptive early stopping.
 *
 * <h2>Design</h2>
 * <p>Each {@link ComputeServer} in the provided list runs batches in a self-rescheduling
 * loop: after completing a batch it calls back into {@link RunState#onBatchComplete}, and
 * the boolean return value tells it whether to start another batch immediately.  This
 * eliminates the per-batch scheduling overhead and maps directly onto the intended RMI
 * callback model — the server reply to a callback doubles as the next-work decision,
 * saving a full round-trip.
 *
 * <p>The main thread blocks on a {@link Phaser} and is woken exactly once when the stop
 * condition is triggered, removing any polling loop.
 *
 * <h2>Stop condition</h2>
 * <p>After each completed batch the analyzer checks every adjacent pair of strategies
 * (ordered by current mean score).  A pair is <em>resolved</em> when either:
 * <ul>
 *   <li>their mean difference is smaller than {@code mde} (practically equivalent), or</li>
 *   <li>we have enough observations to detect that difference with 95 % confidence
 *       <strong>and</strong> 80 % power — i.e.
 *       {@code n ≥ 2 × ((Z_α/2 + Z_β) × σ_pooled / Δ)²}.</li>
 * </ul>
 * <p>Sampling stops once <em>all</em> adjacent pairs are resolved, or the optional
 * {@code maxBatches} cap is hit.
 *
 * <h2>Batch size guidance</h2>
 * <p>For a local run 1 000–5 000 games/batch gives frequent variance updates with low
 * overhead.  For RMI dispatch, 5 000–10 000 games/batch amortises the network
 * round-trip while still updating the estimate every few seconds.
 */
public class TournamentPowerAnalyzer {

    private static final Logger logger = LogManager.getLogger(TournamentPowerAnalyzer.class);

    /** Z for β = 0.20 (80 % power). */
    public static final double Z_BETA = 0.841;

    /** Combined Z factor: (Z_α/2 + Z_β)². */
    private static final double Z_FACTOR_SQ = Math.pow(RunningStats.Z_ALPHA_2 + Z_BETA, 2);

    // ── Configuration ─────────────────────────────────────────────────────────

    private final List<ComputeServer> servers;
    private final List<List<String>> brackets;
    private final int playerCount;

    /**
     * Primary constructor.
     *
     * @param servers  compute servers to dispatch work to; one batch runs per server at a time
     * @param brackets list of bracket strategy-name lists
     */
    public TournamentPowerAnalyzer(List<ComputeServer> servers, List<List<String>> brackets) {
        Objects.requireNonNull(servers, "servers cannot be null");
        if (servers.isEmpty()) throw new IllegalArgumentException("servers must not be empty");
        Objects.requireNonNull(brackets, "brackets cannot be null");
        if (brackets.isEmpty()) throw new IllegalArgumentException("brackets must not be empty");

        this.servers = List.copyOf(servers);
        this.brackets = brackets;
        this.playerCount = brackets.getFirst().size();
    }


    // ── Power analysis helpers ─────────────────────────────────────────────────

    /**
     * Required number of <em>observations per strategy</em> (i.e. completed
     * batches) to detect a true mean difference of {@code delta} with 95 %
     * confidence and 80 % power, given a pooled standard deviation.
     *
     * <p>Formula: {@code n = 2 × (Z_α/2 + Z_β)² × σ²_pooled / δ²}
     * — but here each observation is one batch, so this is the number of
     * batches required.
     *
     * @param delta        expected mean score difference between the two strategies
     * @param pooledStdDev pooled sample standard deviation of batch scores
     * @return required number of batches (≥ 1)
     */
    public static int requiredBatches(double delta, double pooledStdDev) {
        if (delta <= 0 || pooledStdDev <= 0) return 1;
        return (int) Math.ceil(Z_FACTOR_SQ * 2.0 * (pooledStdDev * pooledStdDev) / (delta * delta));
    }

    /**
     * Actual power for a given observed delta and pooled stddev at the current
     * sample size {@code n}.
     *
     * @param n            number of observations (batches) per strategy
     * @param delta        observed mean score difference
     * @param pooledStdDev pooled sample standard deviation
     * @return estimated power in [0, 1]
     */
    public static double observedPower(int n, double delta, double pooledStdDev) {
        if (n < 2 || pooledStdDev <= 0 || delta <= 0) return 0.0;
        // z = sqrt(n/2) * delta / sigma - Z_alpha/2
        double z = Math.sqrt(n / 2.0) * delta / pooledStdDev - RunningStats.Z_ALPHA_2;
        // Φ(z) approximation using a logistic sigmoid (accurate to ~0.5%)
        return 1.0 / (1.0 + Math.exp(-1.7 * z));
    }

    // ── Main run loop ──────────────────────────────────────────────────────────

    /**
     * Run batches with adaptive early stopping.
     *
     * <p>Seeds each server with one {@code submitBatch} call, then blocks on a
     * {@link Phaser} until the stop condition is met (or the optional timeout fires).
     * Servers self-reschedule via the callback return value — no polling needed.
     *
     * @return the final {@link Result}
     * @throws InterruptedException if the main thread is interrupted while waiting
     */
    public Result run() throws InterruptedException {
        Instant start = Instant.now();

        int gamesPerBatch = Config.configuration.getInt("powerAnalyzerGamesPerBatch",
                Config.configuration.getInt("games"));
        BatchConfig config = new BatchConfig(brackets, gamesPerBatch);

        // One registered party = the main thread.  A server callback calls arrive()
        // exactly once when the stop condition is first triggered.
        Phaser phaser = new Phaser(1);
        int waitPhase = phaser.getPhase();

        RunState state = new RunState(config, phaser);

        // Seed every server — each one self-reschedules until told to stop.
        for (ComputeServer server : servers) {
            server.submitBatch(config, state);
        }

        // Block until stop condition or wall-clock timeout.
        int timeoutSeconds = Config.configuration.getInt("powerAnalyzerTimeoutSeconds", 3600);
        try {
            phaser.awaitAdvanceInterruptibly(waitPhase, timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.warn("TournamentPowerAnalyzer timed out after {} seconds; stopping.", timeoutSeconds);
            state.triggerStop(StopReason.MAX_BATCHES, state.completedBatches);
        }

        Duration elapsed = Duration.between(start, Instant.now());
        int numBrackets = brackets.size();
        int completed = state.completedBatches;
        return new Result(
                completed, gamesPerBatch,
                Collections.unmodifiableMap(state.statsMap),
                buildPairAnalyses(state.statsMap, completed),
                state.stopReason, elapsed,
                playerCount, numBrackets,
                List.copyOf(servers));
    }

    // ── Per-run state (implements the callback) ────────────────────────────────

    /**
     * Holds all mutable state for a single {@link #run()} invocation and implements
     * {@link BatchCallback} so it can be passed directly to the servers.
     *
     * <p>All mutation happens inside {@code synchronized(this)}, so concurrent server
     * threads never race on statistics updates or stop-condition checks.
     */
    private class RunState implements BatchCallback {

        final Map<String, RunningStats> statsMap = new TreeMap<>();
        private final BatchConfig config;
        private final Phaser phaser;

        // guarded by synchronized(this)
        int completedBatches = 0;
        StopReason stopReason = StopReason.MAX_BATCHES;
        private boolean stopped = false;

        RunState(BatchConfig config, Phaser phaser) {
            this.config = config;
            this.phaser = phaser;
        }

        @Override
        public synchronized boolean onBatchComplete(BatchResult result) {
            // If stop was already signaled by another server's callback, park immediately.
            if (stopped) return false;

            completedBatches++;
            result.scores().forEach((strategy, score) ->
                    statsMap.computeIfAbsent(strategy, _ -> new RunningStats()).update(score));

            logger.debug("Batch {} complete from {} ({}ms)",
                    completedBatches, result.serverId(), result.elapsedMillis());

            if (completedBatches % 5 == 0) {
                int numBrackets = brackets.size();
                long gamesRun = (long) completedBatches * numBrackets * config.gamesPerBatch();
                logger.info("After {} batches ({} tournaments, {} games): {}",
                        completedBatches, (long) completedBatches * numBrackets, gamesRun,
                        formatPowerSummary(statsMap, completedBatches));
            }

            int warmupBatches = Config.configuration.getInt("powerAnalyzerWarmupBatches", 5);
            int maxBatches = Config.configuration.getInt("powerAnalyzerMaxBatches", 200);

            if (completedBatches >= warmupBatches) {
                StopEvaluation eval = evaluateStop(statsMap, completedBatches);
                if (eval.shouldStop()) {
                    triggerStop(eval.reason(), completedBatches);
                    return false;
                }
            }

            if (completedBatches >= maxBatches) {
                triggerStop(StopReason.MAX_BATCHES, completedBatches);
                return false;
            }

            return true;
        }

        /** Signal stop exactly once; safe to call while holding or without the monitor. */
        synchronized void triggerStop(StopReason reason, int completed) {
            if (stopped) return;
            stopped = true;
            stopReason = reason;
            int numBrackets = brackets.size();
            int tournamentsRun = completed * numBrackets;
            long gamesRun = (long) tournamentsRun * config.gamesPerBatch();
            logger.info("Early stop after {} batches ({} tournaments, {} games). Reason: {}",
                    completed, tournamentsRun, gamesRun, reason);
            phaser.arrive(); // unblock the main thread
        }
    }
    // ── Stop condition evaluation ──────────────────────────────────────────────

    private StopEvaluation evaluateStop(Map<String, RunningStats> statsMap, int n) {
        List<PairAnalysis> pairs = buildPairAnalyses(statsMap, n);

        // Log all deltas and variances for diagnostics
        for (PairAnalysis p : pairs) {
            logger.info("DIAGNOSTIC: {} vs {}: delta={}, pooledStddev={}, pooledVariance={}, power={}%, resolved={}, practicallyEqual={}",
                    p.strategyA(), p.strategyB(),
                    String.format("%.3f", p.delta()),
                    String.format("%.3f", p.pooledStddev()),
                    String.format("%.3f", p.pooledVariance()),
                    String.format("%.1f", p.observedPower() * 100),
                    p.resolved(), p.practicallyEqual());
        }

        // All pairs must be resolved for an early stop.
        boolean allResolved = pairs.stream().allMatch(PairAnalysis::resolved);
        if (allResolved) {
            return new StopEvaluation(true, StopReason.SUFFICIENT_POWER);
        }

        // Log the least-resolved pair.
        pairs.stream().filter(p -> !p.resolved()).min(Comparator.comparingDouble(PairAnalysis::observedPower)).ifPresent(p -> logger.debug("  Closest unresolved pair: {} vs {} delta={} power={}% need {} more batches", p.strategyA(), p.strategyB(), String.format("%.2f", p.delta()), String.format("%.1f", p.observedPower() * 100), Math.max(0, p.requiredBatches() - n)));
        return new StopEvaluation(false, null);
    }

    private List<PairAnalysis> buildPairAnalyses(Map<String, RunningStats> statsMap, int n) {
        // Sort strategies by current mean score descending.
        List<Map.Entry<String, RunningStats>> ranked = statsMap.entrySet().stream().sorted(Comparator.comparingDouble((Map.Entry<String, RunningStats> e) -> e.getValue().mean()).reversed()).toList();

        List<PairAnalysis> pairs = new ArrayList<>();
        for (int i = 0; i + 1 < ranked.size(); i++) {
            String nameA = ranked.get(i).getKey();
            String nameB = ranked.get(i + 1).getKey();
            RunningStats sA = ranked.get(i).getValue();
            RunningStats sB = ranked.get(i + 1).getValue();

            double delta = sA.mean() - sB.mean();
            double pooledVariance = (sA.variance() + sB.variance()) / 2.0;
            double pooledStddev = Math.sqrt(pooledVariance);

            boolean practicallyEqual = delta < Config.configuration.getDouble("powerAnalyzerMde", 2.0);
            int reqBatches = practicallyEqual ? 0 : requiredBatches(delta, pooledStddev);
            double power = observedPower(n, delta, pooledStddev);
            boolean resolved = practicallyEqual || (n >= reqBatches && power >= 0.80);

            pairs.add(new PairAnalysis(nameA, nameB, delta, pooledStddev, pooledVariance, power, reqBatches, practicallyEqual, resolved));
        }
        return pairs;
    }

    // ── Formatting helpers ─────────────────────────────────────────────────────


    private String formatPowerSummary(Map<String, RunningStats> statsMap, int n) {
        List<PairAnalysis> pairs = buildPairAnalyses(statsMap, n);
        StringBuilder sb = new StringBuilder();
        for (PairAnalysis p : pairs) {
            sb.append(String.format("[%s vs %s: Δ=%.1f power=%.0f%% need~%d batches] ", p.strategyA(), p.strategyB(), p.delta(), p.observedPower() * 100, Math.max(0, p.requiredBatches() - n)));
        }
        return sb.toString().strip();
    }

    // ── Records ────────────────────────────────────────────────────────────────

    /**
     * Reasons a run terminated.
     */
    public enum StopReason {
        /**
         * All adjacent strategy pairs are resolved with ≥ 80 % power.
         */
        SUFFICIENT_POWER,
        /**
         * The {@code maxBatches} cap was hit before convergence.
         */
        MAX_BATCHES
    }

    private record StopEvaluation(boolean shouldStop, StopReason reason) {
    }

    /**
     * Power analysis for a single adjacent strategy pair.
     *
     * @param strategyA        higher-ranked strategy (by current mean score)
     * @param strategyB        lower-ranked strategy
     * @param delta            observed mean score difference (A − B)
     * @param pooledStddev     pooled sample standard deviation of batch scores
     * @param observedPower    estimated power at the current sample size
     * @param requiredBatches  batches needed to reach 80 % power for this delta (0 if practically equal)
     * @param practicallyEqual true if {@code delta < mde}
     * @param resolved         true if this pair needs no more samples
     */
    public record PairAnalysis(String strategyA, String strategyB, double delta, double pooledStddev, double pooledVariance,
                               double observedPower, int requiredBatches, boolean practicallyEqual, boolean resolved) {
    }

    /**
     * Aggregated results from a full power-analyzer run.
     *
     * @param completedBatches total batches fully executed and incorporated into statistics
     * @param gamesPerBatch    games per tournament
     * @param runningStats     per-strategy running statistics
     * @param pairAnalyses     final power analysis for each adjacent strategy pair
     * @param stopReason       why the run terminated
     * @param elapsed          wall-clock time
     * @param playerCount      number of players per game
     * @param numBrackets      number of brackets per batch
     * @param servers          servers used in this run (for per-server reporting)
     */
    public record Result(
            int completedBatches, int gamesPerBatch,
            Map<String, RunningStats> runningStats,
            List<PairAnalysis> pairAnalyses,
            StopReason stopReason, Duration elapsed,
            int playerCount, int numBrackets,
            List<ComputeServer> servers) {

        /** Total games played across all completed batches and brackets. */
        public long trueTotalGames() {
            return (long) completedBatches * numBrackets * gamesPerBatch;
        }

        /** Sum of {@link ComputeServer#getBatchesStarted()} across all servers. */
        public int totalBatchesStarted() {
            return servers.stream().mapToInt(ComputeServer::getBatchesStarted).sum();
        }

        /**
         * Batches started on all servers minus {@link #completedBatches}: batches that were
         * running when the stop condition fired and completed after the stop was signaled.
         */
        public int batchesOverrun() {
            return totalBatchesStarted() - completedBatches;
        }

        /** Formatted summary table. */
        public String toSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%nTournamentPowerAnalyzer: %d batches × %,d games/batch × %d brackets  (total: %,d games)  stop=%s%n",
                    completedBatches, gamesPerBatch, numBrackets, trueTotalGames(), stopReason));

            // Per-server batch stats
            sb.append(String.format("%-20s %14s %14s%n", "Server", "started", "completed"));
            sb.append("-".repeat(50)).append(System.lineSeparator());
            for (ComputeServer s : servers) {
                sb.append(String.format("%-20s %14d %14d%n",
                        s.getId(), s.getBatchesStarted(), s.getBatchesCompleted()));
            }
            sb.append(String.format("%-20s %14d %14d%n", "TOTAL", totalBatchesStarted(), completedBatches));
            sb.append(System.lineSeparator());

            // Strategy statistics
            sb.append(String.format("%-24s %7s %7s %7s %7s%n", "Strategy", "mean", "stddev", "stderr", "95%CI±"));
            sb.append("-".repeat(65)).append(System.lineSeparator());
            runningStats.entrySet().stream()
                    .sorted(Comparator.comparingDouble((Map.Entry<String, RunningStats> e) -> e.getValue().mean()).reversed())
                    .forEach(e -> {
                        RunningStats.Snapshot s = e.getValue().snapshot();
                        sb.append(String.format("%-24s %7.2f %7.2f %7.2f %7.2f%n",
                                e.getKey(), s.mean(), s.stddev(), s.stderr(), s.moe95()));
                    });
            sb.append(System.lineSeparator());

            // Pair analysis
            sb.append(String.format("%-24s %-24s %6s %7s %7s %10s %10s %12s%n",
                    "Strategy A", "Strategy B", "Δ", "power", "n_need", "equiv?", "resolved?", "variance"));
            sb.append("-".repeat(112)).append(System.lineSeparator());
            pairAnalyses.forEach(p -> sb.append(String.format(
                    "%-24s %-24s %6.2f %6.0f%% %7d %10s %10s %12.3f%n",
                    p.strategyA(), p.strategyB(), p.delta(), p.observedPower() * 100,
                    p.requiredBatches(),
                    p.practicallyEqual() ? "YES" : "no",
                    p.resolved() ? "YES" : "no",
                    p.pooledVariance())));
            sb.append(String.format("%nElapsed: %s%n", elapsed));
            return sb.toString();
        }
    }

    // ── Entry point ────────────────────────────────────────────────────────────

    /**
     * Run as a standalone program.  Configuration keys:
     * <ul>
     *   <li>{@code powerAnalyzerGamesPerBatch} — games per tournament (falls back to {@code games})</li>
     *   <li>{@code powerAnalyzerWarmupBatches} — batches before stop check (default 5)</li>
     *   <li>{@code powerAnalyzerMaxBatches} — hard cap on total batches (default 200)</li>
     *   <li>{@code powerAnalyzerConcurrency} — number of local servers to create (default 4)</li>
     *   <li>{@code powerAnalyzerMde} — minimum detectable effect on score scale (default 2.0)</li>
     *   <li>{@code powerAnalyzerTimeoutSeconds} — wall-clock timeout for the whole run (default 3600)</li>
     * </ul>
     */
    public static void main(String[] args) throws InterruptedException {
        List<String> strategyNames = Tournament.getStrategyNames();
        List<List<String>> brackets = Tournament.getStrategyNameBrackets(strategyNames);

        int concurrency = Config.configuration.getInt("powerAnalyzerConcurrency", 4);

        List<ComputeServer> servers = new ArrayList<>();
        for (int i = 0; i < concurrency; i++) {
            servers.add(new LocalComputeServer("local-" + i));
        }

        TournamentPowerAnalyzer analyzer = new TournamentPowerAnalyzer(servers, brackets);
        Result result = analyzer.run();
        logger.info("{}", result.toSummary());
    }
}

