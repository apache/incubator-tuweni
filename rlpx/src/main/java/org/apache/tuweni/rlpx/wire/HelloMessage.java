// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx.wire;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.rlp.RLP;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class HelloMessage implements WireProtocolMessage {

  private final Bytes nodeId;
  private final int listenPort;
  private final String clientId;
  private final int p2pVersion;
  private final List<Capability> capabilities;

  private HelloMessage(Bytes nodeId, int listenPort, String clientId, int p2pVersion, List<Capability> capabilities) {
    this.nodeId = nodeId;
    this.listenPort = listenPort;
    this.clientId = clientId;
    this.p2pVersion = p2pVersion;
    this.capabilities = capabilities;
  }

  static HelloMessage create(
      Bytes nodeId,
      int listenPort,
      int p2pVersion,
      String clientId,
      List<Capability> capabilities) {
    return new HelloMessage(nodeId, listenPort, clientId, p2pVersion, capabilities);
  }

  static HelloMessage read(Bytes data) {
    return RLP.decodeList(data, reader -> {
      int p2pVersion = reader.readInt();
      String clientId = reader.readString();
      List<Capability> capabilities = reader.readList(capabilitiesReader -> {
        List<Capability> caps = new ArrayList<>();
        while (!capabilitiesReader.isComplete()) {
          caps
              .add(
                  capabilitiesReader
                      .readList(
                          capabilityReader -> new Capability(
                              capabilityReader.readString(),
                              capabilityReader.readInt())));
        }
        return caps;
      });
      int listenPort = reader.readInt();
      Bytes nodeId = reader.readValue();
      return new HelloMessage(nodeId, listenPort, clientId, p2pVersion, capabilities);
    });
  }

  @Override
  public Bytes toBytes() {
    return RLP.encodeList(writer -> {
      writer.writeInt(p2pVersion);
      writer.writeString(clientId);
      writer.writeList(capabilitiesWriter -> {
        for (Capability cap : capabilities) {
          capabilitiesWriter.writeList(capabilityWriter -> {
            capabilityWriter.writeString(cap.name());
            capabilityWriter.writeInt(cap.version());
          });
        }
      });
      writer.writeInt(listenPort);
      writer.writeValue(nodeId);
    });
  }

  @Override
  public int messageType() {
    return 0;
  }

  public Bytes nodeId() {
    return nodeId;
  }

  public List<Capability> capabilities() {
    return capabilities;
  }

  public int p2pVersion() {
    return p2pVersion;
  }

  public String clientId() {
    return clientId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    HelloMessage that = (HelloMessage) o;
    if (capabilities.size() != that.capabilities.size()) {
      return false;
    }

    for (int i = 0; i < capabilities.size(); i++) {
      if (!Objects.equals(capabilities.get(i), that.capabilities.get(i))) {
        return false;
      }
    }
    return listenPort == that.listenPort
        && p2pVersion == that.p2pVersion
        && Objects.equals(nodeId, that.nodeId)
        && Objects.equals(clientId, that.clientId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeId, listenPort, clientId, p2pVersion, capabilities);
  }

  @Override
  public String toString() {
    return "HelloMessage{"
        + "nodeId="
        + nodeId
        + ", listenPort="
        + listenPort
        + ", clientId='"
        + clientId
        + '\''
        + ", p2pVersion="
        + p2pVersion
        + ", capabilities="
        + capabilities
        + '}';
  }
}
