<!---
Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at
 *
http://www.apache.org/licenses/LICENSE-2.0
 *
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
 --->
# JSONRPC application

| Status         |               |
|----------------|---------------|
| Stability      | [beta]        |
| Component Type | [application] |

The `jsonrpc` application is a JSON-RPC proxy that can cache, filter, throttle, meter and authenticate requests to a JSON-RPC endpoint.

The application is configured via a toml file, described below:

## Examples

### Proxy a JSON-RPC endpoint publicly

You set up a node and want to proxy. You expose all JSON-RPC methods on your node but only want to expose the `eth_` namespace publicly, with HTTP Basic authentication:

```toml
endpointUrl="http://192.168.1.2:8545" # IP of your node
allowedMethods=["eth_"]
cacheEnabled=true
cachedMethods=["eth_"]
cacheStoragePath="/var/jsonrpccache" # Cache location
```

### Local development

You have a local development and you're using a RPC provider, but you want to work without calling the service constantly.

```toml
endpointUrl="https://example.com:8545/?token=mytoken" # service URL
cacheEnabled=true
cachedMethods=[""] # All methods are cached
cacheStoragePath="/var/jsonrpccache" # Cache location
allowedMethods=[""] # All methods are enabled
```

[beta]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#beta
[application]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#application