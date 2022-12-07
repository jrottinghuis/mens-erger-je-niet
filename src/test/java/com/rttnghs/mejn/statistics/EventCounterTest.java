package com.rttnghs.mejn.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class EventCounterTest {

	@Test
	final void testIncrementAndAdd() {
		EventCounter<String, String> eventCounter = new EventCounter<>();

		eventCounter.increment("actorA", "A");
		eventCounter.increment("actorA", "B");
		eventCounter.increment("actorA", "B");
		eventCounter.increment("actorA", "C");
		eventCounter.increment("actorA", "C");
		eventCounter.increment("actorA", "C");
		eventCounter.increment("actorA", "D");
		eventCounter.increment("actorA", "D");
		eventCounter.increment("actorA", "D");
		eventCounter.increment("actorA", "D");

		// eventCounter is now {actorA={A=1, B=2, C=3, D=4}}
		assertEquals(1, eventCounter.getCount("actorA", "A"));
		assertEquals(2, eventCounter.getCount("actorA", "B"));
		assertEquals(3, eventCounter.getCount("actorA", "C"));
		assertEquals(4, eventCounter.getCount("actorA", "D"));

		eventCounter.add("actorB", "E", 4);
		eventCounter.add("actorB", "F", 3);
		eventCounter.add("actorB", "G", 1);
		eventCounter.increment("actorB", "G");
		eventCounter.add("actorB", "H", 1);

		// eventCounter={actorB={E=4, F=3, G=2, H=1}, actorA={A=1, B=2, C=3, D=4}}
		assertEquals(2, eventCounter.getCount("actorB", "G"));
		assertEquals(4, eventCounter.getCount("actorA", "D"));

		EventCounter<String, String> eventCounter2 = new EventCounter<>();

		eventCounter2.add("actorC", "I", 3);
		eventCounter2.add("actorC", "J", 7);
		eventCounter2.add("actorC", "K", 11);
		eventCounter2.increment("actorC", "L");
		eventCounter2.add("actorC", "M", 17);

		// eventCounter2 is now {actorC={I=3, J=7, K=11, L=1, M=17}}
		assertEquals(0, eventCounter.getCount("actorC", "I")); // actorC not in eventCounter
		assertEquals(3, eventCounter2.getCount("actorC", "I"));

		eventCounter2.add(eventCounter2);
		assertEquals(6, eventCounter2.getCount("actorC", "I"));
		assertEquals(14, eventCounter2.getCount("actorC", "J"));
		assertEquals(22, eventCounter2.getCount("actorC", "K"));
		assertEquals(2, eventCounter2.getCount("actorC", "L"));
		assertEquals(34, eventCounter2.getCount("actorC", "M"));

		eventCounter2.add(null);
		assertEquals(34, eventCounter2.getCount("actorC", "M"));

		assertEquals("{actorC={I=6, J=14, K=22, L=2, M=34}}", eventCounter2.toString());
	}

	@Test
	final void testGenerics() {

		EventCounter<String, Integer> eventCounter = new EventCounter<>();
		eventCounter.increment("actorOne", 1);

		EventCounter<String, Short> eventCounter2 = new EventCounter<>();
		short two = 2;
		eventCounter2.add("actorTwo", two, 2);

		EventCounter<String, Number> eventCounter3 = new EventCounter<>();
		eventCounter3.add(eventCounter).add(eventCounter2).add(eventCounter3);

		// eventCounter3={actorTwo={2=4}, actorOne={1=2}}

		eventCounter3.add("actorThree", 3l, 5);

		// eventCounter3={actorThree={3=5}, actorTwo={2=4}, actorOne={1=2}}
	}

	@Test
	final void testGetActors() {
		EventCounter<String, String> eventCounter = new EventCounter<>();

		Set<String> empty = eventCounter.getActors();
		assertEquals(0, empty.size());

		eventCounter.increment("actorA", "A");
		eventCounter.add("actorB", "E", 4);
		Set<String> actors = eventCounter.getActors();
		assertEquals(2, actors.size());

		eventCounter.add("actorC", "I", 3);
		assertEquals(2, actors.size());
		Set<String> moreActors = eventCounter.getActors();
		assertEquals(3, moreActors.size());

		eventCounter.increment(null, "Q");
		eventCounter.add(null, "Q", 1);
		eventCounter.add(null, null, 1);
		eventCounter.add(null, null, null);
		eventCounter.add("actorD", "Z", null);
		eventCounter.add("actorD", null, null);

		assertEquals(3, moreActors.size());

		assertThrows(UnsupportedOperationException.class, () -> actors.add("boom!"));
	}

	@Test
	final void testGetEvents() {
		EventCounter<String, String> eventCounter = new EventCounter<>();
		eventCounter.increment("actorA", "A");
		eventCounter.add("actorA", "B", 2);
		eventCounter.add("actorA", "C", 3);
		eventCounter.add("actorA", "D", 4);
		eventCounter.add("actorA", "E", 4);
		eventCounter.add("actorA", "F", 3);
		eventCounter.add("actorA", "G", 2);
		eventCounter.add("actorA", "H", 1);
		eventCounter.add("actorA", "I", 3);
		eventCounter.add("actorA", "J", 7);
		eventCounter.add("actorA", "K", 11);
		eventCounter.increment("actorB", "L"); // Note the different actor
		eventCounter.add("actorC", "M", 17);

		Set<String> events = eventCounter.getEvents("actorNobody");
		assertEquals(0, events.size());
		events = eventCounter.getEvents("actorA");
		assertEquals(11, events.size());
		assertTrue(events.contains("K"));
	}

	@Test
	final void testCopy() {
		EventCounter<String, Integer> eventCounter = new EventCounter<>();
		eventCounter.increment("actorA", 1);
		eventCounter.add("actorB", 2, 4);
		eventCounter.add("actorC", 3, 9);
		Set<String> actors = eventCounter.getActors();
		assertEquals(3, actors.size());
		assertEquals(1, eventCounter.getEvents("actorB").size());

		EventCounter<String, Integer> copy = eventCounter.copy();
		Set<String> copyActors = copy.getActors();
		assertEquals(3, copyActors.size());
		assertEquals(1, copy.getEvents("actorB").size());

		// Now modify original
		eventCounter.add("actorB", 4, 16);
		eventCounter.add("actorD", 5, 25);
		assertEquals(2, eventCounter.getEvents("actorB").size());
		assertEquals(4, eventCounter.getActors().size());

		assertEquals(1, copy.getEvents("actorB").size());
		assertEquals(3, copyActors.size());

		copy.increment("actorA", 1);

		assertEquals(1, eventCounter.getCount("actorA", 1));
		assertEquals(2, copy.getCount("actorA", 1));

		// eventCounter={actorD={5=25}, actorC={3=9}, actorB={2=4, 4=16}, actorA={1=1}}
		// copy={actorC={3=9}, actorB={2=4}, actorA={1=2}}
	}
	
	@Test
	final void testGetNormalizedScore() {
		Function<Integer, Integer> scorer = (finishPosition) -> Score.get(finishPosition, 4);
		assertEquals(0, EventCounter.getNormalizedScores(null, scorer, 10).keySet().size());
		
		
		
		EventCounter<String, Integer> eventCounter = new EventCounter<>();
		eventCounter.increment("actorOne", 1);
		EventCounter<String, Short> eventCounter2 = new EventCounter<>();
		short two = 2;
		eventCounter2.add("actorTwo", two, 2);
		EventCounter<String, Number> eventCounter3 = new EventCounter<>();
		eventCounter3.add(eventCounter).add(eventCounter2);

		eventCounter3.add("actorThree", 3l, 3);

		System.out.println(eventCounter3);
		
		
		// eventCounter3={actorOne={1=1}, actorThree={3=3}, actorTwo={2=2}}

		Map<String, Integer> scores = EventCounter.getNormalizedScores(eventCounter3, (number) -> number.intValue(), 1);
		assertEquals(1, scores.get("actorOne"));
		assertEquals(2, scores.get("actorTwo"));
		assertEquals(3, scores.get("actorThree"));
	}

}
