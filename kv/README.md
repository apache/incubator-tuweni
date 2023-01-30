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
# Key/Value Store

| Status         |           |
|----------------|-----------|
| Stability      | [stable]  |
| Component Type | [library] |

This library implements key/value stores backed by different persistence mechanisms.

| Description                                                      | In memory | No external dependencies | Class                                                                                                                        |
|------------------------------------------------------------------|-----------|--------------------------|------------------------------------------------------------------------------------------------------------------------------|
| JPA-backed key value store                                       |           |                          | [EntityManagerKeyValueStore](https://tuweni.apache.org/docs/org.apache.tuweni.kv/-entity-manager-key-value-store/index.html) |
| A key-value store backed by [Infinispan](https://infinispan.org) |           |                          | [InfinispanKeyValueStore](https://tuweni.apache.org/docs/org.apache.tuweni.kv/-infinispan-key-value-store/index.html)        |
| A key-value store backed by LevelDB                              |           | X                        | [LevelDBKeyValueStore](https://tuweni.apache.org/docs/org.apache.tuweni.kv/-level-d-b-key-value-store/index.html)            |
| A key-value store backed by a MapDB instance                     | X         | X                        | [MapDBKeyValueStore](https://tuweni.apache.org/docs/org.apache.tuweni.kv/-map-d-b-key-value-store/index.html)                |
| A key-value store backed by an in-memory Map                     | X         | X                        | [MapKeyValueStore](https://tuweni.apache.org/docs/org.apache.tuweni.kv/-map-key-value-store/index.html)                      |
| A store used as a proxy for another store                        |           |                          | [ProxyKeyValueStore](https://tuweni.apache.org/docs/org.apache.tuweni.kv/-proxy-key-value-store/index.html)                  |
| A key-value store backed by Redis                                |           |                          | [RedisKeyValueStore](https://tuweni.apache.org/docs/org.apache.tuweni.kv/-redis-key-value-store/index.html)                  |
| A key-value store backed by RocksDB                              |           | X                        | [RocksDBKeyValueStore](https://tuweni.apache.org/docs/org.apache.tuweni.kv/-rocks-d-b-key-value-store/index.html)            |
| A key-value store backed by a relational database.               |           |                          | [SQLKeyValueStore](https://tuweni.apache.org/docs/org.apache.tuweni.kv/-s-q-l-key-value-store/index.html)                    |

The stores all implement [KeyValueStore](https://tuweni.apache.org/docs/org.apache.tuweni.kv/-key-value-store/index.html) so they can be used interchangeably in applications.

The interface offers both coroutine-friendly methods (`getAsync`, `putAsync`) and asynchronous methods returning [`AsyncResult`](https://tuweni.apache.org/docs/org.apache.tuweni.concurrent/-async-result/index.html) or [`AsyncCompletion`](https://tuweni.apache.org/docs/org.apache.tuweni.concurrent/-async-completion/index.html) objects.

[stable]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#stable
[library]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#library