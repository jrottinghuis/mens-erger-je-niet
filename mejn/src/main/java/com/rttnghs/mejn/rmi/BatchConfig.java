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
import java.util.List;

/**
 * Configuration for a single batch of tournaments, sent from the coordinator to a
 * {@link ComputeServer}.
 *
 * <p>Serializable so it can be transmitted over RMI when the remote transport layer
 * is added.  The brackets hold strategy <em>names</em> (not instances), so the
 * server resolves them locally via its own {@code StrategyFactory}.
 *
 * @param brackets      list of bracket strategy-name lists; each bracket corresponds to
 *                      one {@link com.rttnghs.mejn.Tournament} run per batch
 * @param gamesPerBatch number of games each tournament plays
 * @param generationId  logical run identifier used to supersede older work without an
 *                      explicit cancel RPC; a newer generation may replace an older one
 *                      at the next batch boundary on the same server.
 *                      <strong>Must be monotonically increasing</strong> across submitted
 *                      runs for a given coordinator/server set. {@link LocalComputeServer}
 *                      compares generation IDs numerically ({@code newer > older}) when
 *                      deciding whether work is stale, so reusing or decreasing IDs can
 *                      cause valid work to be ignored or stale work to continue longer
 *                      than intended. The intended range is the full signed {@code int}
 *                      space, typically starting at {@link Integer#MIN_VALUE}.
 */
public record BatchConfig(List<List<String>> brackets, int gamesPerBatch, int generationId) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

