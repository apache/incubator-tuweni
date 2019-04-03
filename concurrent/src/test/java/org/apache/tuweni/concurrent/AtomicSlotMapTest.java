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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class AtomicSlotMapTest {

  @Test
  void shouldUseSlotsIncrementally() {
    AtomicSlotMap<Integer, String> slotMap = AtomicSlotMap.positiveIntegerSlots();

    assertEquals(1, (int) slotMap.add("value"));
    assertEquals(2, (int) slotMap.add("value"));
    assertEquals(3, (int) slotMap.add("value"));
    assertEquals(4, (int) slotMap.add("value"));
  }

  @Test
  void shouldReuseSlotsIncrementally() {
    AtomicSlotMap<Integer, String> slotMap = AtomicSlotMap.positiveIntegerSlots();

    assertEquals(1, (int) slotMap.add("value"));
    assertEquals(2, (int) slotMap.add("value"));
    assertEquals(3, (int) slotMap.add("value"));
    assertEquals(4, (int) slotMap.add("value"));
    slotMap.remove(2);
    slotMap.remove(4);
    assertEquals(2, (int) slotMap.add("value"));
    assertEquals(4, (int) slotMap.add("value"));
  }

  @Test
  void maintainsCountWhenConcurrentlyRemoving() {
    AtomicSlotMap<Integer, String> slotMap = AtomicSlotMap.positiveIntegerSlots();
    int firstSlot = slotMap.add("foo");

    CompletableAsyncResult<String> result = AsyncResult.incomplete();
    AtomicInteger secondSlot = new AtomicInteger();
    slotMap.computeAsync(s -> {
      secondSlot.set(s);
      return result;
    });
    assertEquals(1, slotMap.size());

    slotMap.remove(secondSlot.get());
    assertEquals(1, slotMap.size());

    result.complete("bar");
    assertEquals(1, slotMap.size());

    slotMap.remove(firstSlot);
    assertEquals(0, slotMap.size());
  }

  @Test
  void shouldNotDuplicateSlotsWhileAddingAndRemoving() throws Exception {
    AtomicSlotMap<Integer, String> slotMap = AtomicSlotMap.positiveIntegerSlots();
    Set<Integer> fastSlots = ConcurrentHashMap.newKeySet();
    Set<Integer> slowSlots = ConcurrentHashMap.newKeySet();

    Callable<Void> fastAdders = () -> {
      int slot = slotMap.add("a fast value");
      fastSlots.add(slot);
      return null;
    };

    Callable<Void> slowAdders = () -> {
      CompletableAsyncResult<String> result = AsyncResult.incomplete();
      slotMap.computeAsync(s -> result).thenAccept(slowSlots::add);

      Thread.sleep(10);
      result.complete("a slow value");
      return null;
    };

    Callable<Void> addAndRemovers = () -> {
      int slot = slotMap.add("a value");
      Thread.sleep(5);
      slotMap.remove(slot);
      return null;
    };

    ExecutorService fastPool = Executors.newFixedThreadPool(20);
    ExecutorService slowPool = Executors.newFixedThreadPool(20);
    ExecutorService addAndRemovePool = Executors.newFixedThreadPool(40);
    List<Future<Void>> fastFutures = fastPool.invokeAll(Collections.nCopies(1000, fastAdders));
    List<Future<Void>> slowFutures = slowPool.invokeAll(Collections.nCopies(1000, slowAdders));
    List<Future<Void>> addAndRemoveFutures = addAndRemovePool.invokeAll(Collections.nCopies(2000, addAndRemovers));

    for (Future<Void> future : addAndRemoveFutures) {
      future.get();
    }
    for (Future<Void> future : slowFutures) {
      future.get();
    }
    for (Future<Void> future : fastFutures) {
      future.get();
    }

    assertEquals(1000, fastSlots.size());
    assertEquals(1000, slowSlots.size());
    slowSlots.addAll(fastSlots);
    assertEquals(2000, slowSlots.size());

    assertEquals(2000, slotMap.size());
  }
}
