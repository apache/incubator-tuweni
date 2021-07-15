package org.apache.tuweni.devp2p.v51

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.SECP256K1
import org.junit.jupiter.api.Test

class ReferenceTest {

  @Test
  fun connect() {
    val key1 = SECP256K1.KeyPair.fromSecretKey(SECP256K1.SecretKey.fromBytes(Bytes32.fromHexString("0xeef77acb6c6a6eebc5b363a475ac583ec7eccdb42b6481424c60f59aa326547f")))
    val key2 = SECP256K1.KeyPair.fromSecretKey(SECP256K1.SecretKey.fromBytes(Bytes32.fromHexString("0x66fb62bfbd66b9177a138c1e5cddbe4f7c30c343e94e68df8769459cb1cde628")))
  }
}
