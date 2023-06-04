// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.plumtree.servlet;

import org.apache.tuweni.plumtree.Peer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;

public class ServletPeer implements Peer {

  private final String serverUrl;

  private final AtomicInteger errorsCounter = new AtomicInteger(0);

  public ServletPeer(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public String getAddress() {
    return serverUrl;
  }

  public AtomicInteger getErrorsCounter() {
    return errorsCounter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ServletPeer))
      return false;
    ServletPeer that = (ServletPeer) o;
    return Objects.equals(serverUrl, that.serverUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serverUrl);
  }

  @Override
  public int compareTo(@NotNull Peer o) {
    return serverUrl.compareTo(((ServletPeer) o).serverUrl);
  }
}
