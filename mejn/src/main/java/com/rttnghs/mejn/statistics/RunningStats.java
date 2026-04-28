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

/**
 * Thread-safe online mean and variance tracker using Welford's algorithm.
 *
 * <p>All mutating and reading methods are {@code synchronized}, so a single instance
 * can be safely updated from one thread (e.g. a batch-callback thread) while being
 * read from another (e.g. a UI or reporting thread).
 *
 * <h2>Consistent multi-value reads</h2>
 * <p>When multiple derived statistics are needed together (e.g. for display), prefer
 * {@link #snapshot()} over calling individual getters in sequence.  Individual getters
 * each acquire and release the lock separately; between two such calls a concurrent
 * {@link #update(double)} could fire, making the pair of values inconsistent.
 * {@code snapshot()} computes everything under one lock acquisition and returns an
 * immutable, mutually-consistent {@link Snapshot}.
 *
 * <h2>Extension point</h2>
 * <p>Subclasses can override {@link #update(double)} to add reactive behaviour — for
 * example, an {@code ObservableRunningStats} in a JavaFX module could push each new
 * observation into a {@code DoubleProperty} bound to a chart series, without any
 * changes to the core statistics logic here.
 */
public class RunningStats {

    /**
     * Z-score for a two-sided α = 0.05 confidence interval (95 % confidence).
     * Placed here so that {@link #moe95()} is fully self-contained and subclasses
     * can reference it without depending on {@code TournamentPowerAnalyzer}.
     */
    public static final double Z_ALPHA_2 = 1.96;

    private int n = 0;
    private double mean = 0.0;
    private double m2 = 0.0; // running sum of squared deviations (Welford)

    /**
     * Incorporate a new observation into the running statistics.
     *
     * <p>Subclasses that override this method should call {@code super.update(value)}
     * first to keep the Welford state consistent.
     *
     * @param value the new observed value
     */
    public synchronized void update(double value) {
        n++;
        double delta = value - mean;
        mean += delta / n;
        double delta2 = value - mean;
        m2 += delta * delta2;
    }

    /** Number of observations incorporated so far. */
    public synchronized int count() {
        return n;
    }

    /** Current running mean. */
    public synchronized double mean() {
        return mean;
    }

    /**
     * Sample variance (Bessel-corrected).
     *
     * @return variance, or {@code 0.0} if fewer than 2 observations have been made
     */
    public synchronized double variance() {
        return n < 2 ? 0.0 : m2 / (n - 1);
    }

    /** Sample standard deviation. */
    public synchronized double stddev() {
        return Math.sqrt(variance());
    }

    /** Standard error of the mean: {@code stddev / √n}. */
    public synchronized double stderr() {
        return n == 0 ? 0.0 : stddev() / Math.sqrt(n);
    }

    /** 95 % margin of error: {@code Z_ALPHA_2 × stderr}. */
    public synchronized double moe95() {
        return Z_ALPHA_2 * stderr();
    }

    /** Lower bound of the 95 % confidence interval. */
    public synchronized double ciLow() {
        return mean - moe95();
    }

    /** Upper bound of the 95 % confidence interval. */
    public synchronized double ciHigh() {
        return mean + moe95();
    }

    /**
     * Returns a consistent, immutable snapshot of all derived statistics computed under
     * a single lock acquisition.
     *
     * <p>Prefer this over calling individual getters in sequence whenever multiple values
     * are needed together (e.g. for table rendering or UI binding), to guarantee that all
     * values reflect the same observation count and cannot be split by a concurrent
     * {@link #update(double)}.
     *
     * @return an immutable {@link Snapshot} of the current state
     */
    public synchronized Snapshot snapshot() {
        double variance = n < 2 ? 0.0 : m2 / (n - 1);
        double stddev   = Math.sqrt(variance);
        double stderr   = n == 0 ? 0.0 : stddev / Math.sqrt(n);
        double moe95    = Z_ALPHA_2 * stderr;
        return new Snapshot(n, mean, variance, stddev, stderr, moe95, mean - moe95, mean + moe95);
    }

    /**
     * Immutable, mutually-consistent point-in-time view of all derived statistics.
     *
     * @param count    number of observations at the time of the snapshot
     * @param mean     running mean
     * @param variance sample variance (Bessel-corrected; 0 if {@code count < 2})
     * @param stddev   sample standard deviation
     * @param stderr   standard error of the mean
     * @param moe95    95 % margin of error ({@code Z_ALPHA_2 × stderr})
     * @param ciLow    lower bound of the 95 % confidence interval
     * @param ciHigh   upper bound of the 95 % confidence interval
     */
    public record Snapshot(int count, double mean, double variance, double stddev,
                           double stderr, double moe95, double ciLow, double ciHigh) {}
}


