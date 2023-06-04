// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx.wire;

import java.util.Objects;

public final class Capability {

  private final String name;

  private final int version;


  Capability(String name, int version) {
    this.name = name;
    this.version = version;
  }

  public String name() {
    return name;
  }

  public int version() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Capability that = (Capability) o;
    return Objects.equals(name, that.name) && version == that.version;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version);
  }

  @Override
  public String toString() {
    return "Capability{" + "name='" + name + '\'' + ", version='" + version + '\'' + '}';
  }
}
