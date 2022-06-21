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
