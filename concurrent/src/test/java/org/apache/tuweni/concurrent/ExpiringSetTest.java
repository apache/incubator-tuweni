// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.concurrent;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpiringSetTest {

  private Instant currentTime;
  private ExpiringSet<String> set;

  @BeforeEach
  void setup() {
    currentTime = Instant.now();
    set = new ExpiringSet<>(Long.MAX_VALUE, () -> currentTime.toEpochMilli(), null);
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
  void addGlobalExpiryListener() {
    AtomicReference<String> key = new AtomicReference<>();
    ExpiringSet<String> listeningSet = new ExpiringSet<>(1L, () -> currentTime.toEpochMilli(), key::set);
    listeningSet.add("foo", -1);
    assertEquals("foo", key.get());
    assertEquals(0, listeningSet.size());
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
