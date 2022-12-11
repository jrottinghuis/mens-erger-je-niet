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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Used to keep track of count of events happening for actors.
 *
 * @param <A> The type of actor generating the events. Type A must have hashCode
 *            and equals properly defined.
 * @param <E> The type of event. Type E must have hashCode and equals properly
 *            defined.
 */
public class EventCounter<A, E> {

	/**
	 * Map of actor -> map (event->count)
	 */
	private final Map<A, Map<E, Integer>> actorEventCounts;

	public EventCounter() {
		actorEventCounts = new TreeMap<>();
	}

	/**
	 * @param previousCount non-null map of events to counts.
	 * @return new map with the count for the given event incremented.
	 */
	private Map<E, Integer> addEvents(Map<E, Integer> eventCounts, E event, Integer count) {
		eventCounts.merge(event, count, Integer::sum);
		return eventCounts;
	}

	/**
	 * Like Java 9's {@link Map#of(E, Integer)} but then a mutable version.
	 * 
	 * @param event to add to the map
	 * @param count to add to the map
	 * @return a map with a single element of event pointing to count.
	 */
	private Map<E, Integer> mapOf(E event, Integer count) {
		Map<E, Integer> hashMap = new HashMap<>();
		hashMap.put(event, count);
		return hashMap;
	}

	/**
	 * An actor has had one event happen.
	 * 
	 * @param actor performing the event
	 * @param event the event that happened
	 * @return reference to this for chaining calls.
	 */
	public EventCounter<A, E> increment(A actor, E event) {
		return add(actor, event, 1);
	}

	/**
	 * Add how many times the event happened for the actor. If any argument is null,
	 * nothing is changed.
	 * 
	 * @param actor actor had the event happen
	 * @param event event that happened
	 * @param count how many times the event happened.
	 * @return reference to self after adding the provided event count for the
	 *         actor.
	 */
	public EventCounter<A, E> add(A actor, E event, Integer count) {
		if (actor != null && event != null & count != null) {
			BiFunction<? super Map<E, Integer>, ? super Map<E, Integer>, ? extends Map<E, Integer>> sumEvents = (
					previousCounts, newCounts) -> addEvents(previousCounts, event, count);
			actorEventCounts.merge(actor, mapOf(event, count), sumEvents);
		}
		return this;
	}

	/**
	 * @param actor
	 * @param otherEvents map of events with their respective counts.
	 * @return reference to this for convenience.
	 */
	private EventCounter<A, E> add(A actor, Map<? extends E, Integer> otherEvents) {
		if (otherEvents != null) {
			otherEvents.forEach((E event, Integer count) -> this.add(actor, event, count));
		}
		return this;
	}

	/**
	 * Add the other EventCounter to this one.
	 * 
	 * @param other
	 * @return reference to this class with the other one added.
	 */
	public EventCounter<A, E> add(EventCounter<? extends A, ? extends E> other) {
		if (other != null) {
			other.actorEventCounts.forEach((actor, otherEventCounts) -> this.add(actor, otherEventCounts));
		}
		return this;
	}

	/**
	 * Returns an unmodifiable Set containing the actors of this EventCounter. If
	 * the EventCounter is subsequently modified, the returned Set will not reflect
	 * such modifications.
	 * 
	 * @return A set of current actors of this EventCounter
	 */
	public Set<A> getActors() {
		return Set.copyOf(actorEventCounts.keySet());
	}

	/**
	 * Returns an unmodifiable Set containing the events for the given actor of this
	 * EventCounter. If the EventCounter is subsequently modified, the returned Set
	 * will not reflect such modifications.
	 * 
	 * * @param actor for whom to return the events
	 * 
	 * @return A set of current actors of this EventCounter
	 */

	public Set<E> getEvents(A actor) {
		Map<E, Integer> eventCounts = actorEventCounts.get(actor);
		Set<E> events = (eventCounts == null) ? Set.of() : eventCounts.keySet();
		return Set.copyOf(events);
	}

	/**
	 * Return how many times the given event occurred for the given actor.
	 * 
	 * @param actor for whom to return the count
	 * @param event event for which to return the count.
	 * @return count of events for given actor, or 0 if it never happened.
	 */
	public Integer getCount(A actor, E event) {
		Map<E, Integer> eventCounts = actorEventCounts.get(actor);
		if (eventCounts == null) {
			return 0;
		}
		Integer count = eventCounts.get(event);
		return (count == null) ? 0 : count;
	}

	@Override
	public String toString() {
		return actorEventCounts.toString();
	}

	/**
	 * @return a copy of this EventCounter. If this EventCounter is subsequently
	 *         modified, the returned EventCounter will not reflect such
	 *         modifications (unless Actors A or events E are modified).
	 */
	public EventCounter<A, E> copy() {
		EventCounter<A, E> newEventCounter = new EventCounter<>();
		actorEventCounts.forEach(
				(actor, eventCounts) -> newEventCounter.actorEventCounts.put(actor, new HashMap<>(eventCounts)));
		return newEventCounter;
	}

	/**
	 * For each actor, sum up the score for all their events.
	 * 
	 * @param <AA>        type parameter indicating the type of actor in the
	 *                    EventCounter.
	 * @param <EE>        type of events in the EventCounter.
	 * @param eventCounts per player (actor) the count of finish positions (events).
	 * @param scorer      assigns a score to each event. The total score will be the
	 *                    score multiplied by the number of times each events
	 *                    happens, multiplied by the accuracy divided by the total
	 *                    number of events.
	 * @param accuracy    Factor that the total counts are multiplied by before
	 *                    dividing by total number of event occurrences. Given that
	 *                    we round down to an integer, a higher value will result in
	 *                    more accuracy.
	 * @return a map of player (actor) to normalized score for each player.
	 *         Normalized means total (score*100) / total number of games each
	 *         player played in. This is because each player may not have played the
	 *         same number of rounds in each, and we still need to be able to
	 *         compare the scores.
	 */
	public static <AA, EE> Map<AA, Integer> getNormalizedScores(EventCounter<AA, EE> eventCounts,
			Function<EE, Integer> scorer, int accuracy) {
		Map<AA, Integer> results = new TreeMap<>();
		if (eventCounts == null) {
			return results;
		}
		Set<AA> actors = eventCounts.getActors();
		for (AA actor : actors) {
			double actorScore = 0;
			Set<EE> events = eventCounts.getEvents(actor);
			int actorEventCountTotal = 0;
			for (EE event : events) {
				Integer score = scorer.apply(event);
				int eventCount = eventCounts.getCount(actor, event);
				actorEventCountTotal += eventCount;
				actorScore += eventCount * score;
			}
			actorScore = (actorScore * accuracy) / actorEventCountTotal;
			// Just drop the decimal bits
			results.put(actor, (int) Math.round(actorScore));
		}
		return results;
	}

}
