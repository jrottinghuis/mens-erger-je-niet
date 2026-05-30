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

import com.rttnghs.rmi.configuration.Config;
import com.rttnghs.rmi.protocol.TaskServer;
import com.rttnghs.rmi.protocol.dto.TournamentBatch;
import com.rttnghs.rmi.protocol.impl.AbstractTaskCallback;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Ad-hoc RMI client that looks up a running {@link TournamentBatchServer} and
 * submits a single batch of four randomly-generated competitors.
 *
 * <p>Intended for manual smoke-testing from the command line:
 * <pre>
 *   gradle runTournamentBatchClient [-PserverName=&lt;suffix&gt;]
 * </pre>
 * The server suffix defaults to {@code 0}, giving bind name
 * {@code TournamentBatchServer-0}.
 *
 * <p>Prerequisites: an RMI registry and at least one {@link TournamentBatchServer}
 * must already be running.
 */
public class TournamentBatchClient {

    private static final Logger logger = Logger.getLogger(TournamentBatchClient.class.getName());

    /** Number of competitors per test batch. */
    private static final int COMPETITOR_COUNT = 4;
    /** Parameters per competitor — must satisfy SomeMoveValuator's minimum-of-6 check. */
    private static final int PARAMS_PER_COMPETITOR = 6;
    /** Repetitions for the test batch. */
    private static final int REPETITIONS = 5120;
    /** How long to wait for the server to return a result before giving up. */
    private static final int TIMEOUT_SECONDS = 30;

    public static void main(String[] args) throws Exception {
        String suffix   = args.length > 0 ? args[0] : "0";
        String bindName = "TournamentBatchServer-" + suffix;
        int    port     = Config.RMI_REGISTRY_PORT;

        // ── Look up server ───────────────────────────────────────────────────
        Registry registry = LocateRegistry.getRegistry(port);
        @SuppressWarnings("unchecked")
        TaskServer<TournamentBatch, ArrayList<Integer>> server =
                (TaskServer<TournamentBatch, ArrayList<Integer>>) registry.lookup(bindName);
        logger.info("Connected to '%s' on port %d".formatted(bindName, port));

        // ── Build a batch of random competitors ──────────────────────────────
        Random rng = new Random();
        List<List<Integer>> competitors = new ArrayList<>(COMPETITOR_COUNT);
        for (int c = 0; c < COMPETITOR_COUNT; c++) {
            List<Integer> params = new ArrayList<>(PARAMS_PER_COMPETITOR);
            for (int p = 0; p < PARAMS_PER_COMPETITOR; p++) {
                params.add(rng.nextInt(21) - 10); // range [-10, 10]
            }
            competitors.add(params);
        }
        TournamentBatch batch = new TournamentBatch(competitors, REPETITIONS);
        logger.info("Submitting batch: %d competitors × %d reps".formatted(COMPETITOR_COUNT, REPETITIONS));
        System.out.println("Competitors:");
        for (int c = 0; c < competitors.size(); c++) {
            System.out.printf("  [%d] %s%n", c, competitors.get(c));
        }

        // ── Create and export a one-shot callback ────────────────────────────
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ArrayList<Integer>> resultRef = new AtomicReference<>();

        AbstractTaskCallback<ArrayList<Integer>> callback = new AbstractTaskCallback<>() {
            @Override
            protected boolean handleResult(ArrayList<Integer> scores) {
                resultRef.set(scores);
                latch.countDown();
                return false; // single result is enough
            }
        };

        // ── Submit and wait ──────────────────────────────────────────────────
        int taskId = server.submitTask(batch, callback);
        if (taskId == TaskServer.REJECTED) {
            System.out.println("Task REJECTED by server — is the server busy?");
        } else {
            callback.notifySubmission(taskId);
            System.out.printf("Task accepted (id=%d), waiting up to %ds for result…%n", taskId, TIMEOUT_SECONDS);

            if (latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                System.out.println("Scores: " + resultRef.get());
            } else {
                System.out.println("Timed out waiting for result.");
            }
        }

        UnicastRemoteObject.unexportObject(callback, true);
    }
}
