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

import static java.lang.String.format;
import static org.apache.tuweni.net.tls.TLS.certificateFingerprint;

import org.apache.tuweni.bytes.Bytes;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;


final class ServerFingerprintTrustManager extends X509ExtendedTrustManager {

  private static final X509Certificate[] EMPTY_X509_CERTIFICATES = new X509Certificate[0];

  static ServerFingerprintTrustManager record(FingerprintRepository repository) {
    return new ServerFingerprintTrustManager(repository, true, true);
  }

  static ServerFingerprintTrustManager tofu(FingerprintRepository repository) {
    return new ServerFingerprintTrustManager(repository, true, false);
  }

  static ServerFingerprintTrustManager allowlist(FingerprintRepository repository) {
    return new ServerFingerprintTrustManager(repository, false, false);
  }

  private final FingerprintRepository repository;
  private final boolean acceptNewFingerprints;
  private final boolean updateFingerprints;

  private ServerFingerprintTrustManager(
      FingerprintRepository repository,
      boolean acceptNewFingerprints,
      boolean updateFingerprints) {
    this.repository = repository;
    this.acceptNewFingerprints = acceptNewFingerprints;
    this.updateFingerprints = updateFingerprints;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
    InetSocketAddress socketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
    checkTrusted(chain, socketAddress.getHostName(), socketAddress.getPort());
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
      throws CertificateException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
      throws CertificateException {
    checkTrusted(chain, engine.getPeerHost(), engine.getPeerPort());
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    throw new UnsupportedOperationException();
  }

  private void checkTrusted(X509Certificate[] chain, String host, int port) throws CertificateException {
    X509Certificate cert = chain[0];
    String identifier = hostIdentifier(host, port);
    Bytes fingerprint = Bytes.wrap(certificateFingerprint(cert));
    if (repository.contains(identifier, fingerprint)) {
      return;
    }

    if (repository.contains(identifier)) {
      if (!updateFingerprints) {
        throw new CertificateException(
            format(
                "Remote host identification has changed!!" + " Certificate for %s (%s) has fingerprint %s",
                identifier,
                cert.getSubjectDN(),
                fingerprint.toHexString().substring(2).toLowerCase()));
      }
    } else if (!acceptNewFingerprints) {
      throw new CertificateException(
          format(
              "Certificate for %s (%s) has unknown fingerprint %s",
              identifier,
              cert.getSubjectDN(),
              fingerprint.toHexString().substring(2).toLowerCase()));
    }

    repository.addFingerprint(identifier, fingerprint);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return EMPTY_X509_CERTIFICATES;
  }

  private String hostIdentifier(String host, int port) {
    return host.trim().toLowerCase() + ":" + port;
  }
}
