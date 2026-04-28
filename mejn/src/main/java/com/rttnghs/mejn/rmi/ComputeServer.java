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

/**
 * A compute server capable of running batches of tournaments on behalf of a coordinator.
 *
 * <h2>Dispatch model</h2>
 * <p>{@link #submitBatch} returns <em>immediately</em> after accepting the work.
 * The result is delivered asynchronously via the provided {@link BatchCallback}.
 * The callback's return value determines whether another batch should start right
 * away (see {@link BatchCallback} for the continuation protocol).
 * A newer {@link BatchConfig#generationId()} may supersede an older one on the same
 * server; implementations should let the older generation finish at most its current
 * batch, then stop rescheduling it and switch to the newer generation.
 *
 * <h2>RMI note</h2>
 * <p>This interface will extend {@code java.rmi.Remote} once the transport layer is
 * added.  The in-process {@link LocalComputeServer} implementation is a drop-in
 * stand-in until then.
 */
public interface ComputeServer {

    /**
     * Immutable point-in-time view of a server's cumulative statistics.
     *
     * @param serverId                stable server identifier
     * @param batchesStarted          total batches started since server creation
     * @param batchesCompleted        total batches fully completed since server creation
     * @param staleSubmissionsIgnored stale submit requests ignored because a newer
     *                                generation had already been observed
     * @param supersededResultsDropped completed batch results dropped locally because the
     *                                 generation became stale before callback delivery
     * @param abortedBatches          batches aborted locally during execution after
     *                                supersession/cancellation was detected
     * @param chunkFuturesCanceled    queued chunk futures canceled due to newer-generation
     *                                supersession
     */
    record StatsSnapshot(
            String serverId,
            int batchesStarted,
            int batchesCompleted,
            int staleSubmissionsIgnored,
            int supersededResultsDropped,
            int abortedBatches,
            int chunkFuturesCanceled) {
    }

    /**
     * Stable identifier for this server (e.g. {@code "local-0"} or
     * {@code "host:1099/server-A"} for a future RMI binding).
     */
    String getId();

    /**
     * Accept one batch of work and begin processing asynchronously.
     *
     * <p>Returns immediately.  The result is posted back via
     * {@link BatchCallback#onBatchComplete}, whose return value tells this server
     * whether to immediately start another batch with the same configuration.
     *
     * @param config   batch configuration (brackets, games per batch)
     * @param callback where to post the completed {@link BatchResult}
     */
    void submitBatch(BatchConfig config, BatchCallback callback);

    /**
     * Total number of batches started on this server since creation,
     * including any currently in progress.
     */
    int getBatchesStarted();

    /**
     * Total number of batches fully completed on this server since creation
     * (i.e. {@link BatchCallback#onBatchComplete} has been called and returned).
     */
    int getBatchesCompleted();

    /**
     * Immutable point-in-time snapshot of this server's statistics.
     */
    default StatsSnapshot statsSnapshot() {
        return new StatsSnapshot(getId(), getBatchesStarted(), getBatchesCompleted(), 0, 0, 0, 0);
    }
}

