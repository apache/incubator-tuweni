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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import javax.annotation.Nullable;

/**
 * A concurrent hash set that stores values along with an expiry.
 *
 * Elements are stored in the set until their expiry is reached, after which they will no longer be available and will
 * appear as if removed. The actual removal is done lazily whenever the set is accessed, or when the
 * {@link #purgeExpired()} method is invoked.
 *
 * @param <E> The element type.
 */
public final class ExpiringSet<E> implements Set<E> {

  // Uses object equality, to ensure uniqueness as a value in the storage map
  private static final class ExpiringEntry<E> implements Comparable<ExpiringEntry<E>> {
    private E element;
    private long expiry;
    @Nullable
    private Consumer<E> expiryListener;

    ExpiringEntry(E element, long expiry, @Nullable Consumer<E> expiryListener) {
      this.element = element;
      this.expiry = expiry;
      this.expiryListener = expiryListener;
    }

    @Override
    public int compareTo(ExpiringEntry<E> o) {
      return Long.compare(expiry, o.expiry);
    }
  }

  private final ConcurrentHashMap<E, ExpiringEntry<E>> storage = new ConcurrentHashMap<>();
  private final PriorityBlockingQueue<ExpiringEntry<E>> expiryQueue = new PriorityBlockingQueue<>();
  private final LongSupplier currentTimeSupplier;
  private final long evictionTimeout;

  /**
   * Construct an empty expiring set.
   * 
   * @param evictionTimeout the default eviction timeout for entries in milliseconds.
   */
  public ExpiringSet(long evictionTimeout) {
    this(evictionTimeout, System::currentTimeMillis);
  }

  /**
   * Construct an empty set.
   */
  public ExpiringSet() {
    this(Long.MAX_VALUE, System::currentTimeMillis);
  }

  ExpiringSet(long evictionTimeout, LongSupplier currentTimeSupplier) {
    if (evictionTimeout <= 0) {
      throw new IllegalArgumentException("Invalid eviction timeout " + evictionTimeout);
    }
    this.evictionTimeout = evictionTimeout;
    this.currentTimeSupplier = currentTimeSupplier;
  }

  @Override
  public boolean contains(Object element) {
    purgeExpired();
    return storage.containsKey(element);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    purgeExpired();
    for (Object element : c) {
      if (!storage.containsKey(element)) {
        return false;
      }
    }
    return true;
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

  @Override
  public Iterator<E> iterator() {
    purgeExpired();
    return storage.keySet().iterator();
  }

  @Override
  public Object[] toArray() {
    purgeExpired();
    return storage.keySet().toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    requireNonNull(a);
    purgeExpired();
    return storage.keySet().toArray(a);
  }

  @Override
  public boolean add(E e) {
    requireNonNull(e);
    purgeExpired();
    ExpiringEntry<E> oldEntry =
        storage.put(e, new ExpiringEntry<>(e, currentTimeSupplier.getAsLong() + evictionTimeout, null));
    return oldEntry == null;
  }

  /**
   * Adds the specified element to this set if it is not already present, and expires the entry when the specified
   * expiry time is reached.
   *
   * @param element The element to add to the set.
   * @param expiry The expiry time for the element, in milliseconds since the epoch.
   * @return {@code true} if this set did not already contain the specified element.
   */
  public boolean add(E element, long expiry) {
    return add(element, expiry, null);
  }

  /**
   * Adds the specified element to this set if it is not already present, and expires the entry when the specified
   * expiry time is reached.
   *
   * @param element The element to add to the set.
   * @param expiry The expiry time for the element, in milliseconds since the epoch.
   * @param expiryListener A listener that will be invoked when the entry expires.
   * @return {@code true} if this set did not already contain the specified element.
   */
  public boolean add(E element, long expiry, @Nullable Consumer<E> expiryListener) {
    requireNonNull(element);

    long now = currentTimeSupplier.getAsLong();
    purgeExpired(now);

    if (expiry <= now) {
      boolean removedPrevious = remove(element);
      if (expiryListener != null) {
        expiryListener.accept(element);
      }
      return removedPrevious;
    }

    ExpiringEntry<E> newEntry = new ExpiringEntry<>(element, expiry, expiryListener);
    ExpiringEntry<E> oldEntry = storage.put(element, newEntry);
    expiryQueue.offer(newEntry);
    if (oldEntry != null && oldEntry.expiry < Long.MAX_VALUE) {
      expiryQueue.remove(oldEntry);
    }
    return oldEntry == null;
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    requireNonNull(c);
    purgeExpired();
    boolean noOldElements = true;
    for (E element : c) {
      ExpiringEntry<E> oldEntry = storage.put(element, new ExpiringEntry<>(element, Long.MAX_VALUE, null));
      if (oldEntry != null) {
        noOldElements = false;
      }
    }
    return noOldElements;
  }

  @Override
  public boolean remove(Object element) {
    requireNonNull(element);
    purgeExpired();
    ExpiringEntry<E> entry = storage.remove(element);
    if (entry == null) {
      return false;
    }
    if (entry.expiry < Long.MAX_VALUE) {
      expiryQueue.remove(entry);
    }
    return true;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    requireNonNull(c);
    purgeExpired();
    boolean changed = false;
    for (Object element : c) {
      ExpiringEntry<E> entry = storage.remove(element);
      if (entry != null) {
        if (entry.expiry < Long.MAX_VALUE) {
          expiryQueue.remove(entry);
        }
        changed = true;
      }
    }
    return changed;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    expiryQueue.clear();
    storage.clear();
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
    ExpiringEntry<E> entry;
    while ((entry = expiryQueue.peek()) != null && entry.expiry <= oldest) {
      // only remove if it's still mapped to the same entry (object equality is used)
      if (!expiryQueue.remove(entry) || !storage.remove(entry.element, entry)) {
        continue;
      }
      if (entry.expiryListener != null) {
        entry.expiryListener.accept(entry.element);
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
    if (!(obj instanceof ExpiringSet)) {
      return false;
    }
    ExpiringSet other = (ExpiringSet) obj;
    return storage.equals(other.storage);
  }

  @Override
  public int hashCode() {
    return storage.hashCode();
  }
}
