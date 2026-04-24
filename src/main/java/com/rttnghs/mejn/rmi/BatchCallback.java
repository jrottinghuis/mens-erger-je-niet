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
 * Callback invoked by a {@link ComputeServer} when a batch of tournaments completes.
 *
 * <h2>Continuation protocol</h2>
 * <p>The boolean return value is the key efficiency mechanism: returning {@code true}
 * tells the <em>server</em> to start another batch with the same configuration
 * immediately, without a new request from the coordinator.  This eliminates an
 * extra RMI round-trip per batch once the remote transport layer is added.
 * Returning {@code false} parks the server; the coordinator will call
 * {@link ComputeServer#submitBatch} again when it wants more work.
 *
 * <h2>Generation supersession</h2>
 * <p>Each {@link BatchResult} carries a {@code generationId}.  If a result belongs to a
 * stale generation, implementations should ignore it and return {@code false} so the
 * server stops self-rescheduling that older run at the next batch boundary.
 *
 * <h2>Thread safety</h2>
 * <p>Implementations must be thread-safe: multiple servers may invoke this
 * concurrently from different threads.
 *
 * <h2>RMI note</h2>
 * <p>This interface will extend {@code java.rmi.Remote} when the transport layer
 * is introduced.  The return value maps naturally to the RMI method reply, keeping
 * the round-trip count minimal.
 */
public interface BatchCallback {

    /**
     * Called by a server when one batch has completed.
     *
     * @param result the completed batch result including per-strategy scores
     * @return {@code true} if the server should immediately start another batch
     *         with the same configuration; {@code false} if it should idle
     */
    boolean onBatchComplete(BatchResult result);
}

