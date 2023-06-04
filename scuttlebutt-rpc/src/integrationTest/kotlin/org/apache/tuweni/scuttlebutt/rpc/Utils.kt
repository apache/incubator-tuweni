// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.rpc

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.io.Base64
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.Scanner

internal object Utils {
  // Filter out the comment lines
  @get:Throws(Exception::class)
  val localKeys: Signature.KeyPair
    get() {
      val env = System.getenv()
      val secretPath = Paths.get(env.getOrDefault("ssb_dir", "/tmp/ssb"), "secret")
      val file = secretPath.toFile()
      if (!file.exists()) {
        throw Exception("Secret file does not exist " + secretPath.toAbsolutePath())
      }
      val s = Scanner(file, StandardCharsets.UTF_8.name())
      s.useDelimiter("\n")
      val list = ArrayList<String>()
      while (s.hasNext()) {
        val next = s.next()

        // Filter out the comment lines
        if (!next.startsWith("#")) {
          list.add(next)
        }
      }
      val secretJSON = java.lang.String.join("", list)
      val mapper = ObjectMapper()
      val values: Map<String, String> =
        mapper.readValue(secretJSON, object : TypeReference<Map<String, String>>() {})
      val pubKey = values["public"]!!.replace(".ed25519", "")
      val privateKey = values["private"]!!.replace(".ed25519", "")
      val pubKeyBytes = Base64.decode(pubKey)
      val privKeyBytes = Base64.decode(privateKey)
      val pub = Signature.PublicKey.fromBytes(pubKeyBytes)
      val secretKey = Signature.SecretKey.fromBytes(privKeyBytes)
      return Signature.KeyPair(pub, secretKey)
    }
}
