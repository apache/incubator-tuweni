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

import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.jetbrains.annotations.Nullable;

/**
 * Trust manager factories for fingerprinting clients and servers.
 */
public final class TrustManagerFactories {
  private TrustManagerFactories() {}

  /**
   * Accept all server certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a server presents a CA-signed certificate, the server host+port and the certificate fingerprint will
   * be written to {@code knownServersFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownServersFile The path to a file in which to record fingerprints by host.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordServerFingerprints(Path knownServersFile) {
    requireNonNull(knownServersFile);
    return recordServerFingerprints(new FileBackedFingerprintRepository(knownServersFile));
  }

  /**
   * Accept all server certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a server presents a CA-signed certificate, the server host+port and the certificate fingerprint will
   * be written to {@code knownServersFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param repository The repository in which to record fingerprints by host.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordServerFingerprints(FingerprintRepository repository) {
    requireNonNull(repository);
    return recordServerFingerprints(repository, true);
  }

  /**
   * Accept all server certificates, recording certificate fingerprints.
   *
   * <p>
   * For all connections, the server host+port and the fingerprint of the presented certificate will be written to
   * {@code knownServersFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownServersFile The path to a file in which to record fingerprints by host.
   * @param skipCASigned If {@code true}, CA-signed certificates are not recorded.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordServerFingerprints(Path knownServersFile, boolean skipCASigned) {
    requireNonNull(knownServersFile);
    return recordServerFingerprints(new FileBackedFingerprintRepository(knownServersFile), skipCASigned);
  }

  /**
   * Accept all server certificates, recording certificate fingerprints.
   *
   * <p>
   * For all connections, the server host+port and the fingerprint of the presented certificate will be written to
   * {@code knownServersFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param repository The repository in which to record fingerprints by host.
   * @param skipCASigned If {@code true}, CA-signed certificates are not recorded.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordServerFingerprints(FingerprintRepository repository, boolean skipCASigned) {
    requireNonNull(repository);
    return wrap(ServerFingerprintTrustManager.record(repository), skipCASigned);
  }

  /**
   * Accept all server certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a server presents a CA-signed certificate, the server host+port and the certificate fingerprint will
   * be written to {@code knownServersFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownServersFile The path to a file in which to record fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordServerFingerprints(Path knownServersFile, TrustManagerFactory tmf) {
    requireNonNull(knownServersFile);
    requireNonNull(tmf);
    return recordServerFingerprints(new FileBackedFingerprintRepository(knownServersFile), tmf);
  }

  /**
   * Accept all server certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a server presents a CA-signed certificate, the server host+port and the certificate fingerprint will
   * be written to {@code knownServersFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param repository The repository in which to record fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordServerFingerprints(
      FingerprintRepository repository,
      TrustManagerFactory tmf) {
    requireNonNull(repository);
    requireNonNull(tmf);
    return wrap(ServerFingerprintTrustManager.record(repository), tmf);
  }

  /**
   * Accept CA-signed certificates, and otherwise trust server certificates on first use.
   *
   * <p>
   * Except when a server presents a CA-signed certificate, on first connection to a server (identified by host+port)
   * the fingerprint of the presented certificate will be recorded in {@code knownServersFile}. On subsequent
   * connections, the presented certificate will be matched to the stored fingerprint to ensure it has not changed.
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustServerOnFirstUse(Path knownServersFile) {
    requireNonNull(knownServersFile);
    return trustServerOnFirstUse(new FileBackedFingerprintRepository(knownServersFile));
  }

  /**
   * Accept CA-signed certificates, and otherwise trust server certificates on first use.
   *
   * <p>
   * Except when a server presents a CA-signed certificate, on first connection to a server (identified by host+port)
   * the fingerprint of the presented certificate will be recorded in {@code knownServersFile}. On subsequent
   * connections, the presented certificate will be matched to the stored fingerprint to ensure it has not changed.
   *
   * @param repository The repository in which to record fingerprints by host.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustServerOnFirstUse(FingerprintRepository repository) {
    requireNonNull(repository);
    return trustServerOnFirstUse(repository, true);
  }

  /**
   * Trust server certificates on first use.
   *
   * <p>
   * On first connection to a server (identified by host+port) the fingerprint of the presented certificate will be
   * recorded in {@code knownServersFile}. On subsequent connections, the presented certificate will be matched to the
   * stored fingerprint to ensure it has not changed.
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted (and the fingerprint will not
   *        be recorded).
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustServerOnFirstUse(Path knownServersFile, boolean acceptCASigned) {
    requireNonNull(knownServersFile);
    return trustServerOnFirstUse(new FileBackedFingerprintRepository(knownServersFile), acceptCASigned);
  }

  /**
   * Trust server certificates on first use.
   *
   * <p>
   * On first connection to a server (identified by host+port) the fingerprint of the presented certificate will be
   * recorded in {@code knownServersFile}. On subsequent connections, the presented certificate will be matched to the
   * stored fingerprint to ensure it has not changed.
   *
   * @param repository The repository in which to record fingerprints by host.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted (and the fingerprint will not
   *        be recorded).
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustServerOnFirstUse(FingerprintRepository repository, boolean acceptCASigned) {
    requireNonNull(repository);
    return wrap(ServerFingerprintTrustManager.tofu(repository), acceptCASigned);
  }

  /**
   * Accept CA-signed certificates, and otherwise trust server certificates on first use.
   *
   * <p>
   * Except when a server presents a CA-signed certificate, on first connection to a server (identified by host+port)
   * the fingerprint of the presented certificate will be recorded in {@code knownServersFile}. On subsequent
   * connections, the presented certificate will be matched to the stored fingerprint to ensure it has not changed.
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustServerOnFirstUse(Path knownServersFile, TrustManagerFactory tmf) {
    requireNonNull(knownServersFile);
    requireNonNull(tmf);
    return trustServerOnFirstUse(new FileBackedFingerprintRepository(knownServersFile), tmf);
  }

  /**
   * Accept CA-signed certificates, and otherwise trust server certificates on first use.
   *
   * <p>
   * Except when a server presents a CA-signed certificate, on first connection to a server (identified by host+port)
   * the fingerprint of the presented certificate will be recorded in {@code knownServersFile}. On subsequent
   * connections, the presented certificate will be matched to the stored fingerprint to ensure it has not changed.
   *
   * @param repository The repository in which to record fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustServerOnFirstUse(FingerprintRepository repository, TrustManagerFactory tmf) {
    requireNonNull(repository);
    requireNonNull(tmf);
    return wrap(ServerFingerprintTrustManager.tofu(repository), tmf);
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownServersFile}, associated
   * with the server (identified by host+port).
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory allowlistServers(Path knownServersFile) {
    requireNonNull(knownServersFile);
    return allowlistServers(new FileBackedFingerprintRepository(knownServersFile));
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownServersFile}, associated
   * with the server (identified by host+port).
   *
   * @param repository The repository in which to record fingerprints by host.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory allowlistServers(FingerprintRepository repository) {
    requireNonNull(repository);
    return allowlistServers(repository, true);
  }

  /**
   * Require servers to present known certificates.
   *
   * <p>
   * The fingerprint for a server certificate must be present in the {@code knownServersFile}, associated with the
   * server (identified by host+port).
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory allowlistServers(Path knownServersFile, boolean acceptCASigned) {
    requireNonNull(knownServersFile);
    return allowlistServers(new FileBackedFingerprintRepository(knownServersFile), acceptCASigned);
  }

  /**
   * Require servers to present known certificates.
   *
   * <p>
   * The fingerprint for a server certificate must be present in the {@code knownServersFile}, associated with the
   * server (identified by host+port).
   *
   * @param repository The repository in which to record fingerprints by host.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory allowlistServers(FingerprintRepository repository, boolean acceptCASigned) {
    requireNonNull(repository);
    return wrap(ServerFingerprintTrustManager.allowlist(repository), acceptCASigned);
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownServersFile}, associated
   * with the server (identified by host+port).
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory allowlistServers(Path knownServersFile, TrustManagerFactory tmf) {
    requireNonNull(knownServersFile);
    requireNonNull(tmf);
    return allowlistServers(new FileBackedFingerprintRepository(knownServersFile), tmf);
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownServersFile}, associated
   * with the server (identified by host+port).
   *
   * @param repository The repository in which to record fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory allowlistServers(FingerprintRepository repository, TrustManagerFactory tmf) {
    requireNonNull(repository);
    requireNonNull(tmf);
    return wrap(ServerFingerprintTrustManager.allowlist(repository), tmf);
  }

  /**
   * Accept all client certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a client presents a CA-signed certificate, the certificate fingerprint will be written to
   * {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownClientsFile The path to a file in which to record fingerprints.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordClientFingerprints(Path knownClientsFile) {
    requireNonNull(knownClientsFile);
    return recordClientFingerprints(new FileBackedFingerprintRepository(knownClientsFile));
  }

  /**
   * Accept all client certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a client presents a CA-signed certificate, the certificate fingerprint will be written to
   * {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param repository The repository in which to record fingerprints.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordClientFingerprints(FingerprintRepository repository) {
    requireNonNull(repository);
    return recordClientFingerprints(repository, true);
  }

  /**
   * Accept all client certificates, recording certificate fingerprints.
   *
   * <p>
   * For all connections, the fingerprint of the presented certificate will be written to {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownClientsFile The path to a file in which to record fingerprints.
   * @param skipCASigned If {@code true}, CA-signed certificates are not recorded.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordClientFingerprints(Path knownClientsFile, boolean skipCASigned) {
    requireNonNull(knownClientsFile);
    return recordClientFingerprints(new FileBackedFingerprintRepository(knownClientsFile), skipCASigned);
  }

  /**
   * Accept all client certificates, recording certificate fingerprints.
   *
   * <p>
   * For all connections, the fingerprint of the presented certificate will be written to {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param repository The repository in which to record fingerprints.
   * @param skipCASigned If {@code true}, CA-signed certificates are not recorded.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordClientFingerprints(FingerprintRepository repository, boolean skipCASigned) {
    requireNonNull(repository);
    return wrap(ClientFingerprintTrustManager.record(repository), skipCASigned);
  }

  /**
   * Accept all client certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a client presents a CA-signed certificate, the certificate fingerprint will be written to
   * {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownClientsFile The path to a file in which to record fingerprints.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordClientFingerprints(Path knownClientsFile, TrustManagerFactory tmf) {
    requireNonNull(knownClientsFile);
    requireNonNull(tmf);
    return recordClientFingerprints(new FileBackedFingerprintRepository(knownClientsFile), tmf);
  }

  /**
   * Accept all client certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a client presents a CA-signed certificate, the certificate fingerprint will be written to
   * {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param repository The repository in which to record fingerprints.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory recordClientFingerprints(
      FingerprintRepository repository,
      TrustManagerFactory tmf) {
    requireNonNull(repository);
    requireNonNull(tmf);
    return wrap(ClientFingerprintTrustManager.record(repository), tmf);
  }

  /**
   * Accept CA-signed client certificates, and otherwise trust client certificates on first access.
   *
   * <p>
   * Except when a client presents a CA-signed certificate, on first connection to this server the common name and
   * fingerprint of the presented certificate will be recorded. On subsequent connections, the client will be rejected
   * if the fingerprint has changed.
   *
   * <p>
   * <i>Note: unlike the seemingly equivalent {@link #trustServerOnFirstUse(Path)} method for authenticating servers,
   * this method for authenticating clients is <b>insecure</b> and <b>provides zero confidence in client identity</b>.
   * Unlike the server version, which bases the identity on the hostname and port the connection is being established
   * to, the client version only uses the common name of the certificate that the connecting client presents. Therefore,
   * clients can circumvent access control by using a different common name from any previously recorded client.</i>
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustClientOnFirstAccess(Path knownClientsFile) {
    requireNonNull(knownClientsFile);
    return trustClientOnFirstAccess(new FileBackedFingerprintRepository(knownClientsFile));
  }

  /**
   * Accept CA-signed client certificates, and otherwise trust client certificates on first access.
   *
   * <p>
   * Except when a client presents a CA-signed certificate, on first connection to this server the common name and
   * fingerprint of the presented certificate will be recorded. On subsequent connections, the client will be rejected
   * if the fingerprint has changed.
   *
   * <p>
   * <i>Note: unlike the seemingly equivalent {@link #trustServerOnFirstUse(Path)} method for authenticating servers,
   * this method for authenticating clients is <b>insecure</b> and <b>provides zero confidence in client identity</b>.
   * Unlike the server version, which bases the identity on the hostname and port the connection is being established
   * to, the client version only uses the common name of the certificate that the connecting client presents. Therefore,
   * clients can circumvent access control by using a different common name from any previously recorded client.</i>
   *
   * @param repository The repository containing fingerprints.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustClientOnFirstAccess(FingerprintRepository repository) {
    requireNonNull(repository);
    return trustClientOnFirstAccess(repository, true);
  }

  /**
   * Trust client certificates on first access.
   *
   * <p>
   * on first connection to this server the common name and fingerprint of the presented certificate will be recorded.
   * On subsequent connections, the client will be rejected if the fingerprint has changed.
   *
   * <p>
   * <i>Note: unlike the seemingly equivalent {@link #trustServerOnFirstUse(Path)} method for authenticating servers,
   * this method for authenticating clients is <b>insecure</b> and <b>provides zero confidence in client identity</b>.
   * Unlike the server version, which bases the identity on the hostname and port the connection is being established
   * to, the client version only uses the common name of the certificate that the connecting client presents. Therefore,
   * clients can circumvent access control by using a different common name from any previously recorded client.</i>
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustClientOnFirstAccess(Path knownClientsFile, boolean acceptCASigned) {
    requireNonNull(knownClientsFile);
    return trustClientOnFirstAccess(new FileBackedFingerprintRepository(knownClientsFile), acceptCASigned);
  }

  /**
   * Trust client certificates on first access.
   *
   * <p>
   * on first connection to this server the common name and fingerprint of the presented certificate will be recorded.
   * On subsequent connections, the client will be rejected if the fingerprint has changed.
   *
   * <p>
   * <i>Note: unlike the seemingly equivalent {@link #trustServerOnFirstUse(Path)} method for authenticating servers,
   * this method for authenticating clients is <b>insecure</b> and <b>provides zero confidence in client identity</b>.
   * Unlike the server version, which bases the identity on the hostname and port the connection is being established
   * to, the client version only uses the common name of the certificate that the connecting client presents. Therefore,
   * clients can circumvent access control by using a different common name from any previously recorded client.</i>
   *
   * @param repository The repository containing fingerprints.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustClientOnFirstAccess(FingerprintRepository repository, boolean acceptCASigned) {
    requireNonNull(repository);
    return wrap(ClientFingerprintTrustManager.tofa(repository), acceptCASigned);
  }

  /**
   * Accept CA-signed certificates, and otherwise trust client certificates on first access.
   *
   * <p>
   * Except when a client presents a CA-signed certificate, on first connection to this server the common name and
   * fingerprint of the presented certificate will be recorded. On subsequent connections, the client will be rejected
   * if the fingerprint has changed.
   *
   * <p>
   * <i>Note: unlike the seemingly equivalent {@link #trustServerOnFirstUse(Path)} method for authenticating servers,
   * this method for authenticating clients is <b>insecure</b> and <b>provides zero confidence in client identity</b>.
   * Unlike the server version, which bases the identity on the hostname and port the connection is being established
   * to, the client version only uses the common name of the certificate that the connecting client presents. Therefore,
   * clients can circumvent access control by using a different common name from any previously recorded client.</i>
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustClientOnFirstAccess(Path knownClientsFile, TrustManagerFactory tmf) {
    requireNonNull(knownClientsFile);
    requireNonNull(tmf);
    return trustClientOnFirstAccess(new FileBackedFingerprintRepository(knownClientsFile), tmf);
  }

  /**
   * Accept CA-signed certificates, and otherwise trust client certificates on first access.
   *
   * <p>
   * Except when a client presents a CA-signed certificate, on first connection to this server the common name and
   * fingerprint of the presented certificate will be recorded. On subsequent connections, the client will be rejected
   * if the fingerprint has changed.
   *
   * <p>
   * <i>Note: unlike the seemingly equivalent {@link #trustServerOnFirstUse(Path)} method for authenticating servers,
   * this method for authenticating clients is <b>insecure</b> and <b>provides zero confidence in client identity</b>.
   * Unlike the server version, which bases the identity on the hostname and port the connection is being established
   * to, the client version only uses the common name of the certificate that the connecting client presents. Therefore,
   * clients can circumvent access control by using a different common name from any previously recorded client.</i>
   *
   * @param repository The repository containing fingerprints.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory trustClientOnFirstAccess(
      FingerprintRepository repository,
      TrustManagerFactory tmf) {
    requireNonNull(repository);
    requireNonNull(tmf);
    return wrap(ClientFingerprintTrustManager.tofa(repository), tmf);
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownClientsFile}.
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory allowlistClients(Path knownClientsFile) {
    requireNonNull(knownClientsFile);
    return allowlistClients(new FileBackedFingerprintRepository(knownClientsFile));
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownClientsFile}.
   *
   * @param repository The repository containing fingerprints.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory allowlistClients(FingerprintRepository repository) {
    requireNonNull(repository);
    return allowlistClients(repository, true);
  }

  /**
   * Require clients to present known certificates.
   *
   * <p>
   * The fingerprint for a client certificate must be present in {@code knownClientsFile}.
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory allowlistClients(Path knownClientsFile, boolean acceptCASigned) {
    requireNonNull(knownClientsFile);
    return allowlistClients(new FileBackedFingerprintRepository(knownClientsFile), acceptCASigned);
  }

  /**
   * Require clients to present known certificates.
   *
   * <p>
   * The fingerprint for a client certificate must be present in {@code knownClientsFile}.
   *
   * @param repository The repository containing fingerprints.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory allowlistClients(FingerprintRepository repository, boolean acceptCASigned) {
    requireNonNull(repository);
    return wrap(ClientFingerprintTrustManager.allowlist(repository), acceptCASigned);
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownClientsFile}.
   *
   * @param knownClientsFile The path to the file containing fingerprints.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory allowlistClients(Path knownClientsFile, TrustManagerFactory tmf) {
    requireNonNull(knownClientsFile);
    requireNonNull(tmf);
    return allowlistClients(new FileBackedFingerprintRepository(knownClientsFile), tmf);
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the {@code knownClientsFile}.
   *
   * @param repository The repository containing fingerprints.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A trust manager factory.
   */
  public static TrustManagerFactory allowlistClients(FingerprintRepository repository, TrustManagerFactory tmf) {
    requireNonNull(repository);
    requireNonNull(tmf);
    return wrap(ClientFingerprintTrustManager.allowlist(repository), tmf);
  }

  private static TrustManagerFactory wrap(X509TrustManager trustManager, boolean acceptCASigned) {
    return wrap(trustManager, acceptCASigned ? defaultTrustManagerFactory() : null);
  }

  private static TrustManagerFactory wrap(X509TrustManager trustManager, @Nullable TrustManagerFactory delegate) {
    if (delegate != null) {
      return new DelegatingTrustManagerFactory(delegate, trustManager);
    } else {
      return new SingleTrustManagerFactory(trustManager);
    }
  }

  private static TrustManagerFactory defaultTrustManagerFactory() {
    TrustManagerFactory delegate;
    try {
      delegate = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    } catch (NoSuchAlgorithmException e) {
      // not reachable
      throw new RuntimeException(e);
    }
    try {
      delegate.init((KeyStore) null);
    } catch (KeyStoreException e) {
      // not reachable
      throw new RuntimeException(e);
    }
    return delegate;
  }
}
