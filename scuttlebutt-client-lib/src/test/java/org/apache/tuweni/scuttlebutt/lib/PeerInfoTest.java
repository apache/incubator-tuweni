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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.concurrent.CompletableAsyncResult;
import org.apache.tuweni.scuttlebutt.Invite;
import org.apache.tuweni.scuttlebutt.MalformedInviteCodeException;
import org.apache.tuweni.scuttlebutt.lib.model.Peer;
import org.apache.tuweni.scuttlebutt.lib.model.PeerStateChange;
import org.apache.tuweni.scuttlebutt.lib.model.StreamHandler;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class PeerInfoTest {

  private AsyncResult<Void> peerWithNodeUsingInviteCode(ScuttlebuttClient client) throws MalformedInviteCodeException,
      Exception {
    String inviteCode = System.getenv("ssb_invite_code");

    if (inviteCode == null) {
      return AsyncResult.exceptional(
          new Exception("Test requires an 'ssb_invite_code environment variable with a valid ssb invite code"));
    } else {
      return client.getNetworkService().redeemInviteCode(Invite.fromCanonicalForm(inviteCode));
    }
  }


  @Test
  @Disabled("Requires a scuttlebutt backend")
  public void testPeerStream() throws Exception, MalformedInviteCodeException {
    TestConfig config = TestConfig.fromEnvironment();

    AsyncResult<ScuttlebuttClient> scuttlebuttClientLibAsyncResult =
        ScuttlebuttClientFactory.fromNet(new ObjectMapper(), config.getHost(), config.getPort(), config.getKeyPair());

    ScuttlebuttClient scuttlebuttClient = scuttlebuttClientLibAsyncResult.get();

    CompletableAsyncResult<PeerStateChange> incomplete = AsyncResult.incomplete();

    scuttlebuttClient.getNetworkService().createChangesStream((closer) -> new StreamHandler<PeerStateChange>() {
      @Override
      public void onMessage(PeerStateChange item) {
        if (!incomplete.isDone()) {
          incomplete.complete(item);
        }

        closer.run();
      }

      @Override
      public void onStreamEnd() {

        if (!incomplete.isDone()) {
          incomplete.completeExceptionally(new Exception("Stream closed before any items were pushed."));
        }

      }

      @Override
      public void onStreamError(Exception ex) {
        incomplete.completeExceptionally(ex);
      }
    });

    ScuttlebuttClient client = scuttlebuttClientLibAsyncResult.get();

    AsyncResult<Void> inviteRedeemed = peerWithNodeUsingInviteCode(client);

    try {
      inviteRedeemed.get();
    } catch (Exception ex) {
      // Not fatal, since we may already have peered
      System.out.println("Exception while redeeming invite code: " + ex.getMessage());
    }

    PeerStateChange peerStateChange = incomplete.get(10000, TimeUnit.SECONDS);

    String changeType = peerStateChange.getType();

    assertTrue(changeType != null);
  }

  @Test
  @Disabled("Requires a scuttlebutt backend")
  public void getConnectedPeersTest() throws Exception, MalformedInviteCodeException {
    TestConfig config = TestConfig.fromEnvironment();

    AsyncResult<ScuttlebuttClient> scuttlebuttClientLibAsyncResult =
        ScuttlebuttClientFactory.fromNet(new ObjectMapper(), config.getHost(), config.getPort(), config.getKeyPair());

    ScuttlebuttClient scuttlebuttClient = scuttlebuttClientLibAsyncResult.get();

    AsyncResult<Void> asyncResult = peerWithNodeUsingInviteCode(scuttlebuttClient);

    try {
      asyncResult.get();
    } catch (Exception ex) {
      // Not fatal, since we may already have peered
      System.out.println("Exception while redeeming invite code: " + ex.getMessage());
    }

    AsyncResult<List<Peer>> connectedPeers = scuttlebuttClient.getNetworkService().getConnectedPeers();

    Thread.sleep(10000);

    assertTrue(!connectedPeers.get().isEmpty());
  }

}
