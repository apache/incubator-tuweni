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
package org.apache.tuweni.eth.genesis;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.eth.AccountState;
import org.apache.tuweni.eth.Address;
import org.apache.tuweni.eth.Block;
import org.apache.tuweni.eth.BlockBody;
import org.apache.tuweni.eth.BlockHeader;
import org.apache.tuweni.eth.Hash;
import org.apache.tuweni.rlp.RLP;
import org.apache.tuweni.trie.MerklePatriciaTrie;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.bigints.UInt64;
import org.apache.tuweni.units.ethereum.Gas;
import org.apache.tuweni.units.ethereum.Wei;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Utility to read genesis config files and translate them to a block.
 */
public class GenesisFile {

  private final Bytes nonce;
  private final UInt256 difficulty;
  private final Hash mixhash;
  private final Address coinbase;
  private final Instant timestamp;
  private final Bytes extraData;
  private final Gas gasLimit;
  private final Map<Address, Wei> allocs;
  private final int chainId;
  private final List<Long> forks;


  public GenesisFile(
      String nonce,
      String difficulty,
      String mixhash,
      String coinbase,
      String timestamp,
      String extraData,
      String gasLimit,
      String parentHash,
      Map<String, String> allocs,
      int chainId,
      List<Long> forks) {
    if (nonce == null) {
      throw new IllegalArgumentException("nonce must be provided");
    }
    if (difficulty == null) {
      throw new IllegalArgumentException("difficulty must be provided");
    }
    if (mixhash == null) {
      throw new IllegalArgumentException("mixhash must be provided");
    }
    if (coinbase == null) {
      throw new IllegalArgumentException("coinbase must be provided");
    }
    if (timestamp == null) {
      throw new IllegalArgumentException("timestamp must be provided");
    }
    if (extraData == null) {
      throw new IllegalArgumentException("extraData must be provided");
    }
    if (gasLimit == null) {
      throw new IllegalArgumentException("gasLimit must be provided");
    }
    this.nonce = Bytes.fromHexStringLenient(nonce);
    this.difficulty = UInt256.fromHexString(difficulty);
    this.mixhash = Hash.fromHexString(mixhash);
    this.coinbase = Address.fromHexString(coinbase);
    this.timestamp = "0x0".equals(timestamp) ? Instant.ofEpochSecond(0)
        : Instant.ofEpochSecond(Bytes.fromHexStringLenient(timestamp).toLong());
    this.extraData = Bytes.fromHexString(extraData);
    this.gasLimit = Gas.valueOf(Bytes.fromHexStringLenient(gasLimit).toLong());
    this.allocs = new HashMap<>();
    for (Map.Entry<String, String> entry : allocs.entrySet()) {
      Address addr = null;
      try {
        addr = Address.fromHexString(entry.getKey());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid address " + entry.getKey(), e);
      }
      Wei value = null;
      if (entry.getValue().startsWith("0x")) {
        try {
          value = Wei.valueOf(UInt256.fromHexString(entry.getValue()));
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("Invalid balance " + entry.getValue(), e);
        }
      } else {
        try {
          value = Wei.valueOf(UInt256.valueOf(new BigInteger(entry.getValue())));
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("Invalid balance " + entry.getValue(), e);
        }
      }
      this.allocs.put(addr, value);
    }
    this.chainId = chainId;
    this.forks = forks;
  }

