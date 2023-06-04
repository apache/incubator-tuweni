// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ssz;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SSZFixedSizeVector<T extends SSZReadable & SSZWritable> implements SSZReadable, SSZWritable {

  private final int listSize;
  private final int elementSize;
  private final Supplier<T> supplier;

  private final List<T> elements = new ArrayList<>();

  public SSZFixedSizeVector(int listSize, int elementSize, Supplier<T> supplier) {
    this.listSize = listSize;
    this.elementSize = elementSize;
    this.supplier = supplier;
  }

  @Override
  public boolean isFixed() {
    return false;
  }

  @Override
  public void populateFromReader(SSZReader reader) {
    elements.addAll(reader.readTypedVector(listSize, elementSize, supplier));
  }

  @Override
  public void writeTo(SSZWriter writer) {
    writer.writeTypedVector(elements);
  }

  public List<T> getElements() {
    return elements;
  }
}
