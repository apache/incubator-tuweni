// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ssz;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SSZVariableSizeTypeList<T extends SSZReadable & SSZWritable>
    implements SSZReadable, SSZWritable {
  private final Supplier<T> supplier;

  private final List<T> elements = new ArrayList<>();

  public SSZVariableSizeTypeList(Supplier<T> supplier) {
    this.supplier = supplier;
  }

  @Override
  public boolean isFixed() {
    return false;
  }

  @Override
  public void populateFromReader(SSZReader reader) {
    elements.addAll(reader.readVariableSizeTypeList(supplier));
  }

  @Override
  public void writeTo(SSZWriter writer) {
    writer.writeTypedList(elements);
  }

  public List<T> getElements() {
    return elements;
  }
}
