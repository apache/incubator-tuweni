// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.net.tls;

import static java.lang.String.format;
import static org.apache.tuweni.net.tls.TLS.certificateFingerprint;

import org.apache.tuweni.bytes.Bytes;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

final class ClientFingerprintTrustManager extends X509ExtendedTrustManager {

  private static final X509Certificate[] EMPTY_X509_CERTIFICATES = new X509Certificate[0];

  static ClientFingerprintTrustManager record(FingerprintRepository repository) {
    return new ClientFingerprintTrustManager(repository, true, true);
  }

  static ClientFingerprintTrustManager tofa(FingerprintRepository repository) {
    return new ClientFingerprintTrustManager(repository, true, false);
  }

  static ClientFingerprintTrustManager allowlist(FingerprintRepository repository) {
    return new ClientFingerprintTrustManager(repository, false, false);
  }

  private final FingerprintRepository repository;
  private final boolean acceptNewFingerprints;
  private final boolean updateFingerprints;

  private ClientFingerprintTrustManager(
      FingerprintRepository repository, boolean acceptNewFingerprints, boolean updateFingerprints) {
    this.repository = repository;
    this.acceptNewFingerprints = acceptNewFingerprints;
    this.updateFingerprints = updateFingerprints;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
      throws CertificateException {
    X509Certificate cert = chain[0];
    X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
    RDN cn = x500name.getRDNs(BCStyle.CN)[0];
    String hostname = IETFUtils.valueToString(cn.getFirst().getValue());
    checkTrusted(chain, hostname);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
      throws CertificateException {
    X509Certificate cert = chain[0];
    X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
    RDN cn = x500name.getRDNs(BCStyle.CN)[0];
    String hostname = IETFUtils.valueToString(cn.getFirst().getValue());
    checkTrusted(chain, hostname);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) {
    throw new UnsupportedOperationException();
  }

  private void checkTrusted(X509Certificate[] chain, String host) throws CertificateException {
    X509Certificate cert = chain[0];
    Bytes fingerprint = Bytes.wrap(certificateFingerprint(cert));
    if (repository.contains(host, fingerprint)) {
      return;
    }

    if (repository.contains(host)) {
      if (!updateFingerprints) {
        throw new CertificateException(
            format(
                "Client identification has changed!!"
                    + " Certificate for %s (%s) has fingerprint %s",
                host,
                cert.getSubjectDN(),
                fingerprint.toHexString().substring(2).toLowerCase(Locale.ENGLISH)));
      }
    } else if (!acceptNewFingerprints) {
      throw new CertificateException(
          format(
              "Certificate for %s (%s) has unknown fingerprint %s",
              host,
              cert.getSubjectDN(),
              fingerprint.toHexString().substring(2).toLowerCase(Locale.ENGLISH)));
    }

    repository.addFingerprint(host, fingerprint);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return EMPTY_X509_CERTIFICATES;
  }
}
