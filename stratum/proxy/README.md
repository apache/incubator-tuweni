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
# Stratum Proxy

| Status         |               |
|----------------|---------------|
| Stability      | [beta]        |
| Component Type | [application] |

This application acts as a proxy between an Ethereum client and miners.

It connects to the client over JSON-RPC (using http://localhost:8545), polling for new work periodically, and submitting work sent by clients.

It runs a [Stratum server](https://tuweni.apache.org/docs/org.apache.tuweni.stratum.server/-stratum-server/index.html) to which miners can connect to over Stratum.

Usage:
* Expose a Stratum server on 0.0.0.0:16000:
```bash
$> stratum-proxy 16000
```



[beta]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#beta
[library]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#library