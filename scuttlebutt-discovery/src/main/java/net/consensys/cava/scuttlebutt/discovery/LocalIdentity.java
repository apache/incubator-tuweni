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
package org.apache.tuweni.scuttlebutt.discovery;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.scuttlebutt.Identity;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.net.InetAddresses;

/**
 * Representation of an identity associated with an IP and port, used for Scuttlebutt local discovery.
 * <p>
 * See https://ssbc.github.io/scuttlebutt-protocol-guide/ for a detailed description of this identity.
 */
public final class LocalIdentity {

  private static final Pattern regexpPattern = Pattern.compile("^net:(.*):(.*)~shs:(.*)$");

  /**
   * Create a local identity from a String of the form net:IP address:port~shs:base64 of public key
   * 
   * @param str the String to interpret
   * @return the identity or null if the string doesn't match the format.
   */
  public static LocalIdentity fromString(String str) {
    Matcher result = regexpPattern.matcher(str);
    if (!result.matches()) {
      return null;
    }
    return new LocalIdentity(
        result.group(1),
        result.group(2),
        Identity.fromPublicKey(Signature.PublicKey.fromBytes(Bytes.fromBase64String(result.group(3)))));

  }

  private final Identity id;

  private final InetSocketAddress addr;

  /**
   * Constructor for a local identity
   *
   * @param ip the IP address associated with this local identity
   * @param port the port associated with this local identity
   * @param id the identity
   * @throws NumberFormatException if the port does not represent a number
   * @throws IllegalArgumentException if the port is not in the valid port range 0-65536
   */
  public LocalIdentity(String ip, String port, Identity id) {
    this(ip, Integer.valueOf(port), id);
  }

  /**
   * Constructor for a local identity
   *
   * @param ip the IP address associated with this local identity
   * @param port the port associated with this local identity
   * @param id the identity
   * @throws IllegalArgumentException if the port is not in the valid port range 0-65536
   */
  public LocalIdentity(String ip, int port, Identity id) {
    this(new InetSocketAddress(ip, port), id);
  }

  /**
   * Constructor for a local identity
   *
   * @param addr the address associated with this local identity
   * @param id the identity
   * @throws NumberFormatException if the port does not represent a number
   * @throws IllegalArgumentException if the port is not in the valid port range 0-65536
   */
  public LocalIdentity(InetSocketAddress addr, Identity id) {
    InetAddresses.forString(addr.getHostString());
    this.addr = addr;
    this.id = id;
  }

  /**
   *
   * @return the local identity in canonical form according to the Scuttlebutt protocol guide.
   */
  public String toCanonicalForm() {
    return "net:" + addr.getHostString() + ":" + addr.getPort() + "~shs:" + id.publicKeyAsBase64String();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    LocalIdentity that = (LocalIdentity) o;
    return toCanonicalForm().equals(that.toCanonicalForm());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, addr);
  }

  @Override
  public String toString() {
    return toCanonicalForm();
  }
}
