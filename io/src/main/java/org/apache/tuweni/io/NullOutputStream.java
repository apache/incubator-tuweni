// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.io;

import java.io.OutputStream;

final class NullOutputStream extends OutputStream {
  static final NullOutputStream INSTANCE = new NullOutputStream();

  @Override
  public void write(int b) {
    // do nothing
  }
}
