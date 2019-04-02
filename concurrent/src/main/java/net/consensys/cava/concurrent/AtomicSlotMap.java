/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.concurrent;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import com.google.common.collect.DiscreteDomain;

/**
 * An atomic map that locates available keys within a {@link DiscreteDomain}.
 *
 * <p>
 * This is an atomic map that will allocate key slots based on availability. It will attempt to keep the range compact
 * by filling slots as they become available.
 * <p>
 * This implementation should be used with small sets, as addition is an O(N) operation.
 *
 * @param <K> The type of the map keys.
 * @param <V> The type of values to store in the map.
 */
@SuppressWarnings("rawtypes") // allow ungenerified Comparable types
public final class AtomicSlotMap<K extends Comparable, V> {
  private final DiscreteDomain<K> domain;
  private final ConcurrentHashMap<K, Optional<V>> slots = new ConcurrentHashMap<>();
  private final AtomicInteger size = new AtomicInteger(0);

  /**
   * Create a slot map over the range of integers &gt; 0.
   *
   * @param <V> The type of values to store in the map.
   * @return A new slot map.
   */
  public static <V> AtomicSlotMap<Integer, V> positiveIntegerSlots() {
    return new AtomicSlotMap<>(PositiveIntegerDomain.INSTANCE);
  }

  /**
   * Create a slot map over the provided domain.
   *
   * @param domain The {@link DiscreteDomain} that defines the slots to be used.
   */
  public AtomicSlotMap(DiscreteDomain<K> domain) {
    requireNonNull(domain);
    this.domain = domain;
  }

  /**
   * Add a value to the slot map, using the first available slot.
   *
   * @param value The value.
   * @return The slot that was used to store the value.
   */
  public K add(V value) {
    requireNonNull(value);
    K slot = domain.minValue();
    Optional<V> storedValue = Optional.of(value);
    while (slots.containsKey(slot) || slots.putIfAbsent(slot, storedValue) != null) {
      slot = domain.next(slot);
    }
    size.incrementAndGet();
    return slot;
  }

  /**
   * Put a value into a specific slot.
   *
   * @param slot The slot to put the value in.
   * @param value The value.
   * @return The previous value in the slot, if present.
   */
  @Nullable
  public V put(K slot, V value) {
    requireNonNull(slot);
    requireNonNull(value);
    Optional<V> previous = slots.put(slot, Optional.of(value));
    if (previous == null || !previous.isPresent()) {
      size.incrementAndGet();
      return null;
    }
    return previous.get();
  }

  /**
   * Find a slot and compute a value for it.
   *
   * @param fn A function to compute the value for a slot.
   * @return The slot for which the value was computed.
   */
  public K compute(Function<? super K, ? extends V> fn) {
    requireNonNull(fn);
    K slot = domain.minValue();
    // store an empty optional to prevent contention on the slot, then replace with computed value.
    Optional<V> placeholder = Optional.empty();
    while (slots.containsKey(slot) || slots.putIfAbsent(slot, placeholder) != null) {
      slot = domain.next(slot);
    }
    try {
      if (slots.replace(slot, placeholder, Optional.of(fn.apply(slot)))) {
        size.incrementAndGet();
      }
      return slot;
    } catch (Throwable ex) {
      slots.remove(slot, placeholder);
      throw ex;
    }
  }

  /**
   * Find a slot and compute a value for it.
   *
   * @param fn A function to compute the value for a slot.
   * @return A result that will complete with the slot for which the value was computed.
   */
  public AsyncResult<K> computeAsync(Function<? super K, AsyncResult<? extends V>> fn) {
    requireNonNull(fn);
    K slot = domain.minValue();
    // store an empty optional to prevent contention on the slot, then replace with computed value.
    Optional<V> placeholder = Optional.empty();
    while (slots.containsKey(slot) || slots.putIfAbsent(slot, placeholder) != null) {
      slot = domain.next(slot);
    }
    K finalSlot = slot;
    try {
      return fn.apply(finalSlot).thenApply(value -> {
        if (slots.replace(finalSlot, placeholder, Optional.of(value))) {
          size.incrementAndGet();
        }
        return finalSlot;
      });
    } catch (Throwable ex) {
      slots.remove(finalSlot, placeholder);
      throw ex;
    }
  }

  /**
   * Get the value in a slot.
   *
   * @param slot The slot.
   * @return The value, if present.
   */
  @Nullable
  public V get(K slot) {
    requireNonNull(slot);
    Optional<V> value = slots.get(slot);
    if (value == null) {
      return null;
    }
    return value.orElse(null);
  }

  /**
   * Remove a value from a slot, making the slot available again.
   *
   * @param slot The slot.
   * @return The value that was in the slot, if any.
   */
  @Nullable
  public V remove(K slot) {
    requireNonNull(slot);
    Optional<V> previous = slots.remove(slot);
    if (previous == null || !previous.isPresent()) {
      return null;
    }
    size.decrementAndGet();
    return previous.get();
  }

  /**
   * @return The number of slots filled.
   */
  public int size() {
    return size.get();
  }

  /**
   * @return A stream over the entries in the slot map.
   */
  public Stream<Map.Entry<K, V>> entries() {
    return slots.entrySet().stream().filter(e -> e.getValue().isPresent()).map(e -> new Map.Entry<K, V>() {
      @Override
      public K getKey() {
        return e.getKey();
      }

      @Override
      public V getValue() {
        return e.getValue().get();
      }

      @Override
      public V setValue(Object value) {
        throw new UnsupportedOperationException();
      }
    });
  }

  /**
   * @return A stream over the values stored in the slot map.
   */
  public Stream<V> values() {
    return slots.values().stream().filter(Optional::isPresent).map(Optional::get);
  }

  private static final class PositiveIntegerDomain extends DiscreteDomain<Integer> {
    private static final PositiveIntegerDomain INSTANCE = new PositiveIntegerDomain();

    @Override
    public Integer next(Integer value) {
      int i = value;
      return (i == Integer.MAX_VALUE) ? null : i + 1;
    }

    @Override
    public Integer previous(Integer value) {
      int i = value;
      return (i == 1) ? null : i - 1;
    }

    @Override
    public long distance(Integer start, Integer end) {
      return (long) end - start;
    }

    @Override
    public Integer minValue() {
      return 1;
    }

    @Override
    public Integer maxValue() {
      return Integer.MAX_VALUE;
    }
  }
}
