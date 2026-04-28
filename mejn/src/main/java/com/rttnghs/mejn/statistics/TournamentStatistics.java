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
 * Runs repeated tournament rounds to estimate score stability and confidence
 * intervals per strategy.
 *
 * <p>A single <em>round</em> is one complete tournament: all configured brackets
 * are played (in parallel) for {@code gamesPerRound} games each, and the
 * resulting finish counts are aggregated into a single normalized score per
 * strategy, exactly as {@link Tournament#main} does.  Repeating this N times
 * provides enough samples to compute mean, standard deviation, and 95 %
 * confidence intervals, and to estimate how many additional rounds are needed
 * to reach a desired accuracy.
 */
public class TournamentStatistics {

    private static final Logger logger = LogManager.getLogger(TournamentStatistics.class);

    /**
     * Z-value for the 95 % confidence interval.
     */
    public static final double Z_95 = 1.96;

    private final StrategyFactory strategyFactory;
    private final List<List<String>> brackets;
    private final int gamesPerRound;
    private final int rounds;
    /**
     * Number of players per game — assumed the same across all brackets.
     */
    private final int playerCount;

    /**
     * @param strategyFactory factory that resolves strategy names to {@link com.rttnghs.mejn.strategy.Strategy} instances
     * @param brackets        list of bracket strategy-name lists, as returned by {@link Tournament#getStrategyNameBrackets}
     * @param gamesPerRound   number of games each bracket plays per round; must be ≥ 1
     * @param rounds          number of rounds to run; must be ≥ 2 (fewer rounds produce no meaningful stddev)
     */
    public TournamentStatistics(StrategyFactory strategyFactory, List<List<String>> brackets, int gamesPerRound, int rounds) {
        Objects.requireNonNull(strategyFactory, "strategyFactory cannot be null");
        Objects.requireNonNull(brackets, "brackets cannot be null");
        if (brackets.isEmpty()) {
            throw new IllegalArgumentException("brackets must not be empty");
        }
        if (gamesPerRound < 1) {
            throw new IllegalArgumentException("gamesPerRound must be >= 1");
        }
        if (rounds < 2) {
            throw new IllegalArgumentException("rounds must be >= 2 for meaningful statistics");
        }
        this.strategyFactory = strategyFactory;
        this.brackets = brackets;
        this.gamesPerRound = gamesPerRound;
        this.rounds = rounds;
        // Assume all brackets have the same number of players (matches Tournament.main).
        this.playerCount = brackets.getFirst().size();
    }

    /**
     * Execute all rounds.  Each round runs all brackets in parallel (via
     * {@link CompletableFuture#supplyAsync}) and records the resulting
     * per-strategy normalized score and place counts.
     *
     * @return aggregated {@link Result} containing per-strategy statistics and the raw per-round scores
     * @throws InterruptedException if any bracket future is interrupted
     * @throws ExecutionException   if any bracket future throws
     */
    public Result run() throws InterruptedException, ExecutionException {
        Instant start = Instant.now();

        Function<Integer, Integer> scorer = finishPosition -> Score.get(finishPosition, playerCount);

        // per-strategy lists, one element per round
        Map<String, List<Double>> scoresByStrategy = new TreeMap<>();
        Map<String, List<Integer>> firstsByStrategy = new TreeMap<>();
        Map<String, List<Integer>> secondsByStrategy = new TreeMap<>();
        Map<String, List<Integer>> thirdsByStrategy = new TreeMap<>();
        Map<String, List<Integer>> fourthsByStrategy = new TreeMap<>();

        for (int round = 0; round < rounds; round++) {
            EventCounter<String, Integer> roundFinishCounts = new EventCounter<>();

            // Run all brackets concurrently (same pattern as Tournament.main).
            List<CompletableFuture<EventCounter<String, Integer>>> futures = new ArrayList<>(brackets.size());
            for (List<String> bracket : brackets) {
                Tournament tournament = new Tournament(strategyFactory, bracket, gamesPerRound);
                futures.add(CompletableFuture.supplyAsync(tournament::play));
            }
            for (CompletableFuture<EventCounter<String, Integer>> future : futures) {
                roundFinishCounts.add(future.get());
            }

            // Normalized score per strategy for this round (accuracy=100, matching Tournament.main).
            Map<String, Integer> roundScores = EventCounter.getNormalizedScores(roundFinishCounts, scorer, 100);

            for (String strategy : roundScores.keySet()) {
                scoresByStrategy.computeIfAbsent(strategy, _ -> new ArrayList<>()).add(roundScores.get(strategy).doubleValue());
                firstsByStrategy.computeIfAbsent(strategy, _ -> new ArrayList<>()).add(roundFinishCounts.getCount(strategy, 0));
                secondsByStrategy.computeIfAbsent(strategy, _ -> new ArrayList<>()).add(roundFinishCounts.getCount(strategy, 1));
                thirdsByStrategy.computeIfAbsent(strategy, _ -> new ArrayList<>()).add(roundFinishCounts.getCount(strategy, 2));
                fourthsByStrategy.computeIfAbsent(strategy, _ -> new ArrayList<>()).add(roundFinishCounts.getCount(strategy, 3));
            }

            if ((round + 1) % 10 == 0) {
                logger.info("Completed round {}/{}", round + 1, rounds);
            }
        }

        // Build summary statistics per strategy.
        Map<String, StrategyStats> stats = new TreeMap<>();
        for (String strategy : scoresByStrategy.keySet()) {
            stats.put(strategy, StrategyStats.compute(strategy, scoresByStrategy.get(strategy), firstsByStrategy.getOrDefault(strategy, List.of()), secondsByStrategy.getOrDefault(strategy, List.of()), thirdsByStrategy.getOrDefault(strategy, List.of()), fourthsByStrategy.getOrDefault(strategy, List.of())));
        }

        Duration elapsed = Duration.between(start, Instant.now());
        return new Result(rounds, gamesPerRound, Collections.unmodifiableMap(scoresByStrategy), stats, elapsed);
    }

    // ── Records ────────────────────────────────────────────────────────────────

    /**
     * Aggregated results across all rounds.
     *
     * @param rounds           number of rounds that were run
     * @param gamesPerRound    games per round
     * @param scoresByStrategy raw per-round normalized scores per strategy
     * @param strategyStats    computed summary statistics per strategy
     * @param elapsed          wall-clock time for the entire run
     */
    public record Result(int rounds, int gamesPerRound, Map<String, List<Double>> scoresByStrategy,
                         Map<String, StrategyStats> strategyStats, Duration elapsed) {
        /**
         * Formatted summary table, one line per strategy, sorted by mean score descending.
         */
        public String toSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%nTournamentStatistics: %d rounds × %,d games/round  (total games: %,d)%n", rounds, gamesPerRound, (long) rounds * gamesPerRound));
            sb.append(String.format("%-24s %7s %7s %7s %7s %7s %7s %7s  %7s  %8s%n", "Strategy", "mean", "stddev", "stderr", "±95%CI", "min", "max", "median", "CoV%", "n@10%rMOE"));
            sb.append("-".repeat(105)).append(System.lineSeparator());
            strategyStats.values().stream().sorted(Comparator.comparingDouble(StrategyStats::mean).reversed()).forEach(ss -> sb.append(String.format("%-24s %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f  %6.1f%%  %8d%n", ss.strategy(), ss.mean(), ss.stddev(), ss.stderr(), ss.moe95(), ss.min(), ss.max(), ss.median(), ss.coefficientOfVariation() * 100.0, ss.roundsRequiredFor10PctRelativeMoe())));
            sb.append("-".repeat(105)).append(System.lineSeparator());
            sb.append(String.format("%-24s %7s %7s %7s %7s%n", "Strategy", "1st avg", "2nd avg", "3rd avg", "4th avg"));
            strategyStats.values().stream().sorted(Comparator.comparingDouble(StrategyStats::mean).reversed()).forEach(ss -> sb.append(String.format("%-24s %7.1f %7.1f %7.1f %7.1f%n", ss.strategy(), ss.meanFirstPlaces(), ss.meanSecondPlaces(), ss.meanThirdPlaces(), ss.meanFourthPlaces())));
            sb.append(String.format("%nElapsed: %s%n", elapsed));
            return sb.toString();
        }
    }

    /**
     * Summary statistics for a single strategy across all rounds.
     *
     * @param strategy                          strategy name
     * @param mean                              mean normalized score across rounds
     * @param stddev                            sample standard deviation of scores
     * @param stderr                            standard error = stddev / sqrt(n)
     * @param moe95                             95 % margin of error = 1.96 × stderr
     * @param min                               minimum score observed across rounds
     * @param max                               maximum score observed across rounds
     * @param median                            median score across rounds
     * @param coefficientOfVariation            stddev / mean (dimensionless stability measure)
     * @param roundsRequiredFor10PctRelativeMoe total rounds needed for ±10 % relative MOE at 95 %
     * @param meanFirstPlaces                   mean 1st-place count per round
     * @param meanSecondPlaces                  mean 2nd-place count per round
     * @param meanThirdPlaces                   mean 3rd-place count per round
     * @param meanFourthPlaces                  mean 4th-place count per round
     */
    public record StrategyStats(String strategy, double mean, double stddev, double stderr, double moe95, double min,
                                double max, double median, double coefficientOfVariation,
                                int roundsRequiredFor10PctRelativeMoe, double meanFirstPlaces, double meanSecondPlaces,
                                double meanThirdPlaces, double meanFourthPlaces) {
        static StrategyStats compute(String strategy, List<Double> scores, List<Integer> firsts, List<Integer> seconds, List<Integer> thirds, List<Integer> fourths) {

            double mean = TournamentStatistics.mean(scores);
            double stddev = TournamentStatistics.sampleStdDev(scores, mean);
            double stderr = (scores.isEmpty()) ? 0.0 : stddev / Math.sqrt(scores.size());
            double moe95 = Z_95 * stderr;
            double min = scores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double max = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double medianScore = TournamentStatistics.median(scores);
            // Coefficient of variation: how much does the score vary relative to the mean?
            double cov = (mean == 0.0) ? 0.0 : stddev / mean;
            // n_required = ceil((z * s / (0.10 * mean))^2) — relative 10 % of mean
            int nRequired = (mean == 0.0 || stddev == 0.0) ? scores.size() : (int) Math.ceil(Math.pow(Z_95 * stddev / (0.10 * mean), 2));

            return new StrategyStats(strategy, mean, stddev, stderr, moe95, min, max, medianScore, cov, nRequired, TournamentStatistics.mean(toDoubles(firsts)), TournamentStatistics.mean(toDoubles(seconds)), TournamentStatistics.mean(toDoubles(thirds)), TournamentStatistics.mean(toDoubles(fourths)));
        }

        /**
         * How many <em>additional</em> rounds need to be run (beyond the ones already
         * completed) to reach ±10 % relative margin of error at 95 % confidence.
         *
         * @param alreadyRun rounds already completed
         * @return additional rounds needed, or 0 if already sufficient
         */
        public int additionalRoundsFor10PctRelativeMoe(int alreadyRun) {
            return Math.max(0, roundsRequiredFor10PctRelativeMoe - alreadyRun);
        }

        /**
         * Total rounds required to reach an absolute margin of error at 95 % confidence.
         *
         * @param desiredMoe absolute margin of error on the score scale; must be &gt; 0
         * @return required total rounds
         */
        public int roundsRequiredForAbsoluteMoe(double desiredMoe) {
            if (desiredMoe <= 0) {
                throw new IllegalArgumentException("desiredMoe must be > 0");
            }
            if (stddev == 0.0) {
                return 1;
            }
            return (int) Math.ceil(Math.pow(Z_95 * stddev / desiredMoe, 2));
        }
    }

    // ── Pure-statistic helpers (package-private for testing) ──────────────────

    static double mean(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    static double sampleStdDev(List<Double> values, double mean) {
        if (values.size() < 2) {
            return 0.0;
        }
        double sumSq = 0.0;
        for (double v : values) {
            double d = v - mean;
            sumSq += d * d;
        }
        return Math.sqrt(sumSq / (values.size() - 1));
    }

    static double median(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int mid = sorted.size() / 2;
        return ((sorted.size() & 1) == 0) ? (sorted.get(mid - 1) + sorted.get(mid)) / 2.0 : sorted.get(mid);
    }

    private static List<Double> toDoubles(List<Integer> ints) {
        return ints.stream().map(Integer::doubleValue).toList();
    }

    // ── Entry point ────────────────────────────────────────────────────────────

    /**
     * Run the tournament statistics as a standalone program.  Configuration is
     * read from the standard MEJN configuration chain (mejn-config.xml and
     * associated property files).  The number of rounds is taken from the
     * {@code tournamentRounds} property; the number of games per round from the
     * {@code games} property.
     */
    static void main(String[] args) throws InterruptedException, ExecutionException {
        List<String> strategyNames = Tournament.getStrategyNames();
        List<List<String>> brackets = Tournament.getStrategyNameBrackets(strategyNames);
        int gamesPerRound = Config.configuration.getInt("games");
        int rounds = Config.configuration.getInt("tournamentRounds");

        logger.info("Starting TournamentStatistics: {} rounds × {} games/round, {} brackets", rounds, gamesPerRound, brackets.size());

        TournamentStatistics ts = new TournamentStatistics(new BaseStrategyFactory(), brackets, gamesPerRound, rounds);

        Result result = ts.run();
        logger.info("{}", result.toSummary());
    }
}


