/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.scuttlebutt.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The available type of Scuttlebutt RPC requests
 */
public enum RPCRequestType {

  /**
   * An 'async' request, which returns one result some time in the future.
   */
  @JsonProperty("async")
  ASYNC,

  /**
   * A 'source' type request, which begins a stream of results
   */
  @JsonProperty("source")
  SOURCE


}
