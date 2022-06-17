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
    JSONRPCRequest req = new JSONRPCRequest(new StringOrLong("3"), "foo_method", new Object[] {"foo", "bar"}, "2.0");
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
