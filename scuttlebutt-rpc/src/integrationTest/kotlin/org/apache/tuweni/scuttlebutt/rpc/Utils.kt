/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
