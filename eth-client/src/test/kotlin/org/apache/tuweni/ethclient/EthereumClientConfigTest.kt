// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethclient

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

class EthereumClientConfigTest {

  @Test
  fun testFileConfig() {
    val config = EthereumClientConfig.fromFile(
      Paths.get(EthereumClientConfigTest::class.java.getResource("/minimal.conf").toURI()),
    )
    assertNotNull(config)
  }

  @Test
  fun testInvalidFileConfig() {
    val exception: IllegalArgumentException = assertThrows {
      EthereumClientConfig.fromFile(Paths.get("foo"))
    }
    assertEquals("Missing config file: 'foo'", exception.message)
  }

  @Test
  fun testEmptyConfig() {
    val config = EthereumClientConfig.fromString("")
    assertNotNull(config)
  }

  @Test
  fun testEmptyConfigHasNoPeerRepository() {
    val config = EthereumClientConfig.fromString("")
    assertEquals(0, config.peerRepositories().size)
  }

  @Test
  fun testConfigHasMemoryPeerRepository() {
    val config = EthereumClientConfig.fromString("[peerRepository.default]\ntype=\"memory\"")
    assertEquals(1, config.peerRepositories().size)
    assertEquals("memory", config.peerRepositories().get(0).getType())
    val errors = config.validate()
    assertEquals(0, errors.count())
  }

  @Test
  fun testConfigInvalidPeerRepository() {
    val config = EthereumClientConfig.fromString("[peerRepository.default]\ntype=\"foo\"")
    assertEquals(1, config.peerRepositories().size)
    val errors = config.validate()
    assertEquals(1, errors.count())
  }

  @Test
  fun testEmptyConfigHasOneDataStorage() {
    val config = EthereumClientConfig.empty()
    assertEquals(1, config.dataStores().size)
    val store = config.dataStores()[0]
    assertEquals("default", store.getName())
    assertEquals(Paths.get("data"), store.getStoragePath())
  }

  @Test
  fun testDefinedStorageTakesTheDefaultSpot() {
    val config = EthereumClientConfig.fromString("[storage.mine]\npath=\"data2\"\ngenesis=\"default\"")
    assertEquals(1, config.dataStores().size)
    val store = config.dataStores()[0]
    assertEquals("mine", store.getName())
    assertEquals(Paths.get("data2"), store.getStoragePath())
  }

  @Test
  fun toToml() {
    val config = EthereumClientConfig.fromString("[storage.forui]\npath=\"data\"")
    assertEquals(
      "[storage.forui]${System.lineSeparator()}path = \"data\"${System.lineSeparator()}",
      config.toToml(),
    )
  }

  @Test
  fun testDNSClient() {
    val config = EthereumClientConfig.fromString("[dns.mine]\nenrLink=\"example.com\"\npollingPeriod=1000")
    assertEquals(1, config.dnsClients().size)
    assertEquals("example.com", config.dnsClients()[0].enrLink())
    assertEquals(1000, config.dnsClients()[0].pollingPeriod())
    assertEquals("default", config.dnsClients()[0].peerRepository())
    assertEquals("mine", config.dnsClients()[0].getName())
  }

  @Test
  fun testDNSClientWithDNSServer() {
    val config = EthereumClientConfig.fromString(
      "[dns.mine]\nenrLink=\"example.com\"\npollingPeriod=1000\ndnsServer=\"4.4.5.5\"",
    )
    assertEquals(1, config.dnsClients().size)
    assertEquals("example.com", config.dnsClients()[0].enrLink())
    assertEquals(1000, config.dnsClients()[0].pollingPeriod())
    assertEquals("default", config.dnsClients()[0].peerRepository())
    assertEquals("mine", config.dnsClients()[0].getName())
    assertEquals("4.4.5.5", config.dnsClients()[0].dnsServer())
  }

  @Test
  fun testProxyConfig() {
    val config = EthereumClientConfig.fromString("[proxy.foo]\nname=\"foo\"\nupstream=\"localhost:15000\"")
    assertEquals(1, config.proxies().size)
    assertEquals("localhost:15000", config.proxies()[0].upstream())
    assertEquals("", config.proxies()[0].downstream())
  }

  @Test
  fun testInvalidPeers() {
    val config = EthereumClientConfig.fromString("[static.default]\nenodes=[\"enode://foo:bar@localhost:303\"]")
    val errors = config.validate()
    assertEquals(1, errors.count())
  }
}
