/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpiringMapTest {

  private Instant currentTime;
  private ExpiringMap<Integer, String> map;

  @BeforeEach
  void setup() {
    currentTime = Instant.now();
    map = new ExpiringMap<>(() -> currentTime.toEpochMilli(), Long.MAX_VALUE);
  }

  @Test
  void canAddAndRemoveWithoutExpiry() {
    map.put(1, "foo");
    assertTrue(map.containsKey(1));
    assertTrue(map.containsValue("foo"));
    assertEquals("foo", map.get(1));
    assertEquals(1, map.size());
    assertFalse(map.isEmpty());

    String removed = map.remove(1);
    assertEquals("foo", removed);
    assertFalse(map.containsKey(1));
    assertFalse(map.containsValue("foo"));
    assertNull(map.get(1));
    assertEquals(0, map.size());
    assertTrue(map.isEmpty());

    assertNull(map.remove(1));
  }

  @Test
  void canAddAndRemoveWithExpiry() {
    map.put(1, "foo", currentTime.plusMillis(1).toEpochMilli());
    assertTrue(map.containsKey(1));
    assertTrue(map.containsValue("foo"));
    assertEquals("foo", map.get(1));
    assertEquals(1, map.size());
    assertFalse(map.isEmpty());

    String removed = map.remove(1);
    assertEquals("foo", removed);
    assertFalse(map.containsKey(1));
    assertFalse(map.containsValue("foo"));
    assertNull(map.get(1));
    assertEquals(0, map.size());
    assertTrue(map.isEmpty());

    assertNull(map.remove(1));
  }

  @Test
  void itemIsExpiredAfterExpiry() {
    Instant futureTime = currentTime.plusSeconds(10);
    map.put(1, "foo", futureTime.toEpochMilli());
    assertTrue(map.containsKey(1));
    assertEquals("foo", map.get(1));
    currentTime = futureTime;
    assertFalse(map.containsKey(1));
  }

  @Test
  void itemIsMissingAfterExpiry() {
    Instant futureTime = currentTime.plusSeconds(10);
    map.put(1, "foo", futureTime.toEpochMilli());
    assertTrue(map.containsKey(1));
    assertEquals("foo", map.get(1));
    currentTime = futureTime;
    assertNull(map.get(1));
  }

  @Test
  void addingExpiredItemRemovesExisting() {
    map.put(1, "foo");
    String prev = map.put(1, "bar", 0);
    assertEquals("foo", prev);
    assertFalse(map.containsKey(1));
  }

  @Test
  void doesNotExpireItemThatWasReplaced() {
    Instant futureTime = currentTime.plusSeconds(10);
    map.put(1, "foo", futureTime.toEpochMilli());
    map.put(1, "bar", futureTime.plusSeconds(1).toEpochMilli());
    currentTime = futureTime;
    assertTrue(map.containsKey(1));
    assertEquals("bar", map.get(1));
  }

  @Test
  void shouldReturnNextExpiryTimeWhenPurging() {
    Instant futureTime1 = currentTime.plusSeconds(15);
    Instant futureTime2 = currentTime.plusSeconds(12);
    Instant futureTime3 = currentTime.plusSeconds(10);
    map.put(1, "foo", futureTime1.toEpochMilli());
    map.put(2, "bar", futureTime2.toEpochMilli());
    map.put(3, "baz", futureTime3.toEpochMilli());
    currentTime = futureTime3;
    assertEquals(futureTime2.toEpochMilli(), map.purgeExpired());
    currentTime = futureTime2;
    assertEquals(futureTime1.toEpochMilli(), map.purgeExpired());
    currentTime = futureTime1;
    assertEquals(Long.MAX_VALUE, map.purgeExpired());
  }

  @Test
  void shouldCallExpiryListener() {
    AtomicBoolean removed1 = new AtomicBoolean(false);
    AtomicBoolean removed2 = new AtomicBoolean(false);
    Instant futureTime = currentTime.plusSeconds(15);
    map.put(1, "foo", currentTime.toEpochMilli(), (k, v) -> removed1.set(true));
    assertTrue(removed1.get());
    map.put(2, "bar", futureTime.toEpochMilli(), (k, v) -> removed2.set(true));
    assertFalse(removed2.get());
    currentTime = futureTime;
    map.purgeExpired();
    assertTrue(removed2.get());
  }

  @Test
  void behavesLikeAMap() {
    map.putAll(Collections.singletonMap(1, "foo"));
    assertEquals("foo", map.getOrDefault(1, "bar"));
    assertEquals("bar", map.getOrDefault(2, "bar"));
    assertEquals("foobar", map.compute(1, (v, oldValue) -> oldValue + "bar"));
    assertEquals("foobar", map.getOrDefault(1, "bar"));
    assertEquals("foobar", map.putIfAbsent(1, "foo"));
    assertEquals("foo", map.computeIfAbsent(3, (k) -> "foo"));
    assertEquals("bar", map.computeIfPresent(3, (k, v) -> "bar"));
    assertEquals("bar", map.replace(3, "foo"));
    map.replaceAll((k, v) -> "noes");
    assertEquals("noes", map.get(1));
    map.clear();
    assertEquals(0, map.size());
    assertTrue(map.isEmpty());
    map.putIfAbsent(1, "foo");
    assertEquals(Collections.singleton(1), map.keySet());
    assertEquals(Collections.singletonList("foo"), map.values());
    assertEquals(1, map.entrySet().size());
    AtomicBoolean called = new AtomicBoolean();
    map.forEach((k, v) -> called.set(true));
    assertTrue(called.get());
    assertEquals(new ExpiringMap<Integer, String>(), new ExpiringMap<Integer, String>());
  }

  @Test
  void testUsesDefaultTimeout() throws InterruptedException {
    ExpiringMap<String, String> map = new ExpiringMap<>(10L);
    map.put("foo", "bar");
    Thread.sleep(11);
    assertEquals("bar", map.get("foo"));
  }
}
