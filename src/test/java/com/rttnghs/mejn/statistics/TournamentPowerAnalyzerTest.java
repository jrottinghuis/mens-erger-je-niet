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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TournamentPowerAnalyzerTest {

    // RunningStats

    @Test
    void runningStats_singleObservation_hasZeroVariance() {
        TournamentPowerAnalyzer.RunningStats stats = new TournamentPowerAnalyzer.RunningStats();
        stats.update(42.0);
        assertEquals(1, stats.count());
        assertEquals(42.0, stats.mean(), 1e-9);
        assertEquals(0.0, stats.variance());
        assertEquals(0.0, stats.stddev());
    }

    @Test
    void runningStats_knownValues_matchDirectCalculation() {
        // values: 10, 20, 30 -> mean=20, sample variance=100, stddev=10
        TournamentPowerAnalyzer.RunningStats stats = new TournamentPowerAnalyzer.RunningStats();
        stats.update(10.0);
        stats.update(20.0);
        stats.update(30.0);

        assertEquals(3, stats.count());
        assertEquals(20.0, stats.mean(), 1e-9);
        assertEquals(100.0, stats.variance(), 1e-6);
        assertEquals(10.0, stats.stddev(), 1e-6);
    }

    @Test
    void runningStats_moe95_isZ96TimesStderr() {
        TournamentPowerAnalyzer.RunningStats stats = new TournamentPowerAnalyzer.RunningStats();
        stats.update(10.0);
        stats.update(20.0);
        stats.update(30.0);

        double expectedStderr = stats.stddev() / Math.sqrt(3);
        assertEquals(TournamentPowerAnalyzer.Z_ALPHA_2 * expectedStderr, stats.moe95(), 1e-9);
    }

    @Test
    void runningStats_ciLowCiHigh_bracketsTheMean() {
        TournamentPowerAnalyzer.RunningStats stats = new TournamentPowerAnalyzer.RunningStats();
        for (double v = 1; v <= 10; v++) stats.update(v);

        assertTrue(stats.ciLow() < stats.mean());
        assertTrue(stats.ciHigh() > stats.mean());
        assertEquals(stats.mean() - stats.moe95(), stats.ciLow(), 1e-9);
        assertEquals(stats.mean() + stats.moe95(), stats.ciHigh(), 1e-9);
    }

    // requiredBatches

    @Test
    void requiredBatches_zeroDelta_returnsOne() {
        assertEquals(1, TournamentPowerAnalyzer.requiredBatches(0.0, 10.0));
    }

    @Test
    void requiredBatches_zeroStddev_returnsOne() {
        assertEquals(1, TournamentPowerAnalyzer.requiredBatches(5.0, 0.0));
    }

    @Test
    void requiredBatches_largeEffect_requiresFewBatches() {
        int n = TournamentPowerAnalyzer.requiredBatches(50.0, 5.0);
        assertTrue(n > 0);
        assertTrue(n < 10, "Large effect should need < 10 batches, got " + n);
    }

    @Test
    void requiredBatches_smallEffect_requiresManyBatches() {
        int n = TournamentPowerAnalyzer.requiredBatches(1.0, 10.0);
        assertTrue(n > 100, "Small effect should need many batches, got " + n);
    }

    @Test
    void requiredBatches_formula_matchesExpected() {
        double sigma = 5.0;
        double delta = 2.0;
        double expected = Math.ceil(2.0
                * Math.pow(TournamentPowerAnalyzer.Z_ALPHA_2 + TournamentPowerAnalyzer.Z_BETA, 2)
                * sigma * sigma / (delta * delta));
        assertEquals((int) expected, TournamentPowerAnalyzer.requiredBatches(delta, sigma));
    }

    // observedPower

    @Test
    void observedPower_insufficientSamples_lowPower() {
        double power = TournamentPowerAnalyzer.observedPower(2, 1.0, 10.0);
        assertTrue(power < 0.5, "Expected low power for tiny n, got " + power);
    }

    @Test
    void observedPower_atRequiredN_nearEightyPercent() {
        double sigma = 5.0;
        double delta = 2.0;
        int n = TournamentPowerAnalyzer.requiredBatches(delta, sigma);
        double power = TournamentPowerAnalyzer.observedPower(n, delta, sigma);
        // Sigmoid approximation: allow +/-5% tolerance around 80%.
        assertTrue(power >= 0.75 && power <= 0.90,
                "Expected power ~80%% at required n=" + n + ", got " + power);
    }

    @Test
    void observedPower_zeroDelta_returnsZero() {
        assertEquals(0.0, TournamentPowerAnalyzer.observedPower(100, 0.0, 5.0));
    }

    @Test
    void observedPower_zeroStddev_returnsZero() {
        assertEquals(0.0, TournamentPowerAnalyzer.observedPower(100, 5.0, 0.0));
    }

    // Constructor validation

    @Test
    void constructor_invalidGamesPerBatch_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new TournamentPowerAnalyzer(
                        new com.rttnghs.mejn.strategy.BaseStrategyFactory(),
                        java.util.List.of(java.util.List.of("A")),
                        0, 5, 200, 4, 2.0));
    }

    @Test
    void constructor_warmupLessThanTwo_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new TournamentPowerAnalyzer(
                        new com.rttnghs.mejn.strategy.BaseStrategyFactory(),
                        java.util.List.of(java.util.List.of("A")),
                        100, 1, 200, 4, 2.0));
    }

    @Test
    void constructor_maxBatchesLessThanWarmup_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new TournamentPowerAnalyzer(
                        new com.rttnghs.mejn.strategy.BaseStrategyFactory(),
                        java.util.List.of(java.util.List.of("A")),
                        100, 5, 3, 4, 2.0));
    }

    @Test
    void constructor_negativeMde_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new TournamentPowerAnalyzer(
                        new com.rttnghs.mejn.strategy.BaseStrategyFactory(),
                        java.util.List.of(java.util.List.of("A")),
                        100, 5, 200, 4, -1.0));
    }
}
