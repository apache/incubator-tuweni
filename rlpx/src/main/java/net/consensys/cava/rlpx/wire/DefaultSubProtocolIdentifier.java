/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.rlpx.wire;

/**
 * Default implementation of a sub protocol identifier
 */
final class DefaultSubProtocolIdentifier implements SubProtocolIdentifier {

  private final String name;
  private final int version;

  public DefaultSubProtocolIdentifier(String name, int version) {
    this.name = name;
    this.version = version;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public int version() {
    return version;
  }
}
