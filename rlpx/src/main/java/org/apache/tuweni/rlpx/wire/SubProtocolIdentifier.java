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
package org.apache.tuweni.rlpx.wire;


import static java.util.Objects.requireNonNull;

/**
 * Identifier of a subprotocol, comprised of a name and version.
 */
public interface SubProtocolIdentifier {

  static SubProtocolIdentifier of(String name, int version) {
    return of(name, version, 0);
  }

  static SubProtocolIdentifier of(String name, int version, int range) {
    requireNonNull(name);
    return new DefaultSubProtocolIdentifier(name, version, range);
  }

  /**
   * Provides the subprotocol name
   * 
   * @return the name of the subprotocol
   */
  String name();

  /**
   * Provides the subprotocol version
   * 
   * @return the version of the subprotocol
   */
  int version();

  /**
   * Provides the length of the range of message types supported by the subprotocol for a given version
   *
   * @return the length of the range of message types supported by the subprotocol for a given version
   */
  int versionRange();
}
