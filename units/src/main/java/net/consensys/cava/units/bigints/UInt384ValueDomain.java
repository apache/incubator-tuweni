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
package org.apache.tuweni.units.bigints;

import java.util.function.Function;

import com.google.common.collect.DiscreteDomain;

/**
 * A {@link DiscreteDomain} over a {@link UInt384Value}.
 */
public final class UInt384ValueDomain<T extends UInt384Value<T>> extends DiscreteDomain<T> {

  private final T minValue;
  private final T maxValue;

  /**
   * @param ctor The constructor for the {@link UInt384Value} type.
   */
  public UInt384ValueDomain(Function<UInt384, T> ctor) {
    this.minValue = ctor.apply(UInt384.MIN_VALUE);
    this.maxValue = ctor.apply(UInt384.MAX_VALUE);
  }

  @Override
  public T next(T value) {
    return value.add(1);
  }

  @Override
  public T previous(T value) {
    return value.subtract(1);
  }

  @Override
  public long distance(T start, T end) {
    boolean negativeDistance = start.compareTo(end) < 0;
    T distance = negativeDistance ? end.subtract(start) : start.subtract(end);
    if (!distance.fitsLong()) {
      return negativeDistance ? Long.MIN_VALUE : Long.MAX_VALUE;
    }
    long distanceLong = distance.toLong();
    return negativeDistance ? -distanceLong : distanceLong;
  }

  @Override
  public T minValue() {
    return minValue;
  }

  @Override
  public T maxValue() {
    return maxValue;
  }
}
