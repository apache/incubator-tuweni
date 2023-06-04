// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.io.Base64
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Scanner

/**
 * Utility class for loading scuttlebutt keys from the file system.
 */
object KeyFileLoader {
  private val objectMapper = ObjectMapper()

  /**
   * Attempts to load the keys from the default scuttlebutt directory (~/.ssb), and throws an exception if the keys are
   * not available at the given path
   *
   * @param ssbFolder the folder containing the secret file.
   * @return the scuttlebutt key pair
   */
  @JvmStatic
  fun getLocalKeys(ssbFolder: Path): Signature.KeyPair {
    val secretPath = ssbFolder.resolve("secret")
    require(secretPath.toFile().exists()) { "Secret file does not exist" }
    require(secretPath.toFile().canRead()) { "Secret file cannot be read" }
    return loadKeysFromFile(secretPath)
  }

  /**
   * Attempts to load the scuttlebutt secret key with the supplied file path
   *
   * @param secretPath the filepath to the scuttlebutt secret key to load
   * @return the scuttlebutt key pair
   */
  @JvmStatic
  fun loadKeysFromFile(secretPath: Path): Signature.KeyPair {
    return try {
      val s = Scanner(secretPath.toFile(), StandardCharsets.UTF_8.name())
      s.useDelimiter("\n")
      val list = ArrayList<String>()
      while (s.hasNext()) {
        val next = s.next()

        // Filter out the comment lines
        if (!next.startsWith("#")) {
          list.add(next)
        }
      }
      val secretJSON = list.joinToString("")
      val values: HashMap<String, String> = objectMapper.readValue(
        secretJSON,
        object : TypeReference<HashMap<String, String>>() {}
      )
      val pubKey = values["public"]!!.replace(".ed25519", "")
      val privateKey = values["private"]!!.replace(".ed25519", "")
      val pubKeyBytes = Base64.decode(pubKey)
      val privKeyBytes = Base64.decode(privateKey)
      val pub = Signature.PublicKey.fromBytes(pubKeyBytes)
      val secretKey = Signature.SecretKey.fromBytes(privKeyBytes)
      Signature.KeyPair(pub, secretKey)
    } catch (e: IOException) {
      throw UncheckedIOException(e)
    }
  }
}
