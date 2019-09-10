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
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest;
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction;
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse;
import org.apache.tuweni.scuttlebutt.rpc.mux.Multiplexer;

import java.util.Arrays;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * A service for operations that connect nodes together and other network related operations
 *
 * Should not be constructed directly, should be used via an ScuttlebuttClient instance.
 */
public class NetworkService {

  private final Multiplexer multiplexer;

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

  private Invite inviteFromRPCResponse(RPCResponse response) throws MalformedInviteCodeException {
    String rawInviteCode = response.asString();
    return Invite.fromCanonicalForm(rawInviteCode);
  }

}
