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

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Result of a single completed batch, posted back from a {@link ComputeServer} to
 * the coordinator via {@link BatchCallback#onBatchComplete}.
 *
 * <p>Serializable so it can be transmitted over RMI when the remote transport layer
 * is added.
 *
 * @param serverId    stable identifier of the server that ran the batch
 * @param scores      per-strategy normalized mean scores for this batch
 * @param startedAt   wall-clock instant when the batch started
 * @param completedAt wall-clock instant when the batch completed
 */
public record BatchResult(
        String serverId,
        Map<String, Double> scores,
        Instant startedAt,
        Instant completedAt
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Wall-clock time this batch took in milliseconds. */
    public long elapsedMillis() {
        return Duration.between(startedAt, completedAt).toMillis();
    }
}

