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

import com.rttnghs.rmi.protocol.TaskCallback;
import com.rttnghs.rmi.protocol.TaskServer;
import com.rttnghs.rmi.protocol.dto.TournamentBatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for TournamentBatchServer: submits a real TournamentBatch
 * and verifies the async result via TaskCallback.
 *
 * <p>No mocks - a real TournamentBatchServer executes tournament games and
 * delivers scores to a local TaskCallback stub.
 */
public class TournamentBatchServerIT {

    // 4 competitors, each with 6 valid SomeMoveValuator parameters
    private static final List<List<Integer>> COMPETITORS = List.of(
            List.of(-10,  5,  8, -5,  3, -3),
            List.of(  5, 10,  6, -2,  4, -4),
            List.of(  0,  8, 10,  0,  2, -2),
            List.of( -5,  6,  4, -8,  5, -5)
    );

    private TournamentBatchServer server;
    // The implementation object is kept separately so tearDown can unexport it.
    // UnicastRemoteObject.unexportObject() requires the original impl, not the stub.
    private TaskCallback<ArrayList<Integer>> callbackImpl;

    @BeforeEach
    void setUp() throws RemoteException {
        server = new TournamentBatchServer(2);
    }

    @AfterEach
    void tearDown() throws RemoteException {
        if (callbackImpl != null) {
            UnicastRemoteObject.unexportObject(callbackImpl, true);
            callbackImpl = null;
        }
        UnicastRemoteObject.unexportObject(server, true);
    }

    /**
     * Exports the callback implementation and returns the RMI stub to pass to submitTask.
     * Also saves the impl in {@code callbackImpl} so tearDown can unexport it.
     */
    @SuppressWarnings("unchecked")
    private TaskCallback<ArrayList<Integer>> export(TaskCallback<ArrayList<Integer>> impl) throws RemoteException {
        callbackImpl = impl;
        return (TaskCallback<ArrayList<Integer>>) UnicastRemoteObject.exportObject(impl, 0);
    }

    @Test
    void connectReturnsTrue() throws RemoteException {
        assertTrue(server.connect());
    }

    @Test
    void submitValidBatchDeliversResultToCallback() throws Exception {
        TournamentBatch batch = new TournamentBatch(COMPETITORS, 20);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ArrayList<Integer>> receivedResult = new AtomicReference<>();
        AtomicInteger receivedTaskId = new AtomicInteger();

        TaskCallback<ArrayList<Integer>> stub = export((taskId, result) -> {
            receivedTaskId.set(taskId);
            receivedResult.set(result);
            latch.countDown();
            return false; // do not continue
        });

        int taskId = server.submitTask(batch, stub);

        assertNotEquals(TaskServer.REJECTED, taskId, "Task should be accepted");
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Callback should fire within 10 seconds");

        ArrayList<Integer> scores = receivedResult.get();
        assertNotNull(scores);
        assertEquals(COMPETITORS.size(), scores.size(), "One score per competitor");
        assertEquals(taskId, receivedTaskId.get(), "Callback taskId matches submitted taskId");

        // Scores are normalized non-negative integers
        for (int score : scores) {
            assertTrue(score >= 0, "All scores should be >= 0, got: " + score);
        }
    }

    @Test
    void rejectsInvalidCompetitorParameters() throws RemoteException {
        // Only 2 params per competitor - fails SomeMoveValuator's minimum-of-6 check
        TournamentBatch bad = new TournamentBatch(
                List.of(List.of(1, 2), List.of(3, 4)), 5);

        TaskCallback<ArrayList<Integer>> stub = export((id, r) -> false);

        assertEquals(TaskServer.REJECTED, server.submitTask(bad, stub),
                "Batch with invalid competitor params should be rejected");
    }
}

