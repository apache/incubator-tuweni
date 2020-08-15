package org.apache.tuweni.devp2p.v5

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.io.Base64URLSafe
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetSocketAddress
import java.net.URI

@ExtendWith(BouncyCastleExtension::class)
class LighthouseTest {

  @Test
  fun testConnect() = runBlocking {

    val enrRec = "-KO4QKkxI682IL-BRW_sQ0gQPCtVENyIxrQLttrbAY0g3WV2WcOMKT4dnTQq45Pddwg5rtZTDazYJuiOYRiFqDHAaCoBh2F0dG5ldHOIAAAAAAAAAACEZXRoMpDnp11aAAAAAf__________gmlkgnY0iXNlY3AyNTZrMaECG3AqNtcYU2XpMIlQEQhyUPKZq56oM67S6RrATa437MWDdGNwgiMo"
//    val record =
//      EthereumNodeRecord.fromRLP(Base64URLSafe.decode(enrRec))

    val service = DiscoveryService.open(
      SECP256K1.KeyPair.random(),
      localPort = 10000,
      bindAddress = InetSocketAddress("0.0.0.0", 10000),
      bootstrapENRList = listOf(enrRec)
    )
    service.start()
    kotlinx.coroutines.delay(50000)
  }
}
