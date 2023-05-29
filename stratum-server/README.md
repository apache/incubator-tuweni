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
# Stratum Server

| Status         |           |
|----------------|-----------|
| Stability      | [beta]    |
| Component Type | [library] |

This library introduces a [Stratum server](https://tuweni.apache.org/docs/org.apache.tuweni.stratum.server/-stratum-server/index.html) which listens on a TCP port for connections from stratum clients.

The server is compatible with the eth-proxy and Stratum1 protocols.

The server accepts new work with the `setNewWork(powInput: PoWInput)` method, with a parameter of type [PowInput](https://tuweni.apache.org/docs/org.apache.tuweni.stratum.server/-po-w-input/index.html).

[beta]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#beta
[library]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#library