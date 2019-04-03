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
package org.apache.tuweni.crypto.sodium;

/**
 * Details of a sodium native library version.
 */
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
