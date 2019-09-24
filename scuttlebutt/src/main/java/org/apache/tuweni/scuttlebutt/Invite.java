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
package org.apache.tuweni.scuttlebutt;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.sodium.Signature;

import java.util.List;

import com.google.common.base.Splitter;

/**
 * An invite code as defined by the Secure Scuttlebutt protocol guide.
 * <p>
 * See https://ssbc.github.io/scuttlebutt-protocol-guide/ for a detailed description of invites.
 */
public final class Invite {

  private final String host;
  private final int port;
  private final Identity identity;
  private final Signature.Seed seedKey;

  /**
   * Default constructor
   *
   * @param host the host to connect to
   * @param port the port of the host to connect to
   * @param identity the public key of the host to connect to
   * @param seedKey an Ed25519 secret key used to identify the invite
   */
  public Invite(String host, int port, Identity identity, Signature.Seed seedKey) {
    if (port <= 0 || port > 65535) {
      throw new IllegalArgumentException("Invalid port");
    }
    this.host = host;
    this.port = port;
    this.identity = identity;
    this.seedKey = seedKey;
  }

  /**
   *
   * @return the host to connect to to redeem the invite
   */
  public String host() {
    return host;
  }

  /**
   *
   * @return the port of the host to connect to to redeem the invite
   */
  public int port() {
    return port;
  }

  /**
   *
   * @return the public key associated with the invite
   */
  public Identity identity() {
    return identity;
  }

  /**
   * The secret key the user may use while redeeming the invite to make the publisher follow them back.
   *
   * @return the secret key
   */
  public Signature.Seed seedKey() {
    return seedKey;
  }

  /**
   * Provides the invite as a string that is understood by other Secure Scuttlebutt clients.
   *
   * @return the canonical form of an invite.
   */
  public String toCanonicalForm() {
    return host
        + ":"
        + port
        + ":"
        + "@"
        + identity.publicKeyAsBase64String()
        + "."
        + identity.curveName()
        + "~"
        + seedKey.bytes().toBase64String();

  }

  /**
   * Takes an invite string in its canonical form and parses it into an Invite object, throwing an error if it is not of
   * the expected format.
   * 
   * @param inviteCode the invite code in its canonical form
   * @return the invite code
   * @throws MalformedInviteCodeException
   */
  public static Invite fromCanonicalForm(String inviteCode) throws MalformedInviteCodeException {
    String exceptionMessage = "Invite code should be of format host:port:publicKey.curveName~secretKey";

    List<String> parts = Splitter.on(':').splitToList(inviteCode);

    if (parts.size() != 3) {
      throw new MalformedInviteCodeException(exceptionMessage);
    }

    String host = parts.get(0);
    String portString = parts.get(1);
    int port = toPort(portString);

    List<String> keyAndSecret = Splitter.on('~').splitToList(parts.get(2));

    if (keyAndSecret.size() != 2) {
      throw new MalformedInviteCodeException(exceptionMessage);
    }

    String fullKey = keyAndSecret.get(0);
    List<String> splitKey = Splitter.on('.').splitToList(fullKey);

    if (splitKey.size() != 2) {
      throw new MalformedInviteCodeException(exceptionMessage);
    }
    String keyPart = splitKey.get(0);

    String secretKeyPart = keyAndSecret.get(1);

    Signature.Seed secretKey = toSecretKey(secretKeyPart);
    Ed25519PublicKeyIdentity identity = toPublicKey(keyPart);

    return new Invite(host, port, identity, secretKey);
  }

  private static Signature.Seed toSecretKey(String secretKeyPart) {
    Bytes secret = Bytes.fromBase64String(secretKeyPart);
    return Signature.Seed.fromBytes(secret);
  }

  private static Ed25519PublicKeyIdentity toPublicKey(String keyPart) {
    // Remove the @ from the front of the key
    String keyPartSuffix = keyPart.substring(1);
    Bytes publicKeyBytes = Bytes.fromBase64String(keyPartSuffix);
    Signature.PublicKey publicKey = Signature.PublicKey.fromBytes(publicKeyBytes);
    return new Ed25519PublicKeyIdentity(publicKey);
  }

  private static int toPort(String portString) throws MalformedInviteCodeException {
    try {
      return Integer.parseInt(portString);
    } catch (NumberFormatException ex) {
      throw new MalformedInviteCodeException("Expected a string for the port. Value parsed: " + portString);
    }
  }

  @Override
  public String toString() {
    return toCanonicalForm();
  }
}
