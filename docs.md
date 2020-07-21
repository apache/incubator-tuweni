# Module tuweni

Apache Tuweni is a set of libraries and other tools to aid development of blockchain and other decentralized software in Java and other JVM languages. It includes a low-level bytes library, serialization and deserialization codecs (e.g. RLP), various cryptography functions and primitives, and lots of other helpful utilities.

Learn more at [https://tuweni.apache.org].

# Gossip application

This application creates a sample standalone application applying the Plumtree gossip library.

```bash
$>bin/gossip --help
 Usage: <main class> [-h] [--sending] [--numberOfMessages=<numberOfMessages>]
                     [--payloadSize=<payloadSize>]
                     [--sendInterval=<sendInterval>] [-c=<configPath>]
                     [-l=<port>] [-m=<messageLog>] [-n=<networkInterface>]
                     [-r=<rpcPort>] [-p[=<peers>...]]...
       --numberOfMessages=<numberOfMessages>
                             Number of messages to publish (load testing)
       --payloadSize=<payloadSize>
                             Size of the random payload to send to other peers (load
                               testing)
       --sending             Whether this peer sends random messages to all other
                               peers (load testing)
       --sendInterval=<sendInterval>
                             Interval to wait in between sending messages in
                               milliseconds (load testing)
   -c, --config=<configPath> Configuration file.
   -h, --help                Prints usage prompt
   -l, --listen=<port>       Port to listen on
   -m, --messageLog=<messageLog>
                             Log file where messages are stored
   -n, --networkInterface=<networkInterface>
                             Network interface to bind to
   -p, --peer[=<peers>...]   Static peers list
   -r, --rpc=<rpcPort>       RPC port to listen on
```

Example usage:
```bash
$>bin/gossip --sending --payloadSize 512 --sendInterval 200 -l 2000 -r 4000 -n 0.0.0.0 -p tcp://127.0.0.1:3000 -p tcp://127.0.0.1:3001
```

You can configure this application to send random messages to other peers.
The application can also open up a RPC port accepting messages to propagate:

```bash
$>curl -X POST http://localhost:4000/publish -d "hello"
```

The application optionally takes a config file (with the `-c` flag).

Default configuration:
```toml
listenPort = 0
rpcPort = 0
networkInterface = "0.0.0.0"
peers = []
messagelog = "messages.log"
sending = false
sendInterval = 1000
numberOfMessages=100
payloadSize = 200
```

Sample sender configuration:
```toml
listenPort = 2000
rpcPort = 4000
networkInterface = "0.0.0.0"
peers = [ "tcp://127.0.0.1:3000", "tcp://127.0.0.1:3001"]
messagelog = "messages.log"
sending = true
sendInterval = 1000
numberOfMessages=100
payloadSize = 200
```

Peers are encoded as URIs using the TCP scheme: `tcp://127.0.0.1:3000`

## More information

- [Official website](https://tuweni.apache.org)
- [GitHub project](https://github.com/apache/incubator-tuweni)

# License

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.


# Relayer application

This application relays two hobbits endpoints.

Usage:
```bash
$> bin/hobbits-relayer --help
Usage: <main class> [-h] [-b=<bind>] [-t=<to>]
  -b, --bind=<bind>   Endpoint to bind to
  -h, --help          Prints usage prompt
  -t, --to=<to>       Endpoint to relay to
```

The application prints out messages to STDOUT.

Sample use:

Listen to a port
```bash
$> netcat -l -p 18000
```

Set up the relayer:
```bash
$> bin/hobbits-relayer -b tcp://localhost:10000 -t tcp://localhost:18000
```

Send a message:
```bash
$> cat message 
EWP 0.2 RPC 5 5
hellohello
$> cat message | netcat localhost 10000
```

The relayer will show the message:
```bash
EWP 0.2 RPC 5 5
0x68656C6C6F
0x68656C6C6F
```

The listener will show the message, received:
```bash
$> netcat -l -p 18000
EWP 0.2 RPC 5 5
hellohello
```

## More information

- [Official website](https://tuweni.apache.org)
- [GitHub project](https://github.com/apache/incubator-tuweni)


# License

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.