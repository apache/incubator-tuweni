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

import java.util.Objects;

/**
 * Default implementation of a sub protocol identifier
 */
final class DefaultSubProtocolIdentifier implements SubProtocolIdentifier {

  private final String name;
  private final int version;
  private final int range;

  DefaultSubProtocolIdentifier(String name, int version, int range) {
    this.name = name;
    this.version = version;
    this.range = range;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public int version() {
    return version;
  }

  @Override
  public int versionRange() {
    return range;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    DefaultSubProtocolIdentifier that = (DefaultSubProtocolIdentifier) o;
    return version == that.version && name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version);
  }

  @Override
  public String toString() {
    return "DefaultSubProtocolIdentifier{" + "name='" + name + '\'' + ", version=" + version + '}';
  }
}
