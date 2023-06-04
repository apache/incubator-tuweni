// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.net.tls;

import java.security.KeyStore;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;

import io.netty.handler.ssl.util.SimpleTrustManagerFactory;

final class SingleTrustManagerFactory extends SimpleTrustManagerFactory {

  private final TrustManager[] trustManagers;

  SingleTrustManagerFactory(TrustManager trustManager) {
    this.trustManagers = new TrustManager[] {trustManager};
  }

  @Override
  protected void engineInit(KeyStore keyStore) {}

  @Override
  protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {}

  @Override
  protected TrustManager[] engineGetTrustManagers() {
    return trustManagers;
  }
}
