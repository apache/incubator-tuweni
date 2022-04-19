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
package org.apache.tuweni.crypto;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.junit.BouncyCastleExtension;

import java.security.Provider;
import java.security.Security;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(BouncyCastleExtension.class)
class HashTest {

  @Test
  void sha2_256() {
    String horseSha2 = "fd62862b6dc213bee77c2badd6311528253c6cb3107e03c16051aa15584eca1c";
    String cowSha2 = "beb134754910a4b4790c69ab17d3975221f4c534b70c8d6e82b30c165e8c0c09";

    Bytes resultHorse = Hash.sha2_256(Bytes.wrap("horse".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(horseSha2), resultHorse);

    byte[] resultHorse2 = Hash.sha2_256("horse".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(horseSha2).toArray(), resultHorse2);

    Bytes resultCow = Hash.sha2_256(Bytes.wrap("cow".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(cowSha2), resultCow);

    byte[] resultCow2 = Hash.sha2_256("cow".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(cowSha2).toArray(), resultCow2);
  }

  @Test
  void sha2_256_withoutSodium() {
    Hash.USE_SODIUM = false;
    try {
      String horseSha2 = "fd62862b6dc213bee77c2badd6311528253c6cb3107e03c16051aa15584eca1c";
      String cowSha2 = "beb134754910a4b4790c69ab17d3975221f4c534b70c8d6e82b30c165e8c0c09";

      Bytes resultHorse = Hash.sha2_256(Bytes.wrap("horse".getBytes(UTF_8)));
      assertEquals(Bytes.fromHexString(horseSha2), resultHorse);

      byte[] resultHorse2 = Hash.sha2_256("horse".getBytes(UTF_8));
      assertArrayEquals(Bytes.fromHexString(horseSha2).toArray(), resultHorse2);

      Bytes resultCow = Hash.sha2_256(Bytes.wrap("cow".getBytes(UTF_8)));
      assertEquals(Bytes.fromHexString(cowSha2), resultCow);

      byte[] resultCow2 = Hash.sha2_256("cow".getBytes(UTF_8));
      assertArrayEquals(Bytes.fromHexString(cowSha2).toArray(), resultCow2);
    } finally {
      Hash.USE_SODIUM = true;
    }
  }

  @Test
  void sha2_512_256() {
    String horseSha2 = "6d64886cd066b81cf2dcf16ae70e97017d35f2f4ab73c5c5810aaa9ab573dab3";
    String cowSha2 = "7d26bad15e2f266cb4cbe9b1913978cb8a8bd08d92ee157b6be87c92dfce2d3e";

    Bytes resultHorse = Hash.sha2_512_256(Bytes.wrap("horse".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(horseSha2), resultHorse);

    byte[] resultHorse2 = Hash.sha2_512_256("horse".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(horseSha2).toArray(), resultHorse2);

    Bytes resultCow = Hash.sha2_512_256(Bytes.wrap("cow".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(cowSha2), resultCow);

    byte[] resultCow2 = Hash.sha2_512_256("cow".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(cowSha2).toArray(), resultCow2);
  }

  @Test
  void keccak256() {
    String horseKeccak256 = "c87f65ff3f271bf5dc8643484f66b200109caffe4bf98c4cb393dc35740b28c0";
    String cowKeccak256 = "c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4";

    Bytes resultHorse = Hash.keccak256(Bytes.wrap("horse".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(horseKeccak256), resultHorse);

    byte[] resultHorse2 = Hash.keccak256("horse".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(horseKeccak256).toArray(), resultHorse2);

    Bytes resultCow = Hash.keccak256(Bytes.wrap("cow".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(cowKeccak256), resultCow);

    byte[] resultCow2 = Hash.keccak256("cow".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(cowKeccak256).toArray(), resultCow2);
  }

  @Test
  void sha3_256() {
    String horseSha3 = "d8137088d21c7c0d69107cd51d1c32440a57aa5c59f73ed7310522ea491000ac";
    String cowSha3 = "fba26f1556b8c7b473d01e3eae218318f752e808407794fc0b6490988a33a82d";

    Bytes resultHorse = Hash.sha3_256(Bytes.wrap("horse".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(horseSha3), resultHorse);

    byte[] resultHorse2 = Hash.sha3_256("horse".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(horseSha3).toArray(), resultHorse2);

    Bytes resultCow = Hash.sha3_256(Bytes.wrap("cow".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(cowSha3), resultCow);

    byte[] resultCow2 = Hash.sha3_256("cow".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(cowSha3).toArray(), resultCow2);
  }

  @Test
  void sha3_512() {
    String horseSha3 =
        "d78700def5dd85a9f5a1f8cce8614889e696d4dc82b17189e4974acc050659b49494f03cd0bfbb13a32132b4b4af5e16efd8b0643a5453c87e8e6dfb086b3568";
    String cowSha3 =
        "14accdcf3380cd31674aa5edcd2a53f1b1dad3922eb335e89399321e17a8be5ea315b5346a4c45f6a2595b8e2e24bb345daeb97c7ddd2e970b9e53c9ae439f23";

    Bytes resultHorse = Hash.sha3_512(Bytes.wrap("horse".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(horseSha3), resultHorse);

    byte[] resultHorse2 = Hash.sha3_512("horse".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(horseSha3).toArray(), resultHorse2);

    Bytes resultCow = Hash.sha3_512(Bytes.wrap("cow".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(cowSha3), resultCow);

    byte[] resultCow2 = Hash.sha3_512("cow".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(cowSha3).toArray(), resultCow2);
  }

  @Test
  void testWithoutProviders() {
    Provider[] providers = Security.getProviders();
    Stream.of(Security.getProviders()).map(Provider::getName).forEach(Security::removeProvider);
    Hash.USE_SODIUM = false;
    Hash.cachedDigests.get().clear();
    try {
      assertThrows(IllegalStateException.class, () -> Hash.sha2_256("horse".getBytes(UTF_8)));
      assertThrows(IllegalStateException.class, () -> Hash.sha2_256(Bytes.wrap("horse".getBytes(UTF_8))));
      assertThrows(IllegalStateException.class, () -> Hash.sha3_256("horse".getBytes(UTF_8)));
      assertThrows(IllegalStateException.class, () -> Hash.sha3_256(Bytes.wrap("horse".getBytes(UTF_8))));
      assertThrows(IllegalStateException.class, () -> Hash.sha3_512("horse".getBytes(UTF_8)));
      assertThrows(IllegalStateException.class, () -> Hash.sha3_512(Bytes.wrap("horse".getBytes(UTF_8))));
      assertThrows(IllegalStateException.class, () -> Hash.keccak256("horse".getBytes(UTF_8)));
      assertThrows(IllegalStateException.class, () -> Hash.keccak256(Bytes.wrap("horse".getBytes(UTF_8))));
      assertThrows(IllegalStateException.class, () -> Hash.sha2_512_256("horse".getBytes(UTF_8)));
      assertThrows(IllegalStateException.class, () -> Hash.sha2_512_256(Bytes.wrap("horse".getBytes(UTF_8))));
    } finally {
      for (Provider p : providers) {
        Security.addProvider(p);
      }
      Hash.USE_SODIUM = true;
    }
  }
}
