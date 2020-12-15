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
package org.apache.tuweni.scuttlebutt.rpc;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.io.Base64;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class Utils {

  static Signature.KeyPair getLocalKeys() throws Exception {
    Map<String, String> env = System.getenv();

    Path secretPath = Paths.get(env.getOrDefault("ssb_dir", "/tmp/ssb"), "secret");
    File file = secretPath.toFile();

    if (!file.exists()) {
      throw new Exception("Secret file does not exist " + secretPath.toAbsolutePath());
    }

    Scanner s = new Scanner(file, UTF_8.name());
    s.useDelimiter("\n");

    ArrayList<String> list = new ArrayList<>();
    while (s.hasNext()) {
      String next = s.next();

      // Filter out the comment lines
      if (!next.startsWith("#")) {
        list.add(next);
      }
    }

    String secretJSON = String.join("", list);

    ObjectMapper mapper = new ObjectMapper();

    Map<String, String> values = mapper.readValue(secretJSON, new TypeReference<>() {
    });
    String pubKey = values.get("public").replace(".ed25519", "");
    String privateKey = values.get("private").replace(".ed25519", "");

    Bytes pubKeyBytes = Base64.decode(pubKey);
    Bytes privKeyBytes = Base64.decode(privateKey);

    Signature.PublicKey pub = Signature.PublicKey.fromBytes(pubKeyBytes);
    Signature.SecretKey secretKey = Signature.SecretKey.fromBytes(privKeyBytes);

    return new Signature.KeyPair(pub, secretKey);
  }
}
