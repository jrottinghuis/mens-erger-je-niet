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
import com.rttnghs.mejn.rmi.BatchCallback;
import com.rttnghs.mejn.rmi.BatchConfig;
import com.rttnghs.mejn.rmi.BatchResult;
import com.rttnghs.mejn.rmi.ComputeServer;
import com.rttnghs.mejn.rmi.LocalComputeServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TournamentPowerAnalyzerTest {

    private static final class TestComputeServer implements ComputeServer {
        private final AtomicInteger started = new AtomicInteger();
        private final AtomicInteger completed = new AtomicInteger();
        private final Map<String, Double> scores;

        private TestComputeServer(Map<String, Double> scores) {
            this.scores = scores;
        }

        @Override
        public String getId() {
            return "test-server";
        }

        @Override
        public void submitBatch(BatchConfig config, BatchCallback callback) {
            boolean continueWork = true;
            while (continueWork) {
                started.incrementAndGet();
                completed.incrementAndGet();
                continueWork = callback.onBatchComplete(new BatchResult(getId(), config.generationId(), scores, Instant.now(), Instant.now()));
            }
        }

        @Override
        public int getBatchesStarted() {
            return started.get();
        }

        @Override
        public int getBatchesCompleted() {
            return completed.get();
        }
    }

    private static final class TestAppender extends AbstractAppender {

        private final List<String> messages = new ArrayList<>();

        private TestAppender(String name) {
            super(name, null, null, false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            messages.add(event.getMessage().getFormattedMessage());
        }
    }

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

    /**
     * Verifies that the summary reports the effective MDE used for the run.
     */
    @Test
    void resultSummary_includesEffectiveMde() {
        TournamentPowerAnalyzer.Result result = new TournamentPowerAnalyzer.Result(
                1,
                5_000,
                java.util.Map.of(),
                java.util.List.of(),
                TournamentPowerAnalyzer.StopReason.SUFFICIENT_POWER,
                java.time.Duration.ZERO,
                4,
                6,
                20.0,
                java.util.List.of(new ComputeServer.StatsSnapshot("test-0", 1, 1, 0, 0, 0, 0))
        );

        assertTrue(result.toSummary().contains("Effective MDE: 20.00"));
        assertTrue(result.toSummary().contains("staleSub"));
    }

    /**
     * Verifies that the early-stop log line includes the effective MDE used for the run.
     */
    @Test
    void earlyStopLog_includesEffectiveMde() throws InterruptedException {
        Config.configuration.setProperty("powerAnalyzerWarmupBatches", 1);
        Config.configuration.setProperty("powerAnalyzerMaxBatches", 5);
        Config.configuration.setProperty("powerAnalyzerGamesPerBatch", 100);
        Config.configuration.setProperty("powerAnalyzerTimeoutSeconds", 10);

        TestAppender appender = new TestAppender("test-appender");
        appender.start();
        LoggerContext context = LoggerContext.getContext(false);
        org.apache.logging.log4j.core.Logger logger = context.getLogger(TournamentPowerAnalyzer.class.getName());
        Level originalLevel = logger.getLevel();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        try {
            TournamentPowerAnalyzer analyzer = new TournamentPowerAnalyzer(
                    List.of(new TestComputeServer(Map.of("A", 100.0, "B", 90.0))),
                    List.of(List.of("A", "B")),
                    20.0);

            analyzer.run();

            assertTrue(appender.messages.stream().anyMatch(message ->
                            message.contains("Early stop after 1 batches") && message.contains("Effective MDE: 20.00")),
                    () -> "Expected early-stop log with effective MDE, got: " + appender.messages);
        } finally {
            logger.removeAppender(appender);
            logger.setLevel(originalLevel);
            appender.stop();
        }
    }

    /**
     * Verifies that the periodic progress log line includes the effective MDE used for the run.
     */
    @Test
    void progressLog_includesEffectiveMde() throws InterruptedException {
        Config.configuration.setProperty("powerAnalyzerWarmupBatches", 10);
        Config.configuration.setProperty("powerAnalyzerMaxBatches", 5);
        Config.configuration.setProperty("powerAnalyzerGamesPerBatch", 100);
        Config.configuration.setProperty("powerAnalyzerTimeoutSeconds", 10);

        TestAppender appender = new TestAppender("test-progress-appender");
        appender.start();
        LoggerContext context = LoggerContext.getContext(false);
        org.apache.logging.log4j.core.Logger logger = context.getLogger(TournamentPowerAnalyzer.class.getName());
        Level originalLevel = logger.getLevel();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        try {
            TournamentPowerAnalyzer analyzer = new TournamentPowerAnalyzer(
                    List.of(new TestComputeServer(Map.of("A", 100.0, "B", 70.0))),
                    List.of(List.of("A", "B")),
                    20.0);

            analyzer.run();

            assertTrue(appender.messages.stream().anyMatch(message ->
                            message.contains("After 5 batches") && message.contains("Effective MDE: 20.00")),
                    () -> "Expected progress log with effective MDE, got: " + appender.messages);
        } finally {
            logger.removeAppender(appender);
            logger.setLevel(originalLevel);
            appender.stop();
        }
    }

    /**
     * Verifies that results from a stale generation are ignored.
     */
    @Test
    void staleGenerationResult_isIgnored() throws InterruptedException {
        Object originalTimeout = Config.configuration.getProperty("powerAnalyzerTimeoutSeconds");
        ComputeServer staleServer = new ComputeServer() {
            @Override
            public String getId() {
                return "stale-server";
            }

            @Override
            public void submitBatch(BatchConfig config, BatchCallback callback) {
                callback.onBatchComplete(new BatchResult(getId(), config.generationId() + 1, Map.of("A", 100.0, "B", 0.0), Instant.now(), Instant.now()));
            }

            @Override
            public int getBatchesStarted() {
                return 1;
            }

            @Override
            public int getBatchesCompleted() {
                return 1;
            }
        };

        try {
            Config.configuration.setProperty("powerAnalyzerTimeoutSeconds", 0);
            TournamentPowerAnalyzer analyzer = new TournamentPowerAnalyzer(
                    List.of(staleServer),
                    List.of(List.of("A", "B")),
                    20.0);

            TournamentPowerAnalyzer.Result result = analyzer.run();

            assertEquals(0, result.completedBatches());
            assertEquals(TournamentPowerAnalyzer.StopReason.MAX_BATCHES, result.stopReason());
        } finally {
            if (originalTimeout == null) {
                Config.configuration.clearProperty("powerAnalyzerTimeoutSeconds");
            } else {
                Config.configuration.setProperty("powerAnalyzerTimeoutSeconds", originalTimeout);
            }
        }
    }

    @BeforeEach
    void setUp() {
        // Ensure Config.configuration is initialized with a default or mock configuration
        Config.configuration.addProperty("powerAnalyzerConcurrency", 4);
    }
}
