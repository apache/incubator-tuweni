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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpiringSetTest {

  private Instant currentTime;
  private ExpiringSet<String> set;

  @BeforeEach
  void setup() {
    currentTime = Instant.now();
    set = new ExpiringSet<>(Long.MAX_VALUE, () -> currentTime.toEpochMilli());
  }

  @Test
  void canAddAndRemoveWithoutExpiry() {
    set.add("foo");
    assertTrue(set.contains("foo"));
    assertEquals(1, set.size());
    assertFalse(set.isEmpty());

    assertTrue(set.remove("foo"));
    assertFalse(set.contains("foo"));
    assertEquals(0, set.size());
    assertTrue(set.isEmpty());

    assertFalse(set.remove("foo"));
  }

  @Test
  void canAddAndRemoveWithExpiry() {
    set.add("foo", currentTime.plusMillis(1).toEpochMilli());
    assertTrue(set.contains("foo"));
    assertEquals(1, set.size());
    assertFalse(set.isEmpty());

    assertTrue(set.remove("foo"));
    assertFalse(set.contains("foo"));
    assertEquals(0, set.size());
    assertTrue(set.isEmpty());

    assertFalse(set.remove("foo"));
  }

  @Test
  void itemIsMissingAfterExpiry() {
    Instant futureTime = currentTime.plusSeconds(10);
    set.add("foo", futureTime.toEpochMilli());
    assertTrue(set.contains("foo"));
    currentTime = futureTime;
    assertFalse(set.contains("foo"));
  }

  @Test
  void addingExpiredItemRemovesExisting() {
    set.add("foo");
    assertTrue(set.add("foo", 0));
    assertFalse(set.contains("foo"));
  }

  @Test
  void doesNotExpireItemThatWasReplaced() {
    Instant futureTime = currentTime.plusSeconds(10);
    set.add("foo", futureTime.toEpochMilli());
    set.add("foo", futureTime.plusSeconds(1).toEpochMilli());
    currentTime = futureTime;
    assertTrue(set.contains("foo"));
  }

  @Test
  void shouldReturnNextExpiryTimeWhenPurging() {
    Instant futureTime1 = currentTime.plusSeconds(15);
    Instant futureTime2 = currentTime.plusSeconds(12);
    Instant futureTime3 = currentTime.plusSeconds(10);
    set.add("foo", futureTime1.toEpochMilli());
    set.add("bar", futureTime2.toEpochMilli());
    set.add("baz", futureTime3.toEpochMilli());
    currentTime = futureTime3;
    assertEquals(futureTime2.toEpochMilli(), set.purgeExpired());
    currentTime = futureTime2;
    assertEquals(futureTime1.toEpochMilli(), set.purgeExpired());
    currentTime = futureTime1;
    assertEquals(Long.MAX_VALUE, set.purgeExpired());
  }

  @Test
  void shouldCallExpiryListener() {
    AtomicBoolean removed1 = new AtomicBoolean(false);
    AtomicBoolean removed2 = new AtomicBoolean(false);
    Instant futureTime = currentTime.plusSeconds(15);
    set.add("foo", currentTime.toEpochMilli(), e -> removed1.set(true));
    assertTrue(removed1.get());
    set.add("bar", futureTime.toEpochMilli(), e -> removed2.set(true));
    assertFalse(removed2.get());
    currentTime = futureTime;
    set.purgeExpired();
    assertTrue(removed2.get());
  }
}
