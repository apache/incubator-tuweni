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
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.rlp.RLP;
import org.apache.tuweni.rlp.RLPException;
import org.apache.tuweni.rlp.RLPReader;
import org.apache.tuweni.rlp.RLPWriter;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.ethereum.Gas;
import org.apache.tuweni.units.ethereum.Wei;

import java.math.BigInteger;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

/**
 * An Ethereum transaction.
 */
public final class Transaction {

  // The base of the signature v-value
  private static final int V_BASE = 27;

  /**
   * Deserialize a transaction from RLP encoded bytes.
   *
   * @param encoded The RLP encoded transaction.
   * @return The de-serialized transaction.
   * @throws RLPException If there is an error decoding the transaction.
   */
  public static Transaction fromBytes(Bytes encoded) {
    return fromBytes(encoded, false);
  }

  /**
   * Deserialize a transaction from RLP encoded bytes.
   *
   * @param encoded The RLP encoded transaction.
   * @param lenient If {@code true}, the RLP decoding will be lenient toward any non-minimal encoding.
   * @return The de-serialized transaction.
   * @throws RLPException If there is an error decoding the transaction.
   */
  public static Transaction fromBytes(Bytes encoded, boolean lenient) {
    requireNonNull(encoded);
    return RLP.decode(encoded, lenient, (reader) -> {
      Transaction tx = reader.readList(Transaction::readFrom);
      if (!reader.isComplete()) {
        throw new RLPException("Additional bytes present at the end of the encoded transaction");
      }
      return tx;
    });
  }

  /**
   * Deserialize a transaction from an RLP input.
   *
   * @param reader The RLP reader.
   * @return The de-serialized transaction.
   * @throws RLPException If there is an error decoding the transaction.
   */
  public static Transaction readFrom(RLPReader reader) {
    UInt256 nonce = reader.readUInt256();
    Wei gasPrice = Wei.valueOf(reader.readUInt256());
    Gas gasLimit = Gas.valueOf(reader.readLong());
    Bytes addressBytes = reader.readValue();
    Address address;
    try {
      address = addressBytes.isEmpty() ? null : Address.fromBytes(addressBytes);
    } catch (IllegalArgumentException e) {
      throw new RLPException("Value is the wrong size to be an address", e);
    }
    Wei value = Wei.valueOf(reader.readUInt256());
    Bytes payload = reader.readValue();
    int encodedV = reader.readInt();
    Bytes rbytes = reader.readValue();
    if (rbytes.size() > 32) {
      throw new RLPException("r-value of the signature is " + rbytes.size() + ", it should be at most 32 bytes");
    }
    BigInteger r = rbytes.toUnsignedBigInteger();
    Bytes sbytes = reader.readValue();
    if (sbytes.size() > 32) {
      throw new RLPException("s-value of the signature is " + sbytes.size() + ", it should be at most 32 bytes");
    }
    BigInteger s = sbytes.toUnsignedBigInteger();
    if (!reader.isComplete()) {
      throw new RLPException("Additional bytes present at the end of the encoding");
    }

    try {
      return fromEncoded(nonce, gasPrice, gasLimit, address, value, payload, r, s, encodedV);
    } catch (IllegalArgumentException e) {
      throw new RLPException(e.getMessage(), e);
    }
  }

  private final UInt256 nonce;
  private final Wei gasPrice;
  private final Gas gasLimit;
  @Nullable
  private final Address to;
  private final Wei value;
  private final SECP256K1.Signature signature;
  private final Bytes payload;
  private final Integer chainId;
  private volatile Hash hash;
  private volatile Address sender;
  private volatile Boolean validSignature;

  /**
   * Create a transaction.
   *
   * @param nonce The transaction nonce.
   * @param gasPrice The transaction gas price.
   * @param gasLimit The transaction gas limit.
   * @param to The target contract address, if any.
   * @param value The amount of Eth to transfer.
   * @param payload The transaction payload.
   * @param keyPair A keypair to generate the transaction signature with.
   */
  public Transaction(
      UInt256 nonce,
      Wei gasPrice,
      Gas gasLimit,
      @Nullable Address to,
      Wei value,
      Bytes payload,
      SECP256K1.KeyPair keyPair) {
    this(nonce, gasPrice, gasLimit, to, value, payload, keyPair, null);
  }

