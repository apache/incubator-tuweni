<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
# JSON-RPC proxy

The JSON-RPC proxy allows you to cache requests to a JSON-RPC endpoint.

This example showcases how to use the proxy with a public service.

## Requirements

This example requires Docker, Docker Compose 1.25, Java 11 and Gradle > 6.

## Steps

### Check out this repository

`$> git clone https://github.com/apache/incubator-tuweni`

### Build the image

You need to install the Gradle wrapper the first time you make a checkout:

`$> gradle setup`

You can now run gradle to build the docker image:

`$> ./gradlew dist::buildBinImage`

The build should end with similar lines:
```bash
 ---> be57b46c612c
Successfully built be57b46c612c
Successfully tagged apache-tuweni/tuweni:2.1.0-SNAPSHOT
Created image with ID 'be57b46c612c'.

BUILD SUCCESSFUL in 1m 13s
256 actionable tasks: 2 executed, 254 up-to-date
```

### Run the example

Make sure the image you just tagged matches the one in the `docker_compose.yml` file.

Run Docker:

```bash
docker-compose up
```



