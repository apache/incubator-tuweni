// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ssz;

public interface SSZType {
  default boolean isFixed() {
    return true;
  };
}
