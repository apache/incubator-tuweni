// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ssz;

import org.apache.tuweni.bytes.Bytes;

import java.util.ArrayList;
import java.util.List;

final class BytesSSZWriter implements SSZWriter {

  private final List<Bytes> values = new ArrayList<>();

  @Override
  public void writeSSZ(Bytes value) {
    values.add(value);
  }

  Bytes toBytes() {
    if (values.isEmpty()) {
      return Bytes.EMPTY;
    }
    return Bytes.wrap(values.toArray(new Bytes[0]));
  }
}
