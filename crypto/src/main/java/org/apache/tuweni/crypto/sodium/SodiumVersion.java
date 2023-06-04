// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

/** Details of a sodium native library version. */
public final class SodiumVersion implements Comparable<SodiumVersion> {
  private final int major;
  private final int minor;
  private final String name;

  SodiumVersion(int major, int minor, String name) {
    this.major = major;
    this.minor = minor;
    this.name = name;
  }

  /**
   * The major version number.
   *
   * @return The major version number.
   */
  public int major() {
    return major;
  }

  /**
   * The minor version number.
   *
   * @return The minor version number.
   */
  public int minor() {
    return minor;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int compareTo(SodiumVersion other) {
    if (this.major == other.major) {
      return Integer.compare(this.minor, other.minor);
    }
    return Integer.compare(this.major, other.major);
  }
}
