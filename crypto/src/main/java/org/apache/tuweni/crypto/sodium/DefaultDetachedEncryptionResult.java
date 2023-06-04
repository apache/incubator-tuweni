// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import org.apache.tuweni.bytes.Bytes;

final class DefaultDetachedEncryptionResult implements DetachedEncryptionResult {

  private final byte[] cipherText;
  private final byte[] mac;

  DefaultDetachedEncryptionResult(byte[] cipherText, byte[] mac) {
    this.cipherText = cipherText;
    this.mac = mac;
  }

  @Override
  public Bytes cipherText() {
    return Bytes.wrap(cipherText);
  }

  @Override
  public byte[] cipherTextArray() {
    return cipherText;
  }

  @Override
  public Bytes mac() {
    return Bytes.wrap(mac);
  }

  @Override
  public byte[] macArray() {
    return mac;
  }
}
