// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.net.tls;

import java.util.function.Function;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;

final class TrustManagerFactoryWrapper implements TrustOptions {

  private final TrustManagerFactory trustManagerFactory;

  TrustManagerFactoryWrapper(TrustManagerFactory trustManagerFactory) {
    this.trustManagerFactory = trustManagerFactory;
  }

  @Override
  public TrustOptions copy() {
    return clone();
  }

  @Override
  public TrustOptions clone() {
    return new TrustManagerFactoryWrapper(trustManagerFactory);
  }

  @Override
  public TrustManagerFactory getTrustManagerFactory(Vertx vertx) {
    return trustManagerFactory;
  }

  @Override
  public Function<String, TrustManager[]> trustManagerMapper(Vertx vertx) throws Exception {
    return (server) -> trustManagerFactory.getTrustManagers();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof TrustManagerFactoryWrapper)) {
      return false;
    }
    TrustManagerFactoryWrapper other = (TrustManagerFactoryWrapper) obj;
    return trustManagerFactory.equals(other.trustManagerFactory);
  }

  @Override
  public int hashCode() {
    return trustManagerFactory.hashCode();
  }
}
