// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.kv;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "FOO_STORE")
public class Store {

  public Store() {}

  public Store(String key, String value) {
    this.key = key;
    this.value = value;
  }

  @Id private String key;

  private String value;

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Store)) return false;
    Store store = (Store) o;
    return Objects.equals(key, store.key) && Objects.equals(value, store.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }
}
