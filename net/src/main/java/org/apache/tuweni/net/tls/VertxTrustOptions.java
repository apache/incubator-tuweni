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

import java.nio.file.Path;
import javax.net.ssl.TrustManagerFactory;

import io.vertx.core.net.TrustOptions;

/**
 * Vert.x {@link TrustOptions} for fingerprinting clients and servers.
 *
 * <p>
 * This class depends upon the Vert.X library being available on the classpath, along with its dependencies. See
 * https://vertx.io/download/. Vert.X can be included using the gradle dependency 'io.vertx:vertx-core'.
 */
public final class VertxTrustOptions {
  private VertxTrustOptions() {}

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
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordServerFingerprints(Path knownServersFile) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordServerFingerprints(knownServersFile));
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
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordServerFingerprints(FingerprintRepository repository) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordServerFingerprints(repository));
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
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordServerFingerprints(Path knownServersFile, boolean skipCASigned) {
    return new TrustManagerFactoryWrapper(
        TrustManagerFactories.recordServerFingerprints(knownServersFile, skipCASigned));
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
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordServerFingerprints(FingerprintRepository repository, boolean skipCASigned) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordServerFingerprints(repository, skipCASigned));
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
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordServerFingerprints(Path knownServersFile, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordServerFingerprints(knownServersFile, tmf));
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
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordServerFingerprints(FingerprintRepository repository, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordServerFingerprints(repository, tmf));
  }

  /**
   * Accept CA-signed certificates, and otherwise trust server certificates on first use.
   *
   * <p>
   * Except when a server presents a CA-signed certificate, on first connection to a server (identified by host+port)
   * the fingerprint of the presented certificate will be recorded. On subsequent connections, the presented certificate
   * will be matched to the stored fingerprint to ensure it has not changed.
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustServerOnFirstUse(Path knownServersFile) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.trustServerOnFirstUse(knownServersFile));
  }

  /**
   * Accept CA-signed certificates, and otherwise trust server certificates on first use.
   *
   * <p>
   * Except when a server presents a CA-signed certificate, on first connection to a server (identified by host+port)
   * the fingerprint of the presented certificate will be recorded. On subsequent connections, the presented certificate
   * will be matched to the stored fingerprint to ensure it has not changed.
   *
   * @param repository The repository containing fingerprints by host.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustServerOnFirstUse(FingerprintRepository repository) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.trustServerOnFirstUse(repository));
  }

  /**
   * Trust server certificates on first use.
   *
   * <p>
   * On first connection to a server (identified by host+port) the fingerprint of the presented certificate will be
   * recorded. On subsequent connections, the presented certificate will be matched to the stored fingerprint to ensure
   * it has not changed.
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted (and the fingerprint will not
   *        be recorded).
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustServerOnFirstUse(Path knownServersFile, boolean acceptCASigned) {
    return new TrustManagerFactoryWrapper(
        TrustManagerFactories.trustServerOnFirstUse(knownServersFile, acceptCASigned));
  }

  /**
   * Trust server certificates on first use.
   *
   * <p>
   * On first connection to a server (identified by host+port) the fingerprint of the presented certificate will be
   * recorded. On subsequent connections, the presented certificate will be matched to the stored fingerprint to ensure
   * it has not changed.
   *
   * @param repository The repository containing fingerprints by host.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted (and the fingerprint will not
   *        be recorded).
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustServerOnFirstUse(FingerprintRepository repository, boolean acceptCASigned) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.trustServerOnFirstUse(repository, acceptCASigned));
  }

  /**
   * Accept CA-signed certificates, and otherwise trust server certificates on first use.
   *
   * <p>
   * Except when a server presents a CA-signed certificate, on first connection to a server (identified by host+port)
   * the fingerprint of the presented certificate will be recorded. On subsequent connections, the presented certificate
   * will be matched to the stored fingerprint to ensure it has not changed.
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustServerOnFirstUse(Path knownServersFile, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.trustServerOnFirstUse(knownServersFile, tmf));
  }

  /**
   * Accept CA-signed certificates, and otherwise trust server certificates on first use.
   *
   * <p>
   * Except when a server presents a CA-signed certificate, on first connection to a server (identified by host+port)
   * the fingerprint of the presented certificate will be recorded. On subsequent connections, the presented certificate
   * will be matched to the stored fingerprint to ensure it has not changed.
   *
   * @param repository The repository containing fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustServerOnFirstUse(FingerprintRepository repository, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.trustServerOnFirstUse(repository, tmf));
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the known servers file, associated with
   * the server (identified by host+port).
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions allowlistServers(Path knownServersFile) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.allowlistServers(knownServersFile));
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the known servers file, associated with
   * the server (identified by host+port).
   *
   * @param repository The repository containing fingerprints by host.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions allowlistServers(FingerprintRepository repository) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.allowlistServers(repository));
  }

  /**
   * Require servers to present known certificates.
   *
   * <p>
   * The fingerprint for a server certificate must be present in the known servers file, associated with the server
   * (identified by host+port).
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions allowlistServers(Path knownServersFile, boolean acceptCASigned) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.allowlistServers(knownServersFile, acceptCASigned));
  }

  /**
   * Require servers to present known certificates.
   *
   * <p>
   * The fingerprint for a server certificate must be present in the known servers file, associated with the server
   * (identified by host+port).
   *
   * @param repository The repository containing fingerprints by host.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions allowlistServers(FingerprintRepository repository, boolean acceptCASigned) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.allowlistServers(repository, acceptCASigned));
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the known servers file, associated with
   * the server (identified by host+port).
   *
   * @param knownServersFile The path to the file containing fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions allowlistServers(Path knownServersFile, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.allowlistServers(knownServersFile, tmf));
  }

  /**
   * Require servers to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its fingerprint must be present in the known servers file, associated with
   * the server (identified by host+port).
   *
   * @param repository The repository containing fingerprints by host.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions allowlistServers(FingerprintRepository repository, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.allowlistServers(repository, tmf));
  }

  /**
   * Accept all client certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a client presents a CA-signed certificate, the certificate common name and fingerprint will be
   * written to {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownClientsFile The path to a file in which to record fingerprints by common name.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordClientFingerprints(Path knownClientsFile) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordClientFingerprints(knownClientsFile));
  }

  /**
   * Accept all client certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a client presents a CA-signed certificate, the certificate common name and fingerprint will be
   * written to {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param repository The repository in which to record fingerprints by common name.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordClientFingerprints(FingerprintRepository repository) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordClientFingerprints(repository));
  }

  /**
   * Accept all client certificates, recording certificate fingerprints.
   *
   * <p>
   * For all connections, the common name and fingerprint of the presented certificate will be written to
   * {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownClientsFile The path to a file in which to record fingerprints by common name.
   * @param skipCASigned If {@code true}, CA-signed certificates are not recorded.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordClientFingerprints(Path knownClientsFile, boolean skipCASigned) {
    return new TrustManagerFactoryWrapper(
        TrustManagerFactories.recordClientFingerprints(knownClientsFile, skipCASigned));
  }

  /**
   * Accept all client certificates, recording certificate fingerprints.
   *
   * <p>
   * For all connections, the common name and fingerprint of the presented certificate will be written to
   * {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param repository The repository in which to record fingerprints by common name.
   * @param skipCASigned If {@code true}, CA-signed certificates are not recorded.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordClientFingerprints(FingerprintRepository repository, boolean skipCASigned) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordClientFingerprints(repository, skipCASigned));
  }

  /**
   * Accept all client certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a client presents a CA-signed certificate, the certificate common name and fingerprint will be
   * written to {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param knownClientsFile The path to a file in which to record fingerprints by common name.
   * @param tmf A {@link TrustManagerFactory} for checking client certificates against a CA.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordClientFingerprints(Path knownClientsFile, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordClientFingerprints(knownClientsFile, tmf));
  }

  /**
   * Accept all client certificates, recording certificate fingerprints for those that are not CA-signed.
   *
   * <p>
   * Excepting when a client presents a CA-signed certificate, the certificate common name and fingerprint will be
   * written to {@code knownClientsFile}.
   *
   * <p>
   * Important: this provides no security as it is vulnerable to man-in-the-middle attacks.
   *
   * @param repository The repository in which to record fingerprints by common name.
   * @param tmf A {@link TrustManagerFactory} for checking client certificates against a CA.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions recordClientFingerprints(FingerprintRepository repository, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.recordClientFingerprints(repository, tmf));
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
   * @param knownClientsFile The path to the file containing fingerprints by common name.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustClientOnFirstAccess(Path knownClientsFile) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.trustClientOnFirstAccess(knownClientsFile));
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
   * @param repository The repository containing fingerprints by common name.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustClientOnFirstAccess(FingerprintRepository repository) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.trustClientOnFirstAccess(repository));
  }

  /**
   * Trust client certificates on first access.
   *
   * <p>
   * On first connection to this server the common name and fingerprint of the presented certificate will be recorded.
   * On subsequent connections, the client will be rejected if the fingerprint has changed.
   *
   * <p>
   * <i>Note: unlike the seemingly equivalent {@link #trustServerOnFirstUse(Path)} method for authenticating servers,
   * this method for authenticating clients is <b>insecure</b> and <b>provides zero confidence in client identity</b>.
   * Unlike the server version, which bases the identity on the hostname and port the connection is being established
   * to, the client version only uses the common name of the certificate that the connecting client presents. Therefore,
   * clients can circumvent access control by using a different common name from any previously recorded client.</i>
   *
   * @param knownClientsFile The path to the file containing fingerprints by common name.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustClientOnFirstAccess(Path knownClientsFile, boolean acceptCASigned) {
    return new TrustManagerFactoryWrapper(
        TrustManagerFactories.trustClientOnFirstAccess(knownClientsFile, acceptCASigned));
  }

  /**
   * Trust client certificates on first access.
   *
   * <p>
   * On first connection to this server the common name and fingerprint of the presented certificate will be recorded.
   * On subsequent connections, the client will be rejected if the fingerprint has changed.
   *
   * <p>
   * <i>Note: unlike the seemingly equivalent {@link #trustServerOnFirstUse(Path)} method for authenticating servers,
   * this method for authenticating clients is <b>insecure</b> and <b>provides zero confidence in client identity</b>.
   * Unlike the server version, which bases the identity on the hostname and port the connection is being established
   * to, the client version only uses the common name of the certificate that the connecting client presents. Therefore,
   * clients can circumvent access control by using a different common name from any previously recorded client.</i>
   *
   * @param repository The repository containing fingerprints by common name.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustClientOnFirstAccess(FingerprintRepository repository, boolean acceptCASigned) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.trustClientOnFirstAccess(repository, acceptCASigned));
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
   * @param knownClientsFile The path to the file containing fingerprints by common name.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustClientOnFirstAccess(Path knownClientsFile, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.trustClientOnFirstAccess(knownClientsFile, tmf));
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
   * @param repository The repository containing fingerprints by common name.
   * @param tmf A {@link TrustManagerFactory} for checking server certificates against a CA.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions trustClientOnFirstAccess(FingerprintRepository repository, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.trustClientOnFirstAccess(repository, tmf));
  }

  /**
   * Require clients to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its common name and fingerprint must be present in the
   * {@code knownClientsFile}.
   *
   * @param knownClientsFile The path to the file containing fingerprints by common name.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions allowlistClients(Path knownClientsFile) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.allowlistClients(knownClientsFile));
  }

  /**
   * Require clients to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its common name and fingerprint must be present in the
   * {@code knownClientsFile}.
   *
   * @param repository The repository containing fingerprints by common name.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions allowlistClients(FingerprintRepository repository) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.allowlistClients(repository));
  }

  /**
   * Require clients to present known certificates.
   *
   * <p>
   * The common name and fingerprint for a client certificate must be present in {@code knownClientsFile}.
   *
   * @param knownClientsFile The path to the file containing fingerprints by common name.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions allowlistClients(Path knownClientsFile, boolean acceptCASigned) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.allowlistClients(knownClientsFile, acceptCASigned));
  }

  /**
   * Require clients to present known certificates.
   *
   * <p>
   * The common name and fingerprint for a client certificate must be present in {@code knownClientsFile}.
   *
   * @param repository The repository containing fingerprints by common name.
   * @param acceptCASigned If {@code true}, CA-signed certificates will always be accepted.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions allowlistClients(FingerprintRepository repository, boolean acceptCASigned) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.allowlistClients(repository, acceptCASigned));
  }

  /**
   * Require clients to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its common name and fingerprint must be present in the
   * {@code knownClientsFile}.
   *
   * @param knownClientsFile The path to the file containing fingerprints by common name.
   * @param tmf A {@link TrustManagerFactory} for checking client certificates against a CA.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions allowlistClients(Path knownClientsFile, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.allowlistClients(knownClientsFile, tmf));
  }

  /**
   * Require clients to present known certificates, or CA-signed certificates.
   *
   * <p>
   * If a certificate is not CA-signed, then its common name and fingerprint must be present in the
   * {@code knownClientsFile}.
   *
   * @param repository The repository containing fingerprints by common name.
   * @param tmf A {@link TrustManagerFactory} for checking client certificates against a CA.
   * @return A Vert.x {@link TrustOptions}.
   */
  public static TrustOptions allowlistClients(FingerprintRepository repository, TrustManagerFactory tmf) {
    return new TrustManagerFactoryWrapper(TrustManagerFactories.allowlistClients(repository, tmf));
  }
}
