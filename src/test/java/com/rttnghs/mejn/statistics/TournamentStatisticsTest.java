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
import com.rttnghs.mejn.statistics.TournamentStatistics;
import com.rttnghs.mejn.statistics.TournamentStatistics.StrategyStats;
import com.rttnghs.mejn.strategy.BaseStrategyFactory;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.*;
class TournamentStatisticsTest {
    // ── mean ──────────────────────────────────────────────────────────────────
    @Test
    void testMeanKnownValues() {
        // {2,4,4,4,5,5,7,9} mean = 40/8 = 5
        List<Double> values = List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0);
        assertEquals(5.0, TournamentStatistics.mean(values), 1e-9);
    }
    @Test
    void testMeanSingleValue() {
        assertEquals(42.0, TournamentStatistics.mean(List.of(42.0)), 1e-9);
    }
    @Test
    void testMeanEmptyIsZero() {
        assertEquals(0.0, TournamentStatistics.mean(List.of()));
    }
    // ── sampleStdDev ─────────────────────────────────────────────────────────
    @Test
    void testSampleStdDevKnownValues() {
        // {2,4,4,4,5,5,7,9}: sumSq of (xi-5) = 9+1+1+1+0+0+4+16 = 32
        // sample stddev = sqrt(32/7)
        List<Double> values = List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0);
        double mean = TournamentStatistics.mean(values);
        assertEquals(Math.sqrt(32.0 / 7.0), TournamentStatistics.sampleStdDev(values, mean), 1e-9);
    }
    @Test
    void testSampleStdDevConstantIsZero() {
        List<Double> values = List.of(7.0, 7.0, 7.0);
        assertEquals(0.0, TournamentStatistics.sampleStdDev(values, 7.0), 1e-9);
    }
    @Test
    void testSampleStdDevSingleValueIsZero() {
        assertEquals(0.0, TournamentStatistics.sampleStdDev(List.of(5.0), 5.0));
    }
    // ── median ───────────────────────────────────────────────────────────────
    @Test
    void testMedianOddCount() {
        List<Double> values = List.of(3.0, 1.0, 5.0, 2.0, 4.0);
        assertEquals(3.0, TournamentStatistics.median(values), 1e-9);
    }
    @Test
    void testMedianEvenCount() {
        List<Double> values = List.of(1.0, 4.0, 2.0, 3.0);
        assertEquals(2.5, TournamentStatistics.median(values), 1e-9);
    }
    @Test
    void testMedianSingleValue() {
        assertEquals(99.0, TournamentStatistics.median(List.of(99.0)), 1e-9);
    }
    @Test
    void testMedianEmptyIsZero() {
        assertEquals(0.0, TournamentStatistics.median(List.of()));
    }
    // ── StrategyStats.compute ────────────────────────────────────────────────
    @Test
    void testStrategyStatsComputedDerivedFields() {
        List<Double> scores = List.of(58.0, 59.0, 60.0, 61.0, 62.0);
        List<Integer> dummy = List.of(10, 10, 10, 10, 10);
        StrategyStats ss = StrategyStats.compute("Test", scores, dummy, dummy, dummy, dummy);
        assertEquals("Test", ss.strategy());
        assertEquals(60.0, ss.mean(), 1e-9);
        // sumSq = (4+1+0+1+4)=10; sample stddev = sqrt(10/4) = sqrt(2.5)
        double expectedStddev = Math.sqrt(10.0 / 4.0);
        assertEquals(expectedStddev, ss.stddev(), 1e-9);
        assertEquals(expectedStddev / Math.sqrt(5), ss.stderr(), 1e-9);
        assertEquals(TournamentStatistics.Z_95 * ss.stderr(), ss.moe95(), 1e-9);
        assertEquals(58.0, ss.min(), 1e-9);
        assertEquals(62.0, ss.max(), 1e-9);
        assertEquals(60.0, ss.median(), 1e-9);
        assertEquals(expectedStddev / 60.0, ss.coefficientOfVariation(), 1e-9);
        assertEquals(10.0, ss.meanFirstPlaces(), 1e-9);
    }
    @Test
    void testStrategyStatsPlaceAverages() {
        List<Double> scores = List.of(60.0, 62.0, 58.0);
        StrategyStats ss = StrategyStats.compute("P",
                scores,
                List.of(30, 32, 28),   // mean 30
                List.of(25, 26, 24),   // mean 25
                List.of(25, 24, 26),   // mean 25
                List.of(20, 18, 22));  // mean 20
        assertEquals(30.0, ss.meanFirstPlaces(), 1e-9);
        assertEquals(25.0, ss.meanSecondPlaces(), 1e-9);
        assertEquals(25.0, ss.meanThirdPlaces(), 1e-9);
        assertEquals(20.0, ss.meanFourthPlaces(), 1e-9);
    }
    // ── roundsRequiredFor10PctRelativeMoe ────────────────────────────────────
    @Test
    void testRoundsRequiredFor10PctRelativeMoeMatchesFormula() {
        List<Double> scores = List.of(60.0, 62.0, 58.0, 61.0, 59.0);
        double mean = TournamentStatistics.mean(scores);
        double stddev = TournamentStatistics.sampleStdDev(scores, mean);
        int expected = (int) Math.ceil(Math.pow(TournamentStatistics.Z_95 * stddev / (0.10 * mean), 2));
        List<Integer> dummy = List.of(30, 32, 28, 31, 29);
        StrategyStats ss = StrategyStats.compute("S", scores, dummy, dummy, dummy, dummy);
        assertEquals(expected, ss.roundsRequiredFor10PctRelativeMoe());
    }
    @Test
    void testRoundsRequiredWhenStddevZeroReturnsSampleSize() {
        List<Double> scores = List.of(60.0, 60.0, 60.0, 60.0);
        List<Integer> dummy = List.of(15, 15, 15, 15);
        StrategyStats ss = StrategyStats.compute("S", scores, dummy, dummy, dummy, dummy);
        assertEquals(scores.size(), ss.roundsRequiredFor10PctRelativeMoe());
    }
    // ── additionalRoundsFor10PctRelativeMoe ──────────────────────────────────
    @Test
    void testAdditionalRoundsIsZeroWhenAlreadySufficient() {
        List<Double> constant = List.of(60.0, 60.0, 60.0, 60.0, 60.0);
        List<Integer> dummy = List.of(15, 15, 15, 15, 15);
        StrategyStats ss = StrategyStats.compute("S", constant, dummy, dummy, dummy, dummy);
        assertEquals(0, ss.additionalRoundsFor10PctRelativeMoe(ss.roundsRequiredFor10PctRelativeMoe()));
    }
    @Test
    void testAdditionalRoundsPositiveWhenMoreNeeded() {
        // high spread relative to mean → many rounds required
        List<Double> scores = List.of(60.0, 90.0);
        List<Integer> dummy = List.of(10, 10);
        StrategyStats ss = StrategyStats.compute("S", scores, dummy, dummy, dummy, dummy);
        int additional = ss.additionalRoundsFor10PctRelativeMoe(2);
        assertEquals(Math.max(0, ss.roundsRequiredFor10PctRelativeMoe() - 2), additional);
    }
    // ── roundsRequiredForAbsoluteMoe ─────────────────────────────────────────
    @Test
    void testRoundsRequiredForAbsoluteMoeMatchesFormula() {
        List<Double> scores = List.of(60.0, 62.0, 58.0, 61.0, 59.0);
        double mean = TournamentStatistics.mean(scores);
        double stddev = TournamentStatistics.sampleStdDev(scores, mean);
        double moe = 1.0;
        int expected = (int) Math.ceil(Math.pow(TournamentStatistics.Z_95 * stddev / moe, 2));
        List<Integer> dummy = List.of(30, 32, 28, 31, 29);
        StrategyStats ss = StrategyStats.compute("S", scores, dummy, dummy, dummy, dummy);
        assertEquals(expected, ss.roundsRequiredForAbsoluteMoe(moe));
    }
    @Test
    void testRoundsRequiredForAbsoluteMoeIsOneWhenStddevZero() {
        List<Double> scores = List.of(60.0, 60.0, 60.0);
        List<Integer> dummy = List.of(20, 20, 20);
        StrategyStats ss = StrategyStats.compute("S", scores, dummy, dummy, dummy, dummy);
        assertEquals(1, ss.roundsRequiredForAbsoluteMoe(5.0));
    }
    @Test
    void testRoundsRequiredForAbsoluteMoeThrowsOnNonPositiveMoe() {
        List<Double> scores = List.of(60.0, 62.0);
        List<Integer> dummy = List.of(10, 10);
        StrategyStats ss = StrategyStats.compute("S", scores, dummy, dummy, dummy, dummy);
        assertThrows(IllegalArgumentException.class, () -> ss.roundsRequiredForAbsoluteMoe(0.0));
        assertThrows(IllegalArgumentException.class, () -> ss.roundsRequiredForAbsoluteMoe(-1.0));
    }
    // ── constructor validation ────────────────────────────────────────────────
    @Test
    void testConstructorRejectsNullFactory() {
        List<List<String>> brackets = List.of(
                List.of("RandomStrategy", "FarStrategy", "RandomStrategy", "FarStrategy"));
        assertThrows(NullPointerException.class,
                () -> new TournamentStatistics(null, brackets, 10, 5));
    }
    @Test
    void testConstructorRejectsNullBrackets() {
        BaseStrategyFactory factory = new BaseStrategyFactory();
        assertThrows(NullPointerException.class,
                () -> new TournamentStatistics(factory, null, 10, 5));
    }
    @Test
    void testConstructorRejectsEmptyBrackets() {
        BaseStrategyFactory factory = new BaseStrategyFactory();
        assertThrows(IllegalArgumentException.class,
                () -> new TournamentStatistics(factory, List.of(), 10, 5));
    }
    @Test
    void testConstructorRejectsZeroGamesPerRound() {
        BaseStrategyFactory factory = new BaseStrategyFactory();
        List<List<String>> brackets = List.of(
                List.of("RandomStrategy", "FarStrategy", "RandomStrategy", "FarStrategy"));
        assertThrows(IllegalArgumentException.class,
                () -> new TournamentStatistics(factory, brackets, 0, 5));
    }
    @Test
    void testConstructorRejectsOneRound() {
        BaseStrategyFactory factory = new BaseStrategyFactory();
        List<List<String>> brackets = List.of(
                List.of("RandomStrategy", "FarStrategy", "RandomStrategy", "FarStrategy"));
        assertThrows(IllegalArgumentException.class,
                () -> new TournamentStatistics(factory, brackets, 10, 1));
    }
    // ── integration smoke test ────────────────────────────────────────────────
    /**
     * End-to-end: 3 rounds × 10 games (test properties set games=10 but we
     * pass gamesPerRound=10 explicitly to match that).  Validates shape of
     * the Result without asserting specific numeric outcomes (those are
     * non-deterministic).
     */
    @Test
    void testRunProducesWellFormedResult() throws InterruptedException, ExecutionException {
        List<String> strategyNames = Tournament.getStrategyNames();
        List<List<String>> brackets = Tournament.getStrategyNameBrackets(strategyNames);
        TournamentStatistics ts = new TournamentStatistics(
                new BaseStrategyFactory(), brackets, 10, 3);
        TournamentStatistics.Result result = ts.run();
        assertEquals(3, result.rounds());
        assertEquals(10, result.gamesPerRound());
        assertFalse(result.strategyStats().isEmpty(), "strategyStats should not be empty");
        // Every strategy must have exactly 3 per-round score entries.
        for (var entry : result.scoresByStrategy().entrySet()) {
            assertEquals(3, entry.getValue().size(),
                    "Expected 3 per-round scores for " + entry.getKey());
        }
        // Each StrategyStats record must be numerically valid.
        for (StrategyStats ss : result.strategyStats().values()) {
            assertTrue(Double.isFinite(ss.mean()), "mean should be finite");
            assertTrue(ss.stddev() >= 0.0, "stddev should be >= 0");
            assertTrue(ss.moe95() >= 0.0, "moe95 should be >= 0");
            assertTrue(ss.min() <= ss.max(), "min <= max");
            assertTrue(ss.roundsRequiredFor10PctRelativeMoe() > 0, "roundsRequired should be > 0");
        }
        // toSummary() must produce a multi-line string containing at least one strategy name.
        String summary = result.toSummary();
        assertFalse(summary.isBlank());
        assertTrue(summary.lines().count() > 4, "summary should have multiple lines");
        for (String strategy : result.strategyStats().keySet()) {
            assertTrue(summary.contains(strategy), "summary should mention " + strategy);
        }
    }
}
