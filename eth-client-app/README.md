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
# Ethereum client application

| Status         |               |
|----------------|---------------|
| Stability      | [prototype]   |
| Component Type | [application] |

The `tuweni` application is an Ethereum client that can run multiple chains and multiple discovery mechanisms.

`tuweni` can sync multiple chains at once. It also has a web UI.

NOTE: everything at this point is at best a prototype. This may change at any time.

Usage:

```bash
Apache Tuweni client loading
Usage: <main class> [-h] [-c=<configPath>] [-w=<web>]
-c, --config=<configPath>
Configuration file.
-h, --help        Prints usage prompt
-w, --web=<web>   Web console host:port
```

Most of the action happens in the configuration file, written with TOML.

Example with one chain:

```toml
[storage.default]
path="data"
genesis="default"
[genesis.default]
path=default.json
```

The `default.json` file is your usual genesis configuration file.

Example with two chains:

Example with one chain:

```toml
[storage.foo]
path="data"
genesis="foo"
[genesis.foo]
path=default.json
[storage.bar]
path="data"
genesis="bar"
[genesis.bar]
path=other.json
```

[prototype]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#prototype
[application]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#application