package org.apache.tuweni.scuttlebutt.discovery

import com.google.common.net.InetAddresses
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.scuttlebutt.Identity
import java.net.InetSocketAddress
import java.util.Objects
import java.util.regex.Pattern

/**
 * Representation of an identity associated with an IP and port, used for Scuttlebutt local discovery.
 *
 *
 * See https://ssbc.github.io/scuttlebutt-protocol-guide/ for a detailed description of this identity.
 */
class LocalIdentity(addr: InetSocketAddress, id: Identity) {
  private val id: Identity
  private val addr: InetSocketAddress

  /**
   * Constructor for a local identity
   *
   * @param ip the IP address associated with this local identity
   * @param port the port associated with this local identity
   * @param id the identity
   * @throws NumberFormatException if the port does not represent a number
   * @throws IllegalArgumentException if the port is not in the valid port range 0-65536
   */
  constructor(ip: String?, port: String?, id: Identity) : this(ip, Integer.valueOf(port), id) {}

  /**
   * Constructor for a local identity
   *
   * @param ip the IP address associated with this local identity
   * @param port the port associated with this local identity
   * @param id the identity
   * @throws IllegalArgumentException if the port is not in the valid port range 0-65536
   */
  constructor(ip: String?, port: Int, id: Identity) : this(InetSocketAddress(ip, port), id) {}

  /**
   * Constructor for a local identity
   *
   * @param addr the address associated with this local identity
   * @param id the identity
   * @throws NumberFormatException if the port does not represent a number
   * @throws IllegalArgumentException if the port is not in the valid port range 0-65536
   */
  init {
    InetAddresses.forString(addr.hostString)
    this.addr = addr
    this.id = id
  }

  /**
   * The canonical form of an invite
   *
   * @return the local identity in canonical form according to the Scuttlebutt protocol guide.
   */
  fun toCanonicalForm(): String {
    return "net:" + addr.hostString + ":" + addr.port + "~shs:" + id.publicKeyAsBase64String()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val that = other as LocalIdentity
    return toCanonicalForm() == that.toCanonicalForm()
  }

  override fun hashCode(): Int {
    return Objects.hash(id, addr)
  }

  override fun toString(): String {
    return toCanonicalForm()
  }

  companion object {
    private val regexpPattern = Pattern.compile("^net:(.*):(.*)~shs:(.*)$")

    /**
     * Create a local identity from a String of the form net:IP address:port~shs:base64 of public key
     *
     * @param str the String to interpret
     * @return the identity or null if the string doesn't match the format.
     */
    fun fromString(str: String?): LocalIdentity? {
      val result = regexpPattern.matcher(str)
      return if (!result.matches()) {
        null
      } else LocalIdentity(
        result.group(1),
        result.group(2),
        Identity.fromPublicKey(Signature.PublicKey.fromBytes(Bytes.fromBase64String(result.group(3))))
      )
    }
  }
}
