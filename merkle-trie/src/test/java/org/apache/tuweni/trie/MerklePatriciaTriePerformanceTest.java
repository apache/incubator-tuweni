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
package org.apache.tuweni.trie;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.concurrent.CompletableAsyncCompletion;
import org.apache.tuweni.junit.BouncyCastleExtension;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class MerklePatriciaTriePerformanceTest {

  private static final SecureRandom secureRandom = new SecureRandom();

  private Bytes createRandomBytes() {
    Bytes bytes = Bytes.wrap(new byte[32]);
    secureRandom.nextBytes(bytes.toArrayUnsafe());
    return bytes;
  }

  @Test
  @Disabled("Expensive test worth running on a developer machine")
  void insertOneMillionRecords() throws Exception {
    ExecutorService threadPool = Executors.newFixedThreadPool(16);

    Map<Bytes32, Bytes> storage = new ConcurrentHashMap<>();
    AsyncMerkleStorage merkleStorage = new AsyncMerkleStorage() {
      @Override
      public @NotNull AsyncResult<Bytes> getAsync(@NotNull Bytes32 hash) {
        return AsyncResult.completed(storage.get(hash));
      }

      @Override
      public @NotNull AsyncCompletion putAsync(@NotNull Bytes32 hash, @NotNull Bytes content) {
        CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
        threadPool.submit(() -> {
          storage.put(hash, content);
          completion.complete();
        });
        return completion;
      }
    };

    StoredMerklePatriciaTrie<String> trie = StoredMerklePatriciaTrie.storingStrings(merkleStorage);
    List<Bytes> allKeys = new ArrayList<>();
    long beforeInsertion = System.nanoTime();
    AsyncCompletion.allOf(IntStream.range(0, 1000000).mapToObj(i -> {
      Bytes key = createRandomBytes();
      allKeys.add(key);
      AsyncCompletion completion = trie.putAsync(key, UUID.randomUUID().toString());
      if (i % 1000 == 0) {
        return completion
            .thenRun(
                () -> System.out
                    .println(
                        String.format("%020d", (System.nanoTime() - beforeInsertion) / 1000)
                            + " ms Record #"
                            + i
                            + " ingested"));
      } else {
        return completion;
      }
    })).join(2, TimeUnit.MINUTES);
    long afterInsertion = System.nanoTime();
    System.out.println("Insertion of records done in " + (afterInsertion - beforeInsertion) + " ns");

    long recordsRead = System.nanoTime();

    for (int i = 0; i < allKeys.size(); i += 10) {
      trie.getAsync(allKeys.get(i)).get(1, TimeUnit.SECONDS);
      if (i % 100 == 0) {
        System.out.println("Read 100 records in " + (System.nanoTime() - recordsRead) + " ns");
        recordsRead = System.nanoTime();
      }
    }
  }
}
