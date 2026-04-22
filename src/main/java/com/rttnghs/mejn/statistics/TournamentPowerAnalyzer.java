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
import com.rttnghs.mejn.strategy.BaseStrategyFactory;
import com.rttnghs.mejn.strategy.StrategyFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Sequential tournament runner with adaptive early stopping.
 *
 * <h2>Design</h2>
 * <p>Batches are dispatched up to {@code concurrency} at a time using a
 * <em>sliding window</em> of {@link CompletableFuture}s.  As each batch
 * completes its result is fed into per-strategy {@link RunningStats} (Welford
 * online algorithm), and the stop condition is re-evaluated.  This leaves at
 * most {@code concurrency} batches in-flight simultaneously — matching the
 * number of remote compute servers you intend to use.
 *
 * <h2>RMI seam</h2>
 * <p>{@link #submitBatch()} is the single extension point for remote dispatch.
 * Override it (or inject a {@code BatchDispatcher} functional interface) to
 * send a batch to a remote server via RMI instead of running locally.  The
 * result type — a {@code Map<String, Double>} of normalized scores per
 * strategy — stays identical.
 *
 * <h2>Stop condition</h2>
 * <p>After each completed batch the analyzer checks every adjacent pair of
 * strategies (ordered by current mean score).  A pair is <em>resolved</em>
 * when either:
 * <ul>
 *   <li>their mean difference is smaller than {@code mde} (practically
 *       equivalent — no need for more samples), or</li>
 *   <li>we have accumulated enough observations to detect that difference with
 *       95 % confidence <strong>and</strong> 80 % power — i.e.
 *       {@code n ≥ 2 × ((Z_α/2 + Z_β) × σ_pooled / Δ)²}.</li>
 * </ul>
 * <p>Sampling stops once <em>all</em> adjacent pairs are resolved, or the
 * optional {@code maxBatches} cap is hit.
 *
 * <h2>Batch size guidance</h2>
 * <p>For a local run 1 000–5 000 games/batch gives frequent variance updates
 * with low overhead.  For RMI dispatch, 5 000–10 000 games/batch amortises
 * the network round-trip while still updating the estimate every few seconds.
 */
public class TournamentPowerAnalyzer {

    private static final Logger logger = LogManager.getLogger(TournamentPowerAnalyzer.class);

    /**
     * Z for two-sided α = 0.05 (95 % confidence).
     */
    public static final double Z_ALPHA_2 = 1.96;

    /**
     * Z for β = 0.20 (80 % power).
     */
    public static final double Z_BETA = 0.841;

    /**
     * Combined Z factor: (Z_α/2 + Z_β)².
     */
    private static final double Z_FACTOR_SQ = Math.pow(Z_ALPHA_2 + Z_BETA, 2);

    // ── Configuration ─────────────────────────────────────────────────────────

    private final StrategyFactory strategyFactory;
    private final List<List<String>> brackets;
    private final int concurrency;
    private final int playerCount;


    /**
     * @param strategyFactory factory that resolves strategy names
     * @param brackets        list of bracket strategy-name lists
     * @param concurrency     maximum in-flight batches; set to the number of remote servers (1–N)
     */
    public TournamentPowerAnalyzer(StrategyFactory strategyFactory, List<List<String>> brackets, int concurrency) {
        if (strategyFactory == null) {
            throw new IllegalArgumentException("strategyFactory cannot be null");
        }
        Objects.requireNonNull(brackets, "brackets cannot be null");
        if (brackets.isEmpty()) throw new IllegalArgumentException("brackets must not be empty");
        if (concurrency < 1) throw new IllegalArgumentException("concurrency must be >= 1");

        this.strategyFactory = strategyFactory;
        this.brackets = brackets;
        this.concurrency = concurrency;
        this.playerCount = brackets.get(0).size();
    }

    public TournamentPowerAnalyzer(StrategyFactory strategyFactory, List<List<String>> brackets) {
        this(strategyFactory, brackets, Config.configuration.getInt("powerAnalyzerConcurrency", 4));
    }

    // ── Running statistics ─────────────────────────────────────────────────────

    /**
     * Thread-safe online mean and variance tracker (Welford's algorithm).
     *
     * <p>All accessors are {@code synchronized} so results can safely be read
     * from the main thread while updates arrive from async batch completions.
     */
    public static final class RunningStats {

        private int n = 0;
        private double mean = 0.0;
        private double m2 = 0.0; // running sum of squared deviations

        /**
         * Incorporate a new observation.
         */
        public synchronized void update(double value) {
            n++;
            double delta = value - mean;
            mean += delta / n;
            double delta2 = value - mean;
            m2 += delta * delta2;
        }

        public synchronized int count() {
            return n;
        }

        public synchronized double mean() {
            return mean;
        }

        /**
         * Sample variance (Bessel-corrected). Returns 0 if fewer than 2 observations.
         */
        public synchronized double variance() {
            return n < 2 ? 0.0 : m2 / (n - 1);
        }

        public synchronized double stddev() {
            return Math.sqrt(variance());
        }

        public synchronized double stderr() {
            return n == 0 ? 0.0 : stddev() / Math.sqrt(n);
        }

        public synchronized double moe95() {
            return Z_ALPHA_2 * stderr();
        }

        /**
         * Lower bound of the 95 % CI.
         */
        public synchronized double ciLow() {
            return mean - moe95();
        }

        /**
         * Upper bound of the 95 % CI.
         */
        public synchronized double ciHigh() {
            return mean + moe95();
        }
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
        double z = Math.sqrt(n / 2.0) * delta / pooledStdDev - Z_ALPHA_2;
        // Φ(z) approximation using a logistic sigmoid (accurate to ~0.5%)
        return 1.0 / (1.0 + Math.exp(-1.7 * z));
    }

    // ── Batch submission (RMI seam) ────────────────────────────────────────────

    /**
     * Submit one batch of tournaments and return a future that resolves to the
     * normalized score per strategy.
     *
     * <p><strong>RMI hook</strong>: override or replace this method to dispatch
     * the batch to a remote compute server.  The contract is: the future must
     * eventually resolve to a {@code Map<String, Double>} where each value is
     * the normalized mean score for that strategy over {@code gamesPerBatch}
     * games (same scale as {@link com.rttnghs.mejn.statistics.TournamentStatistics}).
     *
     * @return future resolving to per-strategy normalized scores for this batch
     */
    protected CompletableFuture<Map<String, Double>> submitBatch() {
        return CompletableFuture.supplyAsync(() -> {
            EventCounter<String, Integer> batchCounts = new EventCounter<>();
            Function<Integer, Integer> scorer = pos -> Score.get(pos, playerCount);

            List<CompletableFuture<EventCounter<String, Integer>>> bracketFutures = new ArrayList<>(brackets.size());
            for (List<String> bracket : brackets) {
                Tournament t = new Tournament(strategyFactory, bracket, Config.configuration.getInt("powerAnalyzerGamesPerBatch", Config.configuration.getInt("games")));
                bracketFutures.add(CompletableFuture.supplyAsync(t::play));
            }
            for (CompletableFuture<EventCounter<String, Integer>> f : bracketFutures) {
                try {
                    batchCounts.add(f.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Batch bracket interrupted", e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("Batch bracket failed", e);
                }
            }
            Map<String, Integer> intScores = EventCounter.getNormalizedScores(batchCounts, scorer, 100);
            Map<String, Double> doubleScores = new TreeMap<>();
            intScores.forEach((k, v) -> doubleScores.put(k, v.doubleValue()));
            return doubleScores;
        });
    }

    // ── Main run loop ──────────────────────────────────────────────────────────

    /**
     * Run batches with adaptive early stopping.
     *
     * <p>Up to {@code concurrency} batches are in-flight simultaneously (the
     * sliding-window pattern).  After each batch completes its scores are
     * incorporated into the running statistics and — once the warmup is done —
     * the stop condition is evaluated.
     *
     * @return the final {@link Result}
     * @throws InterruptedException if interrupted while waiting for a batch
     * @throws ExecutionException   if a batch future throws
     */
    public Result run() throws InterruptedException, ExecutionException {
        Instant start = Instant.now();

        Map<String, RunningStats> statsMap = new TreeMap<>();
        int completedBatches = 0;
        int batchesStarted = 0;
        int batchesCanceled = 0;
        StopReason stopReason = StopReason.MAX_BATCHES;
        // For total games calculation
        EventCounter<String, Integer> totalPlacements = new EventCounter<>();

        // ── Sliding-window dispatch ──────────────────────────────────────────
        record InFlight(int batchIndex, CompletableFuture<Map<String, Double>> future) {}
        Set<InFlight> inFlight = new HashSet<>();

        // Seed the window: submit up to concurrency batches immediately.
        int submitted = 0;
        int initialSeed = Math.min(concurrency, Config.configuration.getInt("powerAnalyzerWarmupBatches", 5) + concurrency);
        while (submitted < initialSeed && submitted < Config.configuration.getInt("powerAnalyzerMaxBatches", 200)) {
            inFlight.add(new InFlight(submitted, submitBatch()));
            submitted++;
            batchesStarted++;
        }

        while (!inFlight.isEmpty()) {
            CompletableFuture<?>[] futures = inFlight.stream().map(InFlight::future).toArray(CompletableFuture[]::new);
            try {
                CompletableFuture.anyOf(futures).get(Config.configuration.getInt("tournamentPowerAnalyzer.batchTimeoutSeconds", 15), java.util.concurrent.TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                if (submitted < Config.configuration.getInt("powerAnalyzerMaxBatches", 200) && inFlight.size() < concurrency * 2) {
                    logger.debug("Batches are taking long; speculatively dispatching batch {}", submitted);
                    inFlight.add(new InFlight(submitted, submitBatch()));
                    submitted++;
                    batchesStarted++;
                }
                continue;
            }

            List<InFlight> done = inFlight.stream().filter(f -> f.future().isDone()).toList();
            for (InFlight head : done) {
                inFlight.remove(head);
                Map<String, Double> batchScores;
                try {
                    batchScores = head.future().get();
                } catch (Exception e) {
                    logger.warn("Batch {} failed: {}", head.batchIndex(), e.getMessage());
                    batchesCanceled++;
                    continue;
                }
                completedBatches++;

                // Update running stats.
                batchScores.forEach((strategy, score) -> statsMap.computeIfAbsent(strategy, _ -> new RunningStats()).update(score));

                // For total games: try to get placements from batchScores if possible (not available here, so skip for now)

                logger.debug("Batch {} complete: {}", completedBatches, formatScores(statsMap));

                if (completedBatches % 5 == 0) {
                    logger.info("After {} batches ({} games): {}", completedBatches, (long) completedBatches * Config.configuration.getInt("powerAnalyzerGamesPerBatch", Config.configuration.getInt("games")), formatPowerSummary(statsMap, completedBatches));
                }

                if (completedBatches >= Config.configuration.getInt("powerAnalyzerWarmupBatches", 5)) {
                    StopEvaluation eval = evaluateStop(statsMap, completedBatches);
                    if (eval.shouldStop()) {
                        stopReason = eval.reason();
                        int numBrackets = brackets.size();
                        int gamesPerBatch = Config.configuration.getInt("powerAnalyzerGamesPerBatch", Config.configuration.getInt("games"));
                        int tournamentsRun = completedBatches * numBrackets;
                        long gamesRun = (long) tournamentsRun * gamesPerBatch;
                        logger.info("Early stop after {} batches ({} tournaments, {} games). Reason: {}", completedBatches, tournamentsRun, gamesRun, stopReason);
                        inFlight.forEach(f -> f.future().cancel(true));
                        batchesCanceled += inFlight.size();
                        inFlight.clear();
                        break;
                    }
                }
            }

            if (inFlight.isEmpty() && stopReason != StopReason.MAX_BATCHES) {
                break;
            }

            while (submitted < Config.configuration.getInt("powerAnalyzerMaxBatches", 200) && inFlight.size() < concurrency) {
                inFlight.add(new InFlight(submitted, submitBatch()));
                submitted++;
                batchesStarted++;
            }
        }

        Duration elapsed = Duration.between(start, Instant.now());
        int gamesPerBatch = Config.configuration.getInt("powerAnalyzerGamesPerBatch", Config.configuration.getInt("games"));
        int numBrackets = brackets.size();
        return new Result(completedBatches, gamesPerBatch, Collections.unmodifiableMap(statsMap), buildPairAnalyses(statsMap, completedBatches), stopReason, elapsed, batchesStarted, batchesCanceled, playerCount, numBrackets);
    }
    // ── Stop condition evaluation ──────────────────────────────────────────────

    private StopEvaluation evaluateStop(Map<String, RunningStats> statsMap, int n) {
        List<PairAnalysis> pairs = buildPairAnalyses(statsMap, n);

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
            double pooledStddev = Math.sqrt((sA.variance() + sB.variance()) / 2.0);

            boolean practicallyEqual = delta < Config.configuration.getDouble("powerAnalyzerMde", 2.0);
            int reqBatches = practicallyEqual ? 0 : requiredBatches(delta, pooledStddev);
            double power = observedPower(n, delta, pooledStddev);
            boolean resolved = practicallyEqual || (n >= reqBatches && power >= 0.80);

            pairs.add(new PairAnalysis(nameA, nameB, delta, pooledStddev, power, reqBatches, practicallyEqual, resolved));
        }
        return pairs;
    }

    // ── Formatting helpers ─────────────────────────────────────────────────────

    private static String formatScores(Map<String, RunningStats> statsMap) {
        StringBuilder sb = new StringBuilder();
        statsMap.entrySet().stream().sorted(Comparator.comparingDouble((Map.Entry<String, RunningStats> e) -> e.getValue().mean()).reversed()).forEach(e -> {
            RunningStats s = e.getValue();
            sb.append(String.format("%s=%.1f±%.1f ", e.getKey(), s.mean(), s.moe95()));
        });
        return sb.toString().strip();
    }

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
    public record PairAnalysis(String strategyA, String strategyB, double delta, double pooledStddev,
                               double observedPower, int requiredBatches, boolean practicallyEqual, boolean resolved) {
    }

    /**
     * Aggregated results from a full power-analyzer run.
     *
     * @param completedBatches total number of batches executed
     * @param gamesPerBatch    games per batch
     * @param runningStats     per-strategy running statistics
     * @param pairAnalyses     final power analysis for each adjacent strategy pair
     * @param stopReason       why the run terminated
     * @param elapsed          wall-clock time
     */
    public record Result(int completedBatches, int gamesPerBatch, Map<String, RunningStats> runningStats,
                         List<PairAnalysis> pairAnalyses, StopReason stopReason, Duration elapsed,
                         int batchesStarted, int batchesCanceled, int playerCount, int numBrackets) {
        /**
         * True total games played, calculated as the sum of all placements for all strategies divided by player count.
         */
        public long trueTotalGames() {
            // Actual number of games played: (batchesStarted - batchesCanceled) × numBrackets × gamesPerBatch
            return (long) (batchesStarted - batchesCanceled) * numBrackets * gamesPerBatch;
        }

        /**
         * Formatted summary table.
         */
        public String toSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%nTournamentPowerAnalyzer: %d batches × %,d games/batch × %d brackets  (total: %,d games)  stop=%s\n",
                    completedBatches, gamesPerBatch, numBrackets, trueTotalGames(), stopReason));
            sb.append(String.format("Batches started: %d, Batches canceled: %d\n", batchesStarted, batchesCanceled));
            sb.append(String.format("%-24s %7s %7s %7s %7s%n", "Strategy", "mean", "stddev", "stderr", "95%CI±"));
            sb.append("-".repeat(65)).append(System.lineSeparator());
            runningStats.entrySet().stream().sorted(Comparator.comparingDouble((Map.Entry<String, RunningStats> e) -> e.getValue().mean()).reversed()).forEach(e -> {
                RunningStats s = e.getValue();
                sb.append(String.format("%-24s %7.2f %7.2f %7.2f %7.2f%n", e.getKey(), s.mean(), s.stddev(), s.stderr(), s.moe95()));
            });
            sb.append(System.lineSeparator());
            sb.append(String.format("%-24s %-24s %6s %7s %7s %10s %10s%n", "Strategy A", "Strategy B", "Δ", "power", "n_need", "equiv?", "resolved?"));
            sb.append("-".repeat(100)).append(System.lineSeparator());
            pairAnalyses.forEach(p -> sb.append(String.format("%-24s %-24s %6.2f %6.0f%% %7d %10s %10s%n", p.strategyA(), p.strategyB(), p.delta(), p.observedPower() * 100, p.requiredBatches(), p.practicallyEqual() ? "YES" : "no", p.resolved() ? "YES" : "no")));
            sb.append(String.format("%nElapsed: %s%n", elapsed));
            return sb.toString();
        }
    }

    // ── Entry point ────────────────────────────────────────────────────────────

    /**
     * Run as a standalone program.  Configuration keys:
     * <ul>
     *   <li>{@code games} — games per batch (default from config)</li>
     *   <li>{@code powerAnalyzerWarmupBatches} — warmup rounds before stop check (default 5)</li>
     *   <li>{@code powerAnalyzerMaxBatches} — hard cap on total batches (default 200)</li>
     *   <li>{@code powerAnalyzerConcurrency} — in-flight batches / server count (default 4)</li>
     *   <li>{@code powerAnalyzerMde} — minimum detectable effect on score scale (default 2.0)</li>
     *   <li>{@code tournamentPowerAnalyzer.batchTimeoutSeconds} — maximum time to wait for a batch to complete, in seconds (default 15)</li>
     * </ul>
     */
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        List<String> strategyNames = Tournament.getStrategyNames();
        List<List<String>> brackets = Tournament.getStrategyNameBrackets(strategyNames);

        int concurrency = Config.configuration.getInt("powerAnalyzerConcurrency", 4);

        logger.info("Starting TournamentPowerAnalyzer: concurrency={}", concurrency);

        TournamentPowerAnalyzer analyzer = new TournamentPowerAnalyzer(new BaseStrategyFactory(), brackets, concurrency);

        Result result = analyzer.run();
        logger.info("{}", result.toSummary());
    }
}

