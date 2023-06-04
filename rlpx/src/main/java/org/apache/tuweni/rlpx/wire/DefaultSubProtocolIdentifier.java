// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx.wire;

import java.util.Objects;

/** Default implementation of a sub protocol identifier */
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
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
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