  /**
   * Create a transaction.
   *
   * @param nonce The transaction nonce.
   * @param gasPrice The transaction gas price.
   * @param gasLimit The transaction gas limit.
   * @param to The target contract address, if any.
   * @param value The amount of Eth to transfer.
   * @param payload The transaction payload.
   * @param keyPair A keypair to generate the transaction signature with.
   * @param chainId the chain ID.
   */
  public Transaction(
      UInt256 nonce,
      Wei gasPrice,
      Gas gasLimit,
      @Nullable Address to,
      Wei value,
      Bytes payload,
      SECP256K1.KeyPair keyPair,
      @Nullable Integer chainId) {
    this(
        nonce,
        gasPrice,
        gasLimit,
        to,
        value,
        payload,
        chainId,
        generateSignature(nonce, gasPrice, gasLimit, to, value, payload, chainId, keyPair));
  }

  /**
   * Create a transaction.
   *
   * @param nonce The transaction nonce.
   * @param gasPrice The transaction gas price.
   * @param gasLimit The transaction gas limit.
   * @param to The target contract address, if any.
   * @param value The amount of Eth to transfer.
   * @param payload The transaction payload.
   * @param chainId The chain id.
   * @param signature The transaction signature.
   */
  public Transaction(
      UInt256 nonce,
      Wei gasPrice,
      Gas gasLimit,
      @Nullable Address to,
      Wei value,
      Bytes payload,
      @Nullable Integer chainId,
      SECP256K1.Signature signature) {
    requireNonNull(nonce);
    if (nonce.compareTo(UInt256.ZERO) < 0) {
      throw new IllegalArgumentException("nonce must be >= 0");
    }
    requireNonNull(gasPrice);
    requireNonNull(value);
    requireNonNull(signature);
    requireNonNull(payload);
    this.nonce = nonce;
    this.gasPrice = gasPrice;
    this.gasLimit = gasLimit;
    this.to = to;
    this.value = value;
    this.signature = signature;
    this.payload = payload;
    this.chainId = chainId;
  }

  public static Transaction fromEncoded(
      UInt256 nonce,
      Wei gasPrice,
      Gas gasLimit,
      @Nullable Address to,
      Wei value,
      Bytes payload,
      BigInteger r,
      BigInteger s,
      int encodedV) {
    byte v;
    Integer chainId = null;

    if (encodedV == V_BASE || encodedV == (V_BASE + 1)) {
      v = (byte) (encodedV - V_BASE);
    } else if (encodedV > 35) {
      chainId = (encodedV - 35) / 2;
      v = (byte) (encodedV - (2 * chainId + 35));
    } else {
      throw new RLPException("Invalid v encoded value " + encodedV);
    }

    SECP256K1.Signature signature;
    try {
      signature = SECP256K1.Signature.create(v, r, s);
    } catch (IllegalArgumentException e) {
      throw new RLPException("Invalid signature: " + e.getMessage());
    }
    return new Transaction(nonce, gasPrice, gasLimit, to, value, payload, chainId, signature);
  }

  /**
   * Provides the transaction nonce
   * 
   * @return The transaction nonce.
   */
  public UInt256 getNonce() {
    return nonce;
  }

  /**
   * Provides the gas price
   * 
   * @return The transaction gas price.
   */
  public Wei getGasPrice() {
    return gasPrice;
  }

  /**
   * Provides the gas limit
   * 
   * @return The transaction gas limit.
   */
  public Gas getGasLimit() {
    return gasLimit;
  }

  /**
   * Provides the target contact address.
   * 
   * @return The target contract address, or null if not present.
   */
  @Nullable
  public Address getTo() {
    return to;
  }

  /**
   * Returns true if the transaction creates a contract.
   * 
   * @return {@code true} if the transaction is a contract creation ({@code to} address is {@code null}).
   */
  public boolean isContractCreation() {
    return to == null;
  }

  /**
   * Provides the amount of eth to transfer
   * 
   * @return The amount of Eth to transfer.
   */
  public Wei getValue() {
    return value;
  }

  /**
   * Provides the transaction signature
   * 
   * @return The transaction signature.
   */
  public SECP256K1.Signature getSignature() {
    return signature;
  }

  /**
   * Provides the transaction payload
   * 
   * @return The transaction payload.
   */
  public Bytes getPayload() {
    return payload;
  }

