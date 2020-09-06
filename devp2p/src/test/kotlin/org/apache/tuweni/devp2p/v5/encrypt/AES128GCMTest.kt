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
package org.apache.tuweni.devp2p.v5.encrypt

import org.apache.tuweni.bytes.Bytes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AES128GCMTest {

  @Test
  fun encryptPerformsAES128GCMEncryption() {
    val expectedResult = Bytes.fromHexString("0x000000207FC4FDB0E50ACBDA9CD993CFD3A3752104935B91F61B2AF2602C2" +
      "DC4EFD97AFB943DAB6B1F5A0B13E83C41964F818AB8A51D6D30550BAE8B33A952AA1B6818AB88B66DBD60F5E016FA546808D983B70D")

    val key = Bytes.fromHexString("0xA924872EAE2DA2C0057ED6DEBD8CAAB8")
    val nonce = Bytes.fromHexString("0x7FC4FDB0E50ACBDA9CD993CFD3A3752104935B91F61B2AF2602C2DC4EFD97AFB")
    val data = Bytes.fromHexString("0x19F23925525AF4C2697C1BED166EEB37B5381C10E508A27BCAA02CE661E62A2B")

    val result = AES128GCM.encrypt(key, nonce, data, Bytes.EMPTY)

    assertEquals(result, expectedResult)
  }

  @Test
  fun decryptPerformsAES128GCMDecryption() {
    val expectedResult = Bytes.fromHexString("0x19F23925525AF4C2697C1BED166EEB37B5381C10E508A27BCAA02CE661E62A2B")

    val encryptedData = Bytes.fromHexString("0x000000207FC4FDB0E50ACBDA9CD993CFD3A3752104935B91F61B2AF2602C2" +
      "DC4EFD97AFB943DAB6B1F5A0B13E83C41964F818AB8A51D6D30550BAE8B33A952AA1B6818AB88B66DBD60F5E016FA546808D983B70D")
    val key = Bytes.fromHexString("0xA924872EAE2DA2C0057ED6DEBD8CAAB8")

    val result = AES128GCM.decrypt(encryptedData, key, Bytes.EMPTY)

    assertEquals(result, expectedResult)
  }
}
