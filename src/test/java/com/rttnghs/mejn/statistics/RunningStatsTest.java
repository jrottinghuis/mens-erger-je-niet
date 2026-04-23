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

class RunningStatsTest {

    /**
     * Verifies that a single observation produces zero variance and a stable mean.
     */
    @Test
    void singleObservation_hasZeroVariance() {
        RunningStats stats = new RunningStats();
        stats.update(42.0);
        assertEquals(1, stats.count());
        assertEquals(42.0, stats.mean(), 1e-9);
        assertEquals(0.0, stats.variance());
        assertEquals(0.0, stats.stddev());
    }

    /**
     * Verifies Welford's running statistics match a known three-value dataset.
     */
    @Test
    void knownValues_matchDirectCalculation() {
        // values: 10, 20, 30 -> mean=20, sample variance=100, stddev=10
        RunningStats stats = new RunningStats();
        stats.update(10.0);
        stats.update(20.0);
        stats.update(30.0);

        assertEquals(3, stats.count());
        assertEquals(20.0, stats.mean(), 1e-9);
        assertEquals(100.0, stats.variance(), 1e-6);
        assertEquals(10.0, stats.stddev(), 1e-6);
    }

    /**
     * Verifies the 95% margin of error equals Z_ALPHA_2 × stderr.
     */
    @Test
    void moe95_isZAlpha2TimesStderr() {
        RunningStats stats = new RunningStats();
        stats.update(10.0);
        stats.update(20.0);
        stats.update(30.0);

        double expectedStderr = stats.stddev() / Math.sqrt(3);
        assertEquals(RunningStats.Z_ALPHA_2 * expectedStderr, stats.moe95(), 1e-9);
    }

    /**
     * Verifies the confidence interval bounds the mean symmetrically.
     */
    @Test
    void ciLowCiHigh_bracketsTheMeanSymmetrically() {
        RunningStats stats = new RunningStats();
        for (double v = 1; v <= 10; v++) stats.update(v);

        assertTrue(stats.ciLow() < stats.mean());
        assertTrue(stats.ciHigh() > stats.mean());
        assertEquals(stats.mean() - stats.moe95(), stats.ciLow(), 1e-9);
        assertEquals(stats.mean() + stats.moe95(), stats.ciHigh(), 1e-9);
    }

    /**
     * Verifies that zero observations yield zero stderr and zero moe95.
     */
    @Test
    void noObservations_zeroStderrAndMoe() {
        RunningStats stats = new RunningStats();
        assertEquals(0, stats.count());
        assertEquals(0.0, stats.stderr());
        assertEquals(0.0, stats.moe95());
    }

    /**
     * Verifies that two identical observations yield zero variance.
     */
    @Test
    void identicalObservations_zeroVariance() {
        RunningStats stats = new RunningStats();
        stats.update(5.0);
        stats.update(5.0);
        assertEquals(0.0, stats.variance(), 1e-12);
        assertEquals(5.0, stats.mean(), 1e-9);
    }
}