  /**
   * Provides the chain id
   * 
   * @return the chain id of the transaction, or null if no chain id was encoded on the transaction.
   * @see <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-155.md">EIP-155</a>
   */
  public Integer getChainId() {
    return chainId;
  }

  /**
   * Calculate and return the hash for this transaction.
   *
   * @return The hash.
   */
  public Hash getHash() {
    if (hash != null) {
      return hash;
    }
    Bytes rlp = toBytes();
    hash = Hash.hash(rlp);
    return hash;
  }

  /**
   * Provides the sender's address
   * 
   * @return The sender of the transaction, or {@code null} if the signature is invalid.
   */
  @Nullable
  public Address getSender() {
    if (validSignature != null) {
      return sender;
    }
    return verifySignatureAndGetSender();
  }

  /**
   * Extracts the public key of the signature of the transaction. If the transaction is invalid, the public key may be
   * null.
   * 
   * @return the public key of the key pair that signed the transaction.
   */
  @Nullable
  public SECP256K1.PublicKey extractPublicKey() {
    Bytes data = signatureData(nonce, gasPrice, gasLimit, to, value, payload, chainId);
    SECP256K1.PublicKey publicKey = SECP256K1.PublicKey.recoverFromSignature(data, signature);
    return publicKey;
  }

  @Nullable
  private Address verifySignatureAndGetSender() {
    SECP256K1.PublicKey publicKey = extractPublicKey();
    if (publicKey == null) {
      validSignature = false;
    } else {
      sender = Address.fromPublicKey(publicKey);
      validSignature = true;
    }

    return sender;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Transaction)) {
      return false;
    }
    Transaction that = (Transaction) obj;
    return nonce.equals(that.nonce)
        && gasPrice.equals(that.gasPrice)
        && gasLimit.equals(that.gasLimit)
        && Objects.equals(to, that.to)
        && value.equals(that.value)
        && signature.equals(that.signature)
        && payload.equals(that.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nonce, gasPrice, gasLimit, to, value, signature, payload);
  }

  @Override
  public String toString() {
    return String
        .format(
            "Transaction{nonce=%s, gasPrice=%s, gasLimit=%s, to=%s, value=%s, signature=%s, payload=%s",
            nonce,
            gasPrice,
            gasLimit,
            to,
            value,
            signature,
            payload);
  }

  /**
   * Provides the transaction bytes
   * 
   * @return The RLP serialized form of this transaction.
   */
  public Bytes toBytes() {
    return RLP.encodeList(this::writeTo);
  }

  /**
   * Write this transaction to an RLP output.
   *
   * @param writer The RLP writer.
   */
  public void writeTo(RLPWriter writer) {
    writer.writeUInt256(nonce);
    writer.writeUInt256(gasPrice.toUInt256());
    writer.writeLong(gasLimit.toLong());
    writer.writeValue((to != null) ? to : Bytes.EMPTY);
    writer.writeUInt256(value.toUInt256());
    writer.writeValue(payload);
    if (chainId != null) {
      int v = signature.v() + V_BASE + 8 + chainId * 2;
      writer.writeInt(v);
    } else {
      writer.writeInt((int) signature.v() + V_BASE);
    }
    writer.writeBigInteger(signature.r());
    writer.writeBigInteger(signature.s());
  }

  private static SECP256K1.Signature generateSignature(
      UInt256 nonce,
      Wei gasPrice,
      Gas gasLimit,
      @Nullable Address to,
      Wei value,
      Bytes payload,
      @Nullable Integer chainId,
      SECP256K1.KeyPair keyPair) {
    return SECP256K1.sign(signatureData(nonce, gasPrice, gasLimit, to, value, payload, chainId), keyPair);
  }

  public static Bytes signatureData(
      UInt256 nonce,
      Wei gasPrice,
      Gas gasLimit,
      @Nullable Address to,
      Wei value,
      Bytes payload,
      @Nullable Integer chainId) {
    return RLP.encodeList(writer -> {
      writer.writeUInt256(nonce);
      writer.writeValue(gasPrice.toMinimalBytes());
      writer.writeValue(gasLimit.toMinimalBytes());
      writer.writeValue((to != null) ? to : Bytes.EMPTY);
      writer.writeValue(value.toMinimalBytes());
      writer.writeValue(payload);
      if (chainId != null) {
        writer.writeInt(chainId);
        writer.writeUInt256(UInt256.ZERO);
        writer.writeUInt256(UInt256.ZERO);
      }
    });
  }
}
