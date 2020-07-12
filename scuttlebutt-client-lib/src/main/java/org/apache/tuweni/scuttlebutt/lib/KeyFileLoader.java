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
package org.apache.tuweni.scuttlebutt.lib;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.io.Base64;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for loading scuttlebutt keys from the file system.
 */
public class KeyFileLoader {

  private KeyFileLoader() {}

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Attempts to load the keys from the default scuttlebutt directory (~/.ssb), and throws an exception if the keys are
   * not available at the given path
   *
   * @return the scuttlebutt key pair
   */
  public static Signature.KeyPair getLocalKeys(Path ssbFolder) {

    Path secretPath = ssbFolder.resolve("secret");

    if (!secretPath.toFile().exists()) {
      throw new IllegalArgumentException("Secret file does not exist");
    }

    if (!secretPath.toFile().canRead()) {
      throw new IllegalArgumentException("Secret file cannot be read");
    }

    return loadKeysFromFile(secretPath);
  }

  /**
   * Attempts to load the scuttlebutt secret key with the supplied file path
   *
   * @param secretPath the filepath to the scuttlebutt secret key to load
   * @return the scuttlebutt key pair
   */
  public static Signature.KeyPair loadKeysFromFile(Path secretPath) {
    try {
      Scanner s = new Scanner(secretPath.toFile(), UTF_8.name());

      s.useDelimiter("\n");

      ArrayList<String> list = new ArrayList<String>();
      while (s.hasNext()) {
        String next = s.next();

        // Filter out the comment lines
        if (!next.startsWith("#")) {
          list.add(next);
        }
      }
      String secretJSON = String.join("", list);

      HashMap<String, String> values = objectMapper.readValue(secretJSON, new TypeReference<Map<String, String>>() {});
      String pubKey = values.get("public").replace(".ed25519", "");
      String privateKey = values.get("private").replace(".ed25519", "");

      Bytes pubKeyBytes = Base64.decode(pubKey);
      Bytes privKeyBytes = Base64.decode(privateKey);

      Signature.PublicKey pub = Signature.PublicKey.fromBytes(pubKeyBytes);
      Signature.SecretKey secretKey = Signature.SecretKey.fromBytes(privKeyBytes);

      return new Signature.KeyPair(pub, secretKey);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
