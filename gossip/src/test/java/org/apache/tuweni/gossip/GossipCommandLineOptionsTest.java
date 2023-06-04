// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.gossip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.tuweni.config.Configuration;

import java.net.URI;

import org.junit.jupiter.api.Test;

class GossipCommandLineOptionsTest {

  @Test
  void testInvalidPort() {
    GossipCommandLineOptions opts =
        new GossipCommandLineOptions(new String[0], -4, "0.0.0.0", null, 3, 0, 0, false, 50, null);
    assertThrows(IllegalArgumentException.class, opts::validate);
  }

  @Test
  void testInvalidPeer() {
    GossipCommandLineOptions opts =
        new GossipCommandLineOptions(
            new String[] {"tcp://400.300.200.100:9000"},
            10,
            "0.0.0.0",
            null,
            3,
            0,
            0,
            false,
            50,
            null);
    assertThrows(IllegalArgumentException.class, opts::validate);
  }

  @Test
  void testInvalidNetworkInterface() {
    GossipCommandLineOptions opts =
        new GossipCommandLineOptions(
            new String[] {}, 10, "400.300.200.100", null, 3, 0, 0, false, 50, null);
    assertThrows(IllegalArgumentException.class, opts::validate);
  }

  @Test
  void operateFromConfig() {
    Configuration config =
        Configuration.fromToml(
            ""
                + "peers=[\"tcp://127.0.0.1:2000\"]\n"
                + "listenPort=1080\n"
                + "networkInterface=\"127.0.0.1\"\n"
                + "messageLog=\"D:/Temp\"",
            GossipCommandLineOptions.createConfigFileSchema());
    GossipCommandLineOptions opts =
        new GossipCommandLineOptions(null, null, null, null, 3000, 0, 0, false, 50, config);
    opts.validate();
    assertEquals(1080, opts.listenPort());
    assertEquals(1, opts.peers().size());
    assertEquals(URI.create("tcp://127.0.0.1:2000"), opts.peers().get(0));
    assertEquals("127.0.0.1", opts.networkInterface());
    assertEquals("D:/Temp", opts.messageLog());
  }

  @Test
  void invalidConfigFilePort() {
    Configuration config =
        Configuration.fromToml(
            ""
                + "peers=[\"tcp://127.0.0.1:3000\"]\n"
                + "listenPort=500000\n"
                + "networkInterface=\"127.0.0.1\"\n"
                + "messageLog=\"D:/Temp\"",
            GossipCommandLineOptions.createConfigFileSchema());
    GossipCommandLineOptions opts =
        new GossipCommandLineOptions(null, null, null, null, 3000, 0, 0, false, 50, config);

    assertThrows(IllegalArgumentException.class, opts::validate);
  }

  @Test
  void cliConfigOverConfigFile() {
    Configuration config =
        Configuration.fromToml(
            ""
                + "peers=\"tcp://127.0.0.1:3000\"\n"
                + "listenPort=1080\n"
                + "networkInterface=\"127.0.0.1\"\n"
                + "messageLog=\"D:/Temp\"");
    GossipCommandLineOptions opts =
        new GossipCommandLineOptions(
            new String[] {"tcp://192.168.0.1:3000"},
            400,
            "0.0.0.0",
            "C:/Temp",
            3000,
            0,
            0,
            false,
            50,
            config);
    assertEquals(400, opts.listenPort());
    assertEquals(1, opts.peers().size());
    assertEquals(URI.create("tcp://192.168.0.1:3000"), opts.peers().get(0));
    assertEquals("0.0.0.0", opts.networkInterface());
    assertEquals("C:/Temp", opts.messageLog());
  }
}
