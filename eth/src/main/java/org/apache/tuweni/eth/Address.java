/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.eth;

import static java.util.Objects.requireNonNull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.DelegatingBytes;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.rlp.RLP;
import org.apache.tuweni.units.bigints.UInt256;

/**
 * An Ethereum account address.
 */
public final class Address extends DelegatingBytes {

  /**
   * Burn address.
   */
  public static final Address ZERO = Address.fromBytes(Bytes.repeat((byte) 0, 20));

  /**
   * Computes a contract address from an address and a nonce
   * 
   * @param sender the sender's address
   * @param nonce the current sender's nonce
   * @return a contract address
   */
  public static Address fromSenderAndNonce(Address sender, UInt256 nonce) {
    Bytes encoded = RLP.encodeList((writer) -> {
      writer.writeValue(sender);
      writer.writeValue(nonce.toMinimalBytes());
    });
    return Address.fromBytes(Hash.hash(encoded).slice(12));
  }

  /**
   * Derive a contract address from a transaction.
   */
  public static Address fromTransaction(Transaction transaction) {
    if (transaction.getSender() == null) {
      throw new IllegalArgumentException("Invalid transaction signature, cannot recover sender");
    }
    return fromSenderAndNonce(transaction.getSender(), transaction.getNonce());
  }

  /**
   * Transform a public key into an Ethereum address.
   * 
   * @param publicKey the public key
   * @return the address
   */
  public static Address fromPublicKey(SECP256K1.PublicKey publicKey) {
    requireNonNull(publicKey);
    return fromPublicKeyBytes(publicKey.bytes());
  }

  /**
   * Transform a public key into an Ethereum address.
   * 
   * @param bytes the bytes of the public key
   * @return the address
   */
  public static Address fromPublicKeyBytes(Bytes bytes) {
    requireNonNull(bytes);
    Bytes value = org.apache.tuweni.crypto.Hash.keccak256(bytes);
    return new Address(value.slice(12));
  }

  /**
   * Create an address from Bytes.
   *
   * <p>
   * The address must be exactly 20 bytes.
   *
   * @param bytes The bytes for this address.
   * @return An address.
   * @throws IllegalArgumentException If {@code bytes.size() != 20}.
   */
  public static Address fromBytes(Bytes bytes) {
    requireNonNull(bytes);
    if (bytes.size() != SIZE) {
      throw new IllegalArgumentException(String.format("Expected %s bytes but got %s", SIZE, bytes.size()));
    }
    return new Address(bytes);
  }

  /**
   * Parse a hexadecimal string into a {@link Address}.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x", and should encode exactly 20
   *        bytes.
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to va alid hexadecimal representation
   *         containing 20 bytes.
   */
  public static Address fromHexString(String str) {
    return fromBytes(Bytes.fromHexString(str));
  }

  private static final int SIZE = 20;

  private Address(Bytes value) {
    super(value);
  }
}
