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
package org.apache.tuweni.scuttlebutt.lib;

import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.scuttlebutt.Invite;
import org.apache.tuweni.scuttlebutt.MalformedInviteCodeException;
import org.apache.tuweni.scuttlebutt.lib.model.Peer;
import org.apache.tuweni.scuttlebutt.lib.model.PeerStateChange;
import org.apache.tuweni.scuttlebutt.lib.model.StreamHandler;
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest;
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction;
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse;
import org.apache.tuweni.scuttlebutt.rpc.RPCStreamRequest;
import org.apache.tuweni.scuttlebutt.rpc.mux.Multiplexer;
import org.apache.tuweni.scuttlebutt.rpc.mux.ScuttlebuttStreamHandler;
import org.apache.tuweni.scuttlebutt.rpc.mux.exceptions.ConnectionClosedException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A service for operations that connect nodes together and other network related operations
 * <p>
 * Assumes the standard 'ssb-gossip' plugin is installed and enabled on the node that we're connected to (or that RPC
 * functions meeting its manifest's contract are available.).
 * <p>
 * Should not be constructed directly, should be used via an ScuttlebuttClient instance.
 */
public class NetworkService {

  private final Multiplexer multiplexer;

  // We don't represent all the fields returned over RPC in our java classes, so we configure the mapper
  // to ignore JSON fields without a corresponding Java field
  private final ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  protected NetworkService(Multiplexer multiplexer) {
    this.multiplexer = multiplexer;
  }

  /**
   * Generates an invite code which can be used by another node to connect to our node, and have each node will befriend
   * each other.
   *
   * @param validForUses the number of times this should be usable by different people (0 if an infinite number of
   *        times.)
   * @return the invite that was generated.
   */
  public AsyncResult<Invite> generateInviteCode(int validForUses) {
    RPCFunction function = new RPCFunction(Arrays.asList("invite"), "create");

    RPCAsyncRequest request = new RPCAsyncRequest(function, Arrays.asList(validForUses));

    try {
      return multiplexer.makeAsyncRequest(request).then(response -> {
        try {
          return AsyncResult.completed(inviteFromRPCResponse(response));
        } catch (MalformedInviteCodeException malformedInviteCodeException) {
          return AsyncResult.exceptional(malformedInviteCodeException);
        }
      });
    } catch (JsonProcessingException ex) {
      return AsyncResult.exceptional(ex);
    }
  }

  /**
   * Redeems an invite issued by another node. If successful, the node will connect to the other node and each node will
   * befriend each other.
   *
   * @param invite
   * @return
   */
  public AsyncResult<Void> redeemInviteCode(Invite invite) {
    RPCFunction function = new RPCFunction(Arrays.asList("invite"), "accept");

    RPCAsyncRequest request = new RPCAsyncRequest(function, Arrays.asList(invite.toCanonicalForm()));

    try {
      return multiplexer.makeAsyncRequest(request).thenApply(r -> null);
    } catch (JsonProcessingException ex) {
      return AsyncResult.exceptional(ex);
    }
  }

  /**
   * Queries for the list of peers the instance is connected to.
   *
   * @return
   */
  public AsyncResult<List<Peer>> getConnectedPeers() {
    return getAllKnownPeers().thenApply(
        peers -> peers.stream().filter(peer -> peer.getState().equals("connected")).collect(Collectors.toList()));
  }

  /**
   * Queries for all the peers the instance is aware of in its gossip table.
   *
   * @return
   */
  public AsyncResult<List<Peer>> getAllKnownPeers() {
    RPCFunction function = new RPCFunction(Arrays.asList("gossip"), "peers");
    RPCAsyncRequest request = new RPCAsyncRequest(function, Arrays.asList());

    try {
      return multiplexer.makeAsyncRequest(request).then(rpcResponse -> {
        try {
          List<Peer> peers = rpcResponse.asJSON(mapper, new TypeReference<List<Peer>>() {});
          return AsyncResult.completed(peers);
        } catch (IOException e) {
          return AsyncResult.exceptional(e);
        }
      });
    } catch (JsonProcessingException e) {
      return AsyncResult.exceptional(e);
    }
  }

  /**
   * Opens a stream of peer connection state changes.
   *
   * @param streamHandler A function that can be invoked to instantiate a stream handler to pass events to, with a given
   *        runnable to close the stream early
   * @throws JsonProcessingException If the stream could be opened due to an error serializing the request
   * @throws ConnectionClosedException if the stream could not be opened due to the connection being closed
   */
  public void createChangesStream(Function<Runnable, StreamHandler<PeerStateChange>> streamHandler)
      throws JsonProcessingException,
      ConnectionClosedException {
    RPCFunction function = new RPCFunction(Arrays.asList("gossip"), "changes");

    RPCStreamRequest request = new RPCStreamRequest(function, Arrays.asList());

    multiplexer.openStream(request, (closer) -> new ScuttlebuttStreamHandler() {

      StreamHandler<PeerStateChange> changeStream = streamHandler.apply(closer);

      @Override
      public void onMessage(RPCResponse message) {
        try {
          PeerStateChange peerStateChange = message.asJSON(mapper, PeerStateChange.class);
          changeStream.onMessage(peerStateChange);
        } catch (IOException e) {
          changeStream.onStreamError(e);
          closer.run();
        }
      }

      @Override
      public void onStreamEnd() {
        changeStream.onStreamEnd();
      }

      @Override
      public void onStreamError(Exception ex) {
        changeStream.onStreamError(ex);
      }
    });

  }


  private Invite inviteFromRPCResponse(RPCResponse response) throws MalformedInviteCodeException {
    String rawInviteCode = response.asString();
    return Invite.fromCanonicalForm(rawInviteCode);
  }

}
