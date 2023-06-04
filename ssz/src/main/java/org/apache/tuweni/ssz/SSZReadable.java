// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ssz;

public interface SSZReadable extends SSZType {
  void populateFromReader(SSZReader reader);
}
