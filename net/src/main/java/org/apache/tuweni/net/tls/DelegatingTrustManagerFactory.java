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
package org.apache.tuweni.net.tls;

import static java.util.Objects.requireNonNull;

import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import io.netty.handler.ssl.util.SimpleTrustManagerFactory;

final class DelegatingTrustManagerFactory extends SimpleTrustManagerFactory {

  private static final X509Certificate[] EMPTY_X509_CERTIFICATES = new X509Certificate[0];

  private final TrustManagerFactory delegate;
  private final X509TrustManager fallback;
  private final TrustManager[] trustManagers;

  DelegatingTrustManagerFactory(TrustManagerFactory delegate, X509TrustManager fallback) {
    requireNonNull(delegate);
    requireNonNull(fallback);
    this.delegate = delegate;
    this.fallback = fallback;
    this.trustManagers = new TrustManager[] {new DelegatingTrustManager()};
  }

  @Override
  protected void engineInit(KeyStore keyStore) throws Exception {
    delegate.init(keyStore);
  }

  @Override
  protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception {
    delegate.init(managerFactoryParameters);
  }

  @Override
  protected TrustManager[] engineGetTrustManagers() {
    return trustManagers;
  }

  private class DelegatingTrustManager extends X509ExtendedTrustManager {

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
        throws CertificateException {
      CertificateException caException = null;
      try {
        for (TrustManager trustManager : delegate.getTrustManagers()) {
          if (trustManager instanceof X509ExtendedTrustManager) {
            ((X509ExtendedTrustManager) trustManager).checkClientTrusted(chain, authType, socket);
            return;
          }
        }
      } catch (CertificateException e) {
        caException = e;
      }

      try {
        if (fallback instanceof X509ExtendedTrustManager) {
          ((X509ExtendedTrustManager) fallback).checkClientTrusted(chain, authType, socket);
        } else {
          fallback.checkClientTrusted(chain, authType);
        }
      } catch (CertificateException e) {
        if (caException != null) {
          e.addSuppressed(caException);
        }
        throw e;
      }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
        throws CertificateException {
      CertificateException caException = null;
      try {
        for (TrustManager trustManager : delegate.getTrustManagers()) {
          if (trustManager instanceof X509ExtendedTrustManager) {
            ((X509ExtendedTrustManager) trustManager).checkServerTrusted(chain, authType, socket);
            return;
          }
        }
      } catch (CertificateException e) {
        caException = e;
      }

      try {
        if (fallback instanceof X509ExtendedTrustManager) {
          ((X509ExtendedTrustManager) fallback).checkServerTrusted(chain, authType, socket);
        } else {
          fallback.checkServerTrusted(chain, authType);
        }
      } catch (CertificateException e) {
        if (caException != null) {
          e.addSuppressed(caException);
        }
        throw e;
      }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
        throws CertificateException {
      CertificateException caException = null;
      try {
        for (TrustManager trustManager : delegate.getTrustManagers()) {
          if (trustManager instanceof X509ExtendedTrustManager) {
            ((X509ExtendedTrustManager) trustManager).checkClientTrusted(chain, authType, engine);
            return;
          }
        }
      } catch (CertificateException e) {
        caException = e;
      }

      try {
        if (fallback instanceof X509ExtendedTrustManager) {
          ((X509ExtendedTrustManager) fallback).checkClientTrusted(chain, authType, engine);
        } else {
          fallback.checkClientTrusted(chain, authType);
        }
      } catch (CertificateException e) {
        if (caException != null) {
          e.addSuppressed(caException);
        }
        throw e;
      }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
        throws CertificateException {
      CertificateException caException = null;
      try {
        for (TrustManager trustManager : delegate.getTrustManagers()) {
          if (trustManager instanceof X509ExtendedTrustManager) {
            ((X509ExtendedTrustManager) trustManager).checkServerTrusted(chain, authType, engine);
            return;
          }
        }
      } catch (CertificateException e) {
        caException = e;
      }

      try {
        if (fallback instanceof X509ExtendedTrustManager) {
          ((X509ExtendedTrustManager) fallback).checkServerTrusted(chain, authType, engine);
        } else {
          fallback.checkServerTrusted(chain, authType);
        }
      } catch (CertificateException e) {
        if (caException != null) {
          e.addSuppressed(caException);
        }
        throw e;
      }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      CertificateException caException = null;
      try {
        for (TrustManager trustManager : delegate.getTrustManagers()) {
          if (trustManager instanceof X509TrustManager) {
            ((X509TrustManager) trustManager).checkClientTrusted(chain, authType);
            return;
          }
        }
      } catch (CertificateException e) {
        caException = e;
      }

      try {
        fallback.checkClientTrusted(chain, authType);
      } catch (CertificateException e) {
        if (caException != null) {
          e.addSuppressed(caException);
        }
        throw e;
      }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      CertificateException caException = null;
      try {
        for (TrustManager trustManager : delegate.getTrustManagers()) {
          if (trustManager instanceof X509TrustManager) {
            ((X509TrustManager) trustManager).checkServerTrusted(chain, authType);
            return;
          }
        }
      } catch (CertificateException e) {
        caException = e;
      }

      try {
        fallback.checkServerTrusted(chain, authType);
      } catch (CertificateException e) {
        if (caException != null) {
          e.addSuppressed(caException);
        }
        throw e;
      }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return EMPTY_X509_CERTIFICATES;
    }
  }
}
