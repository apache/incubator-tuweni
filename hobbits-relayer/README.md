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
