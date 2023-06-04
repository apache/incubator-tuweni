// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.net.tls;

import java.util.function.Function;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;

final class InsecureTrustOptions implements TrustOptions {

  static InsecureTrustOptions INSTANCE = new InsecureTrustOptions();

  private InsecureTrustOptions() {}

  @Override
  public TrustOptions copy() {
    return clone();
  }

  @Override
  public TrustOptions clone() {
    return this;
  }

  @Override
  public TrustManagerFactory getTrustManagerFactory(Vertx vertx) {
    return InsecureTrustManagerFactory.INSTANCE;
  }

  @Override
  public Function<String, TrustManager[]> trustManagerMapper(Vertx vertx) throws Exception {
    return (server) -> getTrustManagerFactory(vertx).getTrustManagers();
  }
}