  public static GenesisFile read(byte[] contents) throws IOException {
    JsonFactory factory = new JsonFactory();
    JsonParser parser = factory.createParser(contents);

    int chainId = 0;
    String nonce = null;
    String difficulty = null;
    String mixhash = null;
    String coinbase = null;
    String timestamp = null;
    String extraData = null;
    String gasLimit = null;
    String parentHash = null;
    Map<String, String> allocs = null;
    Set<Long> collectedForks = new TreeSet<>();
    while (!parser.isClosed()) {
      JsonToken jsonToken = parser.nextToken();
      if (JsonToken.FIELD_NAME.equals(jsonToken)) {
        String fieldName = parser.getCurrentName();

        parser.nextToken();

        if ("nonce".equalsIgnoreCase(fieldName)) {
          nonce = parser.getValueAsString();
        } else if ("difficulty".equalsIgnoreCase(fieldName)) {
          difficulty = parser.getValueAsString();
        } else if ("mixHash".equalsIgnoreCase(fieldName)) {
          mixhash = parser.getValueAsString();
        } else if ("coinbase".equalsIgnoreCase(fieldName)) {
          coinbase = parser.getValueAsString();
        } else if ("gasLimit".equalsIgnoreCase(fieldName)) {
          gasLimit = parser.getValueAsString();
        } else if ("timestamp".equalsIgnoreCase(fieldName)) {
          timestamp = parser.getValueAsString();
        } else if ("extraData".equalsIgnoreCase(fieldName)) {
          extraData = parser.getValueAsString();
        } else if ("parentHash".equalsIgnoreCase(fieldName)) {
          parentHash = parser.getValueAsString();
        } else if ("alloc".equalsIgnoreCase(fieldName)) {
          allocs = readAlloc(parser);
        } else if ("chainId".equalsIgnoreCase(fieldName)) {
          chainId = parser.getValueAsInt();
        } else if (fieldName.contains("Block")) {
          collectedForks.add(parser.getValueAsLong());
        }
      }
    }
    List<Long> forks = new ArrayList<>(collectedForks);
    Collections.sort(forks);
    return new GenesisFile(
        nonce,
        difficulty,
        mixhash,
        coinbase,
        timestamp,
        extraData,
        gasLimit,
        parentHash,
        allocs,
        chainId,
        forks);
  }

  private static Map<String, String> readAlloc(JsonParser parser) throws IOException {
    Map<String, String> allocs = new HashMap<>();
    String name = null;
    String value = null;
    int depth = 1;
    while (!parser.isClosed()) {
      JsonToken jsonToken = parser.nextToken();
      if (JsonToken.FIELD_NAME.equals(jsonToken)) {
        String fieldName = parser.getCurrentName();
        if ("balance".equals(fieldName)) {
          parser.nextToken();
          value = parser.getValueAsString();
          allocs.put(name, value);
          name = null;
        } else {
          if (depth == 1) {
            name = parser.getValueAsString();
          }
        }
      } else if (JsonToken.END_OBJECT.equals(jsonToken)) {
        depth--;
        if (depth == 0) {
          return allocs;
        }
      } else if (JsonToken.START_OBJECT.equals(jsonToken)) {
        depth++;
      }
    }
    return allocs;
  }


  public Block toBlock() {
    Hash emptyListHash = Hash.hash(RLP.encodeList(writer -> {
    }));
    Hash emptyHash = Hash.hash(RLP.encode(writer -> {
      writer.writeValue(Bytes.EMPTY);
    }));
    Hash empty = Hash.hash(Bytes.EMPTY);
    MerklePatriciaTrie<AccountState> stateTree = new MerklePatriciaTrie<>(AccountState::toBytes);

    List<AsyncCompletion> futures = new ArrayList<>();
    for (Map.Entry<Address, Wei> entry : allocs.entrySet()) {
      AccountState accountState = new AccountState(UInt256.ZERO, entry.getValue(), emptyHash, empty);
      futures.add(stateTree.putAsync(Hash.hash(entry.getKey()), accountState));
    }
    try {
      AsyncCompletion.allOf(futures).join(10, TimeUnit.SECONDS);
    } catch (InterruptedException | TimeoutException e) {
      throw new RuntimeException("Creating the account states took more than 10 seconds.");
    }
    return new Block(
        new BlockHeader(
            null,
            emptyListHash,
            coinbase,
            Hash.fromBytes(stateTree.rootHash()),
            emptyHash,
            emptyHash,
            Bytes.wrap(new byte[256]),
            difficulty,
            UInt256.ZERO,
            gasLimit,
            Gas.valueOf(0L),
            timestamp,
            extraData,
            mixhash,
            UInt64.fromBytes(nonce)),
        new BlockBody(new ArrayList<>(), new ArrayList<>()));
  }

  public Map<Address, Wei> getAllocations() {
    return allocs;
  }

  public List<Long> getForks() {
    return forks;
  }

  public int getChainId() {
    return chainId;
  }
}
