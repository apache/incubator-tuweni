// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ssz;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SSZFixedSizeTypeList<T extends SSZReadable & SSZWritable>
    implements SSZReadable, SSZWritable {

  private final int elementSize;
  private final Supplier<T> supplier;

  private final List<T> elements = new ArrayList<>();

  public SSZFixedSizeTypeList(int elementSize, Supplier<T> supplier) {
    this.elementSize = elementSize;
    this.supplier = supplier;
  }

  // The elements might be fixed, but the overall type is variable
  @Override
  public boolean isFixed() {
    return false;
  }

  @Override
  public void populateFromReader(SSZReader reader) {
    elements.addAll(reader.readFixedTypedList(elementSize, supplier));
  }

  @Override
  public void writeTo(SSZWriter writer) {
    writer.writeTypedList(elements);
  }

  public List<T> getElements() {
    return elements;
  }
}
