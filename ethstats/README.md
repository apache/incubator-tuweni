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
# EthStats server

| Status         |           |
|----------------|-----------|
| Stability      | [alpha]   |
| Component Type | [library] |

This library defines [`EthStatsServer`](https://tuweni.apache.org/docs/org.apache.tuweni.ethstats/-eth-stats-server/index.html),
a server implementing the EthStats protocol. Over a websocket connection, Ethereum clients can connect and send information
to this server.

The server accepts a [EthStatsServerController](https://tuweni.apache.org/docs/org.apache.tuweni.ethstats/-eth-stats-server-controller/index.html) which will receive all the information from clients.

You will need to implement your own controller. There is a controller for OpenTelemetry provided as example in this library, `OpenTelemetryServerController`.


[alpha]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#alpha
[library]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#library