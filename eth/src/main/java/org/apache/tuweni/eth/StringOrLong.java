// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth;

import java.util.Objects;

/**
 * Class representing the identifier of a JSON-RPC request or response.
 *
 * The identifier can be a string or a number, but not both at the same time.
 */
public class StringOrLong {

  private Long valueAsLong;
  private String valueAsString;

  public StringOrLong(String value) {
    this.valueAsString = value;
  }

  public StringOrLong(Long value) {
    this.valueAsLong = value;
  }

  public String getValueAsString() {
    return valueAsString;
  }

  public Long getValueAsLong() {
    return valueAsLong;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof StringOrLong))
      return false;
    StringOrLong that = (StringOrLong) o;
    return Objects.equals(valueAsLong, that.valueAsLong) || Objects.equals(valueAsString, that.valueAsString);
  }

  @Override
  public int hashCode() {
    return Objects.hash(valueAsLong, valueAsString);
  }

  @Override
  public String toString() {
    return valueAsLong == null ? valueAsString : valueAsLong.toString();
  }
}
