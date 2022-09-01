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

import static java.util.Objects.requireNonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * A concurrent hash map that stores values along with an expiry.
 *
 * Values are stored in the map until their expiry is reached, after which they will no longer be available and will
 * appear as if removed. The actual removal is done lazily whenever the map is accessed, or when the
 * {@link #purgeExpired()} method is invoked.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public final class ExpiringMap<K, V> implements Map<K, V> {

  // Uses object equality, to ensure uniqueness as a value in the storage map
  private static final class ExpiringEntry<K, V> implements Comparable<ExpiringEntry<K, V>> {
    private K key;
    private V value;
    private long expiry;
    @Nullable
    private BiConsumer<K, V> expiryListener;

    ExpiringEntry(K key, V value, long expiry, @Nullable BiConsumer<K, V> expiryListener) {
      this.key = key;
      this.value = value;
      this.expiry = expiry;
      this.expiryListener = expiryListener;
    }

    @Override
    public int compareTo(ExpiringEntry<K, V> o) {
      return Long.compare(expiry, o.expiry);
    }
  }

  private final ConcurrentHashMap<K, ExpiringEntry<K, V>> storage = new ConcurrentHashMap<>();
  private final PriorityBlockingQueue<ExpiringEntry<K, V>> expiryQueue = new PriorityBlockingQueue<>();
  private final LongSupplier currentTimeSupplier;
  private final Long defaultTimeout;

  /**
   * Construct an empty map.
   */
  public ExpiringMap() {
    this(System::currentTimeMillis, Long.MAX_VALUE);
  }

  /**
   * Construct a map with a default timeout value.
   * 
   * @param defaultTimeout the default timeout in milliseconds
   */
  public ExpiringMap(Long defaultTimeout) {
    this(System::currentTimeMillis, defaultTimeout);
  }

  ExpiringMap(LongSupplier currentTimeSupplier, Long defaultTimeout) {
    this.currentTimeSupplier = currentTimeSupplier;
    this.defaultTimeout = defaultTimeout;
  }

  @Nullable
  @Override
  public V get(Object key) {
    requireNonNull(key);
    purgeExpired();
    ExpiringEntry<K, V> entry = storage.get(key);
    return (entry == null) ? null : entry.value;
  }

  @Override
  public V getOrDefault(Object key, V defaultValue) {
    V v;
    return (((v = get(key)) != null)) ? v : defaultValue;
  }

  @Override
  public boolean containsKey(Object key) {
    requireNonNull(key);
    purgeExpired();
    return storage.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    requireNonNull(value);
    purgeExpired();
    return storage.values().stream().anyMatch(e -> e.value.equals(value));
  }

  @Override
  public int size() {
    purgeExpired();
    return storage.size();
  }

  @Override
  public boolean isEmpty() {
    purgeExpired();
    return storage.isEmpty();
  }

  @Nullable
  @Override
  public V put(K key, V value) {
    requireNonNull(key);
    requireNonNull(value);
    purgeExpired();
    ExpiringEntry<K, V> oldEntry = storage.put(key, new ExpiringEntry<>(key, value, defaultTimeout, null));
    return (oldEntry == null) ? null : oldEntry.value;
  }

  /**
   * Associates the specified value with the specified key in this map, and expires the entry when the specified expiry
   * time is reached. If the map previously contained a mapping for the key, the old value is replaced by the specified
   * value.
   *
   * @param key The key with which the specified value is to be associated.
   * @param value The value to be associated with the specified key.
   * @param expiry The expiry time for the value, in milliseconds since the epoch.
   * @return The previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
   */
  @Nullable
  public V put(K key, V value, long expiry) {
    return put(key, value, expiry, null);
  }

  /**
   * Associates the specified value with the specified key in this map, and expires the entry when the specified expiry
   * time is reached. If the map previously contained a mapping for the key, the old value is replaced by the specified
   * value.
   *
   * @param key The key with which the specified value is to be associated.
   * @param value The value to be associated with the specified key.
   * @param expiry The expiry time for the value, in milliseconds since the epoch.
   * @param expiryListener A listener that will be invoked when the entry expires.
   * @return The previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
   */
  @Nullable
  public V put(K key, V value, long expiry, @Nullable BiConsumer<K, V> expiryListener) {
    requireNonNull(key);
    requireNonNull(value);
    if (expiry >= Long.MAX_VALUE) {
      return put(key, value);
    }

    long now = currentTimeSupplier.getAsLong();
    purgeExpired(now);

    if (expiry <= now) {
      V previous = remove(key);
      if (expiryListener != null) {
        expiryListener.accept(key, value);
      }
      return previous;
    }

    ExpiringEntry<K, V> newEntry = new ExpiringEntry<>(key, value, expiry, expiryListener);
    ExpiringEntry<K, V> oldEntry = storage.put(key, newEntry);
    expiryQueue.offer(newEntry);
    if (oldEntry != null && oldEntry.expiry < Long.MAX_VALUE) {
      expiryQueue.remove(oldEntry);
    }
    return (oldEntry == null) ? null : oldEntry.value;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    requireNonNull(m);
    purgeExpired();
    for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
      storage.put(e.getKey(), new ExpiringEntry<>(e.getKey(), e.getValue(), defaultTimeout, null));
    }
  }

  @Nullable
  @Override
  public V putIfAbsent(K key, V value) {
    requireNonNull(key);
    requireNonNull(value);
    purgeExpired();
    ExpiringEntry<K, V> oldEntry = storage.putIfAbsent(key, new ExpiringEntry<>(key, value, defaultTimeout, null));
    return (oldEntry == null) ? null : oldEntry.value;
  }

  /**
   * If the specified key is not already associated with a value, associates the specified value with the specified key
   * in this map, and expires the entry when the specified expiry time is reached.
   *
   * @param key The key with which the specified value is to be associated.
   * @param value The value to be associated with the specified key.
   * @param expiry The expiry time for the value, in milliseconds since the epoch.
   * @return The previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
   */
  @Nullable
  public V putIfAbsent(K key, V value, long expiry) {
    return putIfAbsent(key, value, expiry, null);
  }

  /**
   * If the specified key is not already associated with a value, associates the specified value with the specified key
   * in this map, and expires the entry when the specified expiry time is reached.
   *
   * @param key The key with which the specified value is to be associated.
   * @param value The value to be associated with the specified key.
   * @param expiry The expiry time for the value, in milliseconds since the epoch.
   * @param expiryListener A listener that will be invoked when the entry expires.
   * @return The previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
   */
  @Nullable
  public V putIfAbsent(K key, V value, long expiry, @Nullable BiConsumer<K, V> expiryListener) {
    requireNonNull(key);
    requireNonNull(value);
    if (expiry >= Long.MAX_VALUE) {
      return put(key, value);
    }

    long now = currentTimeSupplier.getAsLong();
    purgeExpired(now);

    if (expiry <= now) {
      V previous = remove(key);
      if (expiryListener != null) {
        expiryListener.accept(key, value);
      }
      return previous;
    }

    ExpiringEntry<K, V> newEntry = new ExpiringEntry<>(key, value, expiry, expiryListener);
    ExpiringEntry<K, V> oldEntry = storage.putIfAbsent(key, newEntry);
    if (oldEntry == null) {
      expiryQueue.offer(newEntry);
      return null;
    }
    return oldEntry.value;
  }

  @Override
  public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    ExpiringEntry<K, V> newEntry = storage.compute(key, (k, oldEntry) -> {
      if (oldEntry != null && oldEntry.expiry < Long.MAX_VALUE) {
        expiryQueue.remove(oldEntry);
      }
      V oldValue = (oldEntry == null) ? null : oldEntry.value;
      V newValue = remappingFunction.apply(k, oldValue);
      return (newValue == null) ? null : new ExpiringEntry<>(k, newValue, defaultTimeout, null);
    });
    return (newEntry == null) ? null : newEntry.value;
  }

  @Override
  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    return computeIfAbsent(key, defaultTimeout, mappingFunction);
  }

  public V computeIfAbsent(K key, long expiration, Function<? super K, ? extends V> mappingFunction) {
    ExpiringEntry<K, V> newEntry = storage.computeIfAbsent(key, k -> {
      V newValue = mappingFunction.apply(k);
      return (newValue == null) ? null : new ExpiringEntry<>(k, newValue, expiration, null);
    });
    return (newEntry == null) ? null : newEntry.value;
  }

  @Override
  public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    ExpiringEntry<K, V> newEntry = storage.computeIfPresent(key, (k, oldEntry) -> {
      if (oldEntry.expiry < Long.MAX_VALUE) {
        expiryQueue.remove(oldEntry);
      }
      V newValue = remappingFunction.apply(k, oldEntry.value);
      return (newValue == null) ? null : new ExpiringEntry<>(k, newValue, defaultTimeout, null);
    });
    return (newEntry == null) ? null : newEntry.value;
  }

  @Override
  public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    ExpiringEntry<K, V> entry =
        storage.merge(key, new ExpiringEntry<>(key, value, defaultTimeout, null), (oldEntry, newEntry) -> {
          if (oldEntry.expiry < Long.MAX_VALUE) {
            expiryQueue.remove(oldEntry);
          }
          V newValue = remappingFunction.apply(oldEntry.value, newEntry.value);
          return (newValue == null) ? null : new ExpiringEntry<>(key, newValue, defaultTimeout, null);
        });
    return (entry == null) ? null : entry.value;
  }

  @Override
  public V replace(K key, V value) {
    ExpiringEntry<K, V> oldEntry = storage.replace(key, new ExpiringEntry<>(key, value, defaultTimeout, null));
    if (oldEntry != null) {
      if (oldEntry.expiry < Long.MAX_VALUE) {
        expiryQueue.remove(oldEntry);
      }
      return oldEntry.value;
    }
    return null;
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    requireNonNull(oldValue);
    requireNonNull(newValue);
    ExpiringEntry<K, V> entry = storage.computeIfPresent(key, (k, oldEntry) -> {
      if (oldEntry.value.equals(oldValue)) {
        if (oldEntry.expiry < Long.MAX_VALUE) {
          expiryQueue.remove(oldEntry);
        }
        return new ExpiringEntry<>(k, newValue, defaultTimeout, null);
      }
      return oldEntry;
    });
    return (entry != null) && entry.value.equals(newValue);
  }

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    storage.replaceAll((k, oldEntry) -> {
      if (oldEntry.expiry < Long.MAX_VALUE) {
        expiryQueue.remove(oldEntry);
      }
      return new ExpiringEntry<>(k, requireNonNull(function.apply(k, oldEntry.value)), defaultTimeout, null);
    });
  }

  @Override
  public V remove(Object key) {
    requireNonNull(key);
    purgeExpired();
    ExpiringEntry<K, V> entry = storage.remove(key);
    if (entry == null) {
      return null;
    }
    if (entry.expiry < Long.MAX_VALUE) {
      expiryQueue.remove(entry);
    }
    return entry.value;
  }

  @Override
  public boolean remove(Object key, Object value) {
    requireNonNull(key);
    requireNonNull(value);
    purgeExpired();
    ExpiringEntry<K, V> entry = storage.get(key);
    if (entry == null || !value.equals(entry.value)) {
      return false;
    }
    if (!storage.remove(key, entry)) {
      return false;
    }
    if (entry.expiry < Long.MAX_VALUE) {
      expiryQueue.remove(entry);
    }
    return true;
  }

  @Override
  public void clear() {
    expiryQueue.clear();
    storage.clear();
  }

  @Override
  public Set<K> keySet() {
    purgeExpired();
    return storage.keySet();
  }

  @Override
  public Collection<V> values() {
    purgeExpired();
    return storage.values().stream().map(e -> e.value).collect(Collectors.toList());
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    purgeExpired();
    return storage.entrySet().stream().map(e -> new Map.Entry<K, V>() {
      @Override
      public K getKey() {
        return e.getKey();
      }

      @Override
      public V getValue() {
        return e.getValue().value;
      }

      @Override
      public V setValue(V value) {
        throw new UnsupportedOperationException();
      }
    }).collect(Collectors.toSet());
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    storage.forEach((k, v) -> action.accept(k, v.value));
  }

  /**
   * Force immediate expiration of any key/value pairs that have reached their expiry.
   *
   * @return The earliest expiry time for the current entries in the map (in milliseconds since the epoch), or
   *         {@code Long.MAX_VALUE} if there are no entries due to expire.
   */
  public long purgeExpired() {
    return purgeExpired(currentTimeSupplier.getAsLong());
  }

  private long purgeExpired(long oldest) {
    ExpiringEntry<K, V> entry;
    while ((entry = expiryQueue.peek()) != null && entry.expiry <= oldest) {
      if (!expiryQueue.remove(entry) || !storage.remove(entry.key, entry)) {
        continue;
      }
      if (entry.expiryListener != null) {
        entry.expiryListener.accept(entry.key, entry.value);
      }
    }
    return entry == null ? Long.MAX_VALUE : entry.expiry;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ExpiringMap)) {
      return false;
    }
    ExpiringMap other = (ExpiringMap) obj;
    return storage.equals(other.storage);
  }

  @Override
  public int hashCode() {
    return storage.hashCode();
  }
}
