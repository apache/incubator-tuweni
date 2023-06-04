// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class JsonRpcTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  static {
    mapper.registerModule(new EthJsonModule());
  }

  @Test
  void testJsonRpcRequestRoundtrip() throws Exception {
    JSONRPCRequest req =
        new JSONRPCRequest(new StringOrLong("3"), "foo_method", new Object[] {"foo", "bar"}, "2.0");
    String value = mapper.writeValueAsString(req);
    JSONRPCRequest req2 = mapper.readValue(value, JSONRPCRequest.class);
    assertEquals(req, req2);
  }

  @Test
  void testJsonRpcResponseRoundtrip() throws Exception {
    JSONRPCResponse resp = new JSONRPCResponse(new StringOrLong("3"), "result", null, "2.0");
    String value = mapper.writeValueAsString(resp);
    JSONRPCResponse resp2 = mapper.readValue(value, JSONRPCResponse.class);
    assertEquals(resp, resp2);
  }
}
