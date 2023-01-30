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
The `hobbits-relayer` application showcases how to use the Hobbits protocol to pass messages between different networks.

```bash
hobbits-relayer --help
Usage: <main class> [-h] [-b=<bind>] [-t=<to>]
-b, --bind=<bind>   Endpoint to bind to
-h, --help          Prints usage prompt
-t, --to=<to>       Endpoint to relay to
```

Example use:

Relay messages over TCP from port 21000 to 22000:
```bash
hobbits-relayer -b tcp://localhost:21000 -t tcp://localhost:22000
```

Relay messages over UDP from port 2222 to 4444:
```bash
hobbits-relayer -b udp://localhost:2222 -t udp://localhost:4444
```

Relay messages from a Web Socket port 2222 to a TCP server on 4444:
```bash
hobbits-relayer -b ws://localhost:2222 -t tcp://localhost:4444
```