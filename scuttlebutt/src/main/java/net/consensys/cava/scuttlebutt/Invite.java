/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.scuttlebutt;

import net.consensys.cava.crypto.sodium.Signature;

/**
 * An invite code as defined by the Secure Scuttlebutt protocol guide.
 * <p>
 * See https://ssbc.github.io/scuttlebutt-protocol-guide/ for a detailed description of invites.
 */
public final class Invite {

  private final String host;
  private final int port;
  private final Identity identity;
  private final Signature.SecretKey secretKey;

  /**
   * Default constructor
   *
   * @param host the host to connect to
   * @param port the port of the host to connect to
   * @param identity the public key of the host to connect to
   * @param secretKey an Ed25519 secret key used to identify the invite
   */
  public Invite(String host, int port, Identity identity, Signature.SecretKey secretKey) {
    if (port <= 0 || port > 65535) {
      throw new IllegalArgumentException("Invalid port");
    }
    this.host = host;
    this.port = port;
    this.identity = identity;
    this.secretKey = secretKey;
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
  public Signature.SecretKey secretKey() {
    return secretKey;
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
        + identity.publicKeyAsBase64String()
        + "."
        + identity.curveName()
        + "~"
        + secretKey.bytes().toBase64String();

  }

  @Override
  public String toString() {
    return toCanonicalForm();
  }
}
