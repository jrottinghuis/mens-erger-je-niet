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

import com.rttnghs.mejn.configuration.Config;
import com.rttnghs.mejn.rmi.LocalComputeServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TournamentPowerAnalyzerTest {

    // requiredBatches

    /**
     * Verifies zero delta yields the minimum batch requirement.
     */
    @Test
    void requiredBatches_zeroDelta_returnsOne() {
        assertEquals(1, TournamentPowerAnalyzer.requiredBatches(0.0, 10.0));
    }

    /**
     * Verifies zero standard deviation yields the minimum batch requirement.
     */
    @Test
    void requiredBatches_zeroStddev_returnsOne() {
        assertEquals(1, TournamentPowerAnalyzer.requiredBatches(5.0, 0.0));
    }

    /**
     * Verifies a large effect size should require only a small number of batches.
     */
    @Test
    void requiredBatches_largeEffect_requiresFewBatches() {
        int n = TournamentPowerAnalyzer.requiredBatches(50.0, 5.0);
        assertTrue(n > 0);
        assertTrue(n < 10, "Large effect should need < 10 batches, got " + n);
    }

    /**
     * Verifies a small effect size requires substantially more batches.
     */
    @Test
    void requiredBatches_smallEffect_requiresManyBatches() {
        int n = TournamentPowerAnalyzer.requiredBatches(1.0, 10.0);
        assertTrue(n > 100, "Small effect should need many batches, got " + n);
    }

    /**
     * Verifies the required-batch formula matches the expected power-analysis equation.
     */
    @Test
    void requiredBatches_formula_matchesExpected() {
        double sigma = 5.0;
        double delta = 2.0;
        double expected = Math.ceil(2.0 * Math.pow(RunningStats.Z_ALPHA_2 + TournamentPowerAnalyzer.Z_BETA, 2) * sigma * sigma / (delta * delta));
        assertEquals((int) expected, TournamentPowerAnalyzer.requiredBatches(delta, sigma));
    }

    // observedPower

    /**
     * Verifies that a tiny sample with a small effect has low observed power.
     */
    @Test
    void observedPower_insufficientSamples_lowPower() {
        double power = TournamentPowerAnalyzer.observedPower(2, 1.0, 10.0);
        assertTrue(power < 0.5, "Expected low power for tiny n, got " + power);
    }

    /**
     * Verifies observed power near the required sample size is roughly 80%.
     */
    @Test
    void observedPower_atRequiredN_nearEightyPercent() {
        double sigma = 5.0;
        double delta = 2.0;
        int n = TournamentPowerAnalyzer.requiredBatches(delta, sigma);
        double power = TournamentPowerAnalyzer.observedPower(n, delta, sigma);
        // Sigmoid approximation: allow +/-5% tolerance around 80%.
        assertTrue(power >= 0.75 && power <= 0.90, "Expected power ~80%% at required n=" + n + ", got " + power);
    }

    /**
     * Verifies zero delta produces zero observed power.
     */
    @Test
    void observedPower_zeroDelta_returnsZero() {
        assertEquals(0.0, TournamentPowerAnalyzer.observedPower(100, 0.0, 5.0));
    }

    /**
     * Verifies zero standard deviation produces zero observed power.
     */
    @Test
    void observedPower_zeroStddev_returnsZero() {
        assertEquals(0.0, TournamentPowerAnalyzer.observedPower(100, 5.0, 0.0));
    }

    // Constructor validation
    /**
     * Verifies that the constructor throws when the servers list is null.
     */
    @Test
    void constructor_nullServers_throwsException() {
        List<List<String>> validBrackets = List.of(List.of("A", "B"));
        assertThrows(NullPointerException.class, () -> new TournamentPowerAnalyzer(null, validBrackets));
    }

    /**
     * Verifies that the constructor throws when the servers list is empty.
     */
    @Test
    void constructor_emptyServers_throwsException() {
        List<List<String>> validBrackets = List.of(List.of("A", "B"));
        assertThrows(IllegalArgumentException.class, () -> new TournamentPowerAnalyzer(List.of(), validBrackets));
    }

    /**
     * Verifies that the constructor throws when the explicit MDE is negative.
     */
    @Test
    void constructor_negativeMde_throwsException() {
        List<List<String>> validBrackets = List.of(List.of("A", "B"));
        assertThrows(IllegalArgumentException.class, () -> new TournamentPowerAnalyzer(
                List.of(new LocalComputeServer("test-0")), validBrackets, -1.0));
    }

    /**
     * Verifies that a valid constructor invocation succeeds.
     */
    @Test
    void constructor_validArgs_succeeds() {
        List<List<String>> validBrackets = List.of(List.of("A", "B"));
        assertDoesNotThrow(() -> new TournamentPowerAnalyzer(
                List.of(new LocalComputeServer("test-0")), validBrackets));
    }

    /**
     * Verifies that a valid constructor invocation with an explicit MDE succeeds.
     */
    @Test
    void constructor_validArgsAndExplicitMde_succeeds() {
        List<List<String>> validBrackets = List.of(List.of("A", "B"));
        assertDoesNotThrow(() -> new TournamentPowerAnalyzer(
                List.of(new LocalComputeServer("test-0")), validBrackets, 20.0));
    }

    @BeforeEach
    void setUp() {
        // Ensure Config.configuration is initialized with a default or mock configuration
        Config.configuration.addProperty("powerAnalyzerConcurrency", 4);
    }
}
