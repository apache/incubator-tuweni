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

import static org.junit.jupiter.api.Assertions.fail;

import org.apache.tuweni.crypto.sodium.Signature;

import java.io.IOException;

public class TestConfig {

  private final String host;
  private final int port;
  private final Signature.KeyPair keyPair;

  public TestConfig(String host, int port, Signature.KeyPair keyPair) {
    this.host = host;
    this.port = port;
    this.keyPair = keyPair;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public Signature.KeyPair getKeyPair() {
    return keyPair;
  }

  public static TestConfig fromEnvironment() throws IOException {
    String keyPath = System.getenv("ssb_keypath");
    String host = System.getenv("ssb_host");
    String portString = System.getenv("ssb_port");

    if (keyPath == null || host == null || portString == null) {
      fail("Expected ssb_keypath, ssb_host and ssb_port parameters.");
      return null;
    } else {
      int port = Integer.parseInt(portString);
      Signature.KeyPair keyPair = KeyFileLoader.loadKeysFromFile(keyPath);
      return new org.apache.tuweni.scuttlebutt.lib.TestConfig(host, port, keyPair);
    }
  }

}
