// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ssz;

public interface SSZWritable extends SSZType {
  void writeTo(SSZWriter writer);
}
