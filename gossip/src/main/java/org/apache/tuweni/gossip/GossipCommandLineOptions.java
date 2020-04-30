/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.gossip;


import org.apache.tuweni.config.Configuration;
import org.apache.tuweni.config.ConfigurationError;
import org.apache.tuweni.config.PropertyValidator;
import org.apache.tuweni.config.Schema;
import org.apache.tuweni.config.SchemaBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import picocli.CommandLine;

final class GossipCommandLineOptions {

  static final Schema createConfigFileSchema() {
    return SchemaBuilder
        .create()
        .addInteger("listenPort", 0, "Port to listen on", PropertyValidator.inRange(0, 65536))
        .addInteger("rpcPort", 0, "RPC port to listen on", PropertyValidator.inRange(0, 65536))
        .addString("networkInterface", "0.0.0.0", "Network interface to bind to", null)
        .addListOfString("peers", Collections.emptyList(), "Static peers list", null)
        .addString("messagelog", "messages.log", "Log file where messages are stored", null)
        .addBoolean("sending", false, "Whether this peer sends random messages to all other peers (load testing)", null)
        .addInteger(
            "sendInterval",
            1000,
            "Interval to wait in between sending messages in milliseconds (load testing)",
            null)
        .addInteger("numberOfMessages", 100, "Number of messages to publish to other peers (load testing)", null)
        .addInteger("payloadSize", 200, "Size of the random payload to send to other peers (load testing)", null)
        .toSchema();
  }

  @CommandLine.Option(names = {"-c", "--config"} , description = "Configuration file.")
  private Path configPath = null;

  @CommandLine.Option(arity = "0..*" , names = {"-p", "--peer"} , description = "Static peers list")
  private String[] peers;

  @CommandLine.Option(names = {"-l", "--listen"} , description = "Port to listen on")
  private Integer port;

  @CommandLine.Option(names = {"-r", "--rpc"} , description = "RPC port to listen on")
  private Integer rpcPort;

  @CommandLine.Option(names = {"-n", "--networkInterface"} , description = "Network interface to bind to")
  private String networkInterface = "0.0.0.0";

  @CommandLine.Option(names = {"-m", "--messageLog"} , description = "Log file where messages are stored")
  private String messageLog;

  @CommandLine.Option(names = {"--sendInterval"} ,
      description = "Interval to wait in between sending messages in milliseconds (load testing)")
  private Integer sendInterval;

  @CommandLine.Option(names = {"--payloadSize"} ,
      description = "Size of the random payload to send to other peers (load testing)")
  private Integer payloadSize;

  @CommandLine.Option(names = {"--numberOfMessages"} , description = "Number of messages to publish (load testing)")
  private Integer numberOfMessages;

  @CommandLine.Option(names = {"--sending"} ,
      description = "Whether this peer sends random messages to all other peers (load testing)")
  private Boolean sending;

  @CommandLine.Option(names = {"-h", "--help"} , description = "Prints usage prompt")
  private boolean help;

  private List<URI> peerAddresses;
  private Configuration config;

  GossipCommandLineOptions() {}

  /**
   * Constructor used for testing.
   */
  GossipCommandLineOptions(
      String[] peers,
      Integer port,
      String networkInterface,
      String messageLog,
      Integer rpcPort,
      Integer payloadSize,
      Integer sendInterval,
      Boolean sending,
      Integer numberOfMessages,
      Configuration config) {
    this.peers = peers;
    this.port = port;
    this.networkInterface = networkInterface;
    this.messageLog = messageLog;
    this.rpcPort = rpcPort;
    this.config = config;
    this.payloadSize = payloadSize;
    this.sendInterval = sendInterval;
    this.numberOfMessages = numberOfMessages;
    this.sending = sending;
  }

  private Configuration config() {
    if (config == null && configPath != null) {
      try {
        config = Configuration.fromToml(configPath, createConfigFileSchema());
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }
    return config;
  }

  List<URI> peers() {
    if (peerAddresses == null) {
      peerAddresses = new ArrayList<>();
      if (peers != null) {
        for (String peer : peers) {
          readPeerInfo(peer);
        }
      } else {
        if (config() != null) {
          for (String peer : config().getListOfString("peers")) {
            readPeerInfo(peer);
          }
        }
      }
    }
    return peerAddresses;
  }

  private void readPeerInfo(String peer) {
    URI peerURI = URI.create(peer);
    if (peerURI.getHost() == null) {
      throw new IllegalArgumentException("Invalid peer URI " + peerURI);
    }
    peerAddresses.add(peerURI);
  }

  void validate() {
    int listenPort = listenPort();
    if (listenPort < 0 || listenPort > 65535) {
      throw new IllegalArgumentException("Invalid port number " + listenPort);
    }
    int rpcPort = rpcPort();
    if (rpcPort < 0 || rpcPort > 65535) {
      throw new IllegalArgumentException("Invalid port number" + rpcPort);
    }
    peers();
    try {
      InetAddress.getByName(networkInterface);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Invalid network interface");
    }
    if (config() != null) {
      List<ConfigurationError> errors = config().errors();
      if (errors.size() > 0) {
        String message = errors
            .stream()
            .map(err -> "[" + err.position() + "] " + err.getMessage())
            .collect(Collectors.joining("\n"));
        throw new IllegalArgumentException(message);
      }
    }
  }

  int listenPort() {
    if (port != null) {
      return port;
    }
    if (config() != null) {
      return config.getInteger("listenPort");
    }
    return 0;
  }

  int rpcPort() {
    if (rpcPort != null) {
      return rpcPort;
    }
    if (config() != null) {
      return config.getInteger("rpcPort");
    }
    return 0;
  }

  String networkInterface() {
    if (networkInterface != null) {
      return networkInterface;
    }
    if (config() != null) {
      return config.getString("networkInterface");
    }
    return "0.0.0.0";
  }

  String messageLog() {
    if (messageLog != null) {
      return messageLog;
    }
    if (config != null) {
      return config.getString("messageLog");
    }
    return "messages.log";
  }

  boolean sending() {
    if (sending != null) {
      return sending;
    }
    if (config != null) {
      return config.getBoolean("sending");
    }
    return false;
  }

  Integer payloadSize() {
    if (payloadSize != null) {
      return payloadSize;
    }
    if (config != null) {
      return config.getInteger("payloadSize");
    }
    return 200;
  }

  Integer sendInterval() {
    if (sendInterval != null) {
      return sendInterval;
    }
    if (config != null) {
      return config.getInteger("sendInterval");
    }
    return 1000;
  }

  Integer numberOfMessages() {
    if (numberOfMessages != null) {
      return numberOfMessages;
    }
    if (config != null) {
      return config.getInteger("numberOfMessages");
    }
    return 100;
  }

  boolean help() {
    return help;
  }
}
