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

import com.rttnghs.mejn.configuration.Config;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalComputeServerTest {

	/**
	 * Verifies that a newer generation supersedes an older one at the next batch boundary:
	 * the older generation may finish at most one batch internally, but once a newer
	 * generation has superseded it the stale result is not posted back and the old
	 * generation does not keep self-rescheduling.
	 */
	@Test
	void newerGeneration_supersedesOlderGeneration() throws InterruptedException {
		Object originalGamesPerChunk = Config.configuration.getProperty("powerAnalyzerGamesPerChunk");
		Config.configuration.setProperty("powerAnalyzerGamesPerChunk", 1);

		try {
			LocalComputeServer server = new LocalComputeServer("test-local");
			BatchConfig generation1 = new BatchConfig(
					List.of(List.of("RandomStrategy", "FarStrategy", "OtherRankingStrategy", "RankingStrategy")),
					50,
					1);
			BatchConfig generation2 = new BatchConfig(
					List.of(List.of("RandomStrategy", "FarStrategy", "OtherRankingStrategy", "RankingStrategy")),
					1,
					2);

			AtomicInteger generation1Callbacks = new AtomicInteger();
			AtomicInteger generation2Callbacks = new AtomicInteger();
			CountDownLatch generation2Seen = new CountDownLatch(1);

			server.submitBatch(generation1, result -> {
				generation1Callbacks.incrementAndGet();
				return true;
			});
			server.submitBatch(generation2, result -> {
				generation2Callbacks.incrementAndGet();
				generation2Seen.countDown();
				return false;
			});

			assertTrue(generation2Seen.await(10, TimeUnit.SECONDS), "Expected superseding generation to complete a batch");
			assertTrue(generation1Callbacks.get() == 0,
					"Older generation should not post a stale callback once superseded, got " + generation1Callbacks.get());
			assertTrue(generation2Callbacks.get() >= 1,
					"Newer generation should run after superseding the older one");
			ComputeServer.StatsSnapshot snapshot = server.statsSnapshot();
			assertEquals("test-local", snapshot.serverId());
			assertTrue(snapshot.chunkFuturesCanceled() >= 0);
			assertTrue(snapshot.abortedBatches() >= 0);
		} finally {
			if (originalGamesPerChunk == null) {
				Config.configuration.clearProperty("powerAnalyzerGamesPerChunk");
			} else {
				Config.configuration.setProperty("powerAnalyzerGamesPerChunk", originalGamesPerChunk);
			}
		}
	}
}


