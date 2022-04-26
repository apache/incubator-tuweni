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
package org.apache.tuweni.crypto.blake2bf;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;

/**
 * Bouncy Castle Security Provider for specific Apache Tuweni message digests.
 */
public final class TuweniProvider extends Provider {

  private static final String info = "Tuweni Security Provider v1.0";

  public static final String PROVIDER_NAME = "Tuweni";

  @SuppressWarnings({"unchecked", "removal"})
  public TuweniProvider() {
    super(PROVIDER_NAME, "1.0", info);
    AccessController.doPrivileged((PrivilegedAction) () -> {
      put("MessageDigest.Blake2bf", Blake2bfMessageDigest.class.getName());
      return null;
    });
  }
}
