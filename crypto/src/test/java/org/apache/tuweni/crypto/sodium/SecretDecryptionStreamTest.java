// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;

import org.junit.jupiter.api.Test;

class SecretDecryptionStreamTest {

  @Test
  void testBytesPull() {
    SecretDecryptionStream stream =
        new SecretDecryptionStream() {
          @Override
          public byte[] pull(byte[] cipherText) {
            return Bytes.fromHexString("deadbeef").toArrayUnsafe();
          }

          @Override
          public boolean isComplete() {
            return false;
          }
        };
    assertEquals(Bytes.fromHexString("deadbeef"), stream.pull(Bytes.EMPTY));
  }
}
