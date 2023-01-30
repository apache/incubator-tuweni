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
# Net

| Status         |           |
|----------------|-----------|
| Stability      | [stable]  |
| Component Type | [library] |

# Getting started

To get started, install the `net` library.

With Maven:

```xml
<dependency>
<groupId>org.apache.tuweni</groupId>
<artifactId>net</artifactId>
<version>{{site.data.project.latest_release}}</version>
</dependency>
```

Or using Gradle:

```groovy
implementation("org.apache.tuweni:net:{{site.data.project.latest_release}}")
```

If you haven't already, you will also need to add Bouncy Castle to your dependencies, and add the Bouncy Castle Security Provider to Java.

```java
Security.addProvider(new BouncyCastleProvider());
```

# Permissions

Since we're engaging in peer-to-peer applications, we will define different scenarios for trust.

First off, we will engage in two-way certificate authentication. The server will provide its certificate to the client and the client will also need to provide its identity.

All those settings are taken after the [Constellation private enclave network options](https://github.com/consensys/constellation).

## CA
By default, Java uses Certificate Authorities on the machine to authenticate a trusted certificate.

## Recording
We can also choose to record the fingerprints of incoming connections while authorizing all of them.

## Trust On First Use (TOFU)
In this setting, incoming connections are recorded, and their certificates are collected, but only the first certificate assigned to a connection is allowed.

This is a good way to mitigate MITM attacks.

## Allowlist

Only explicit hosts and certificate combinations are allowed.

# TrustManagerFactories

[`TrustManagerFactory` implementations](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/net/ssl/TrustManagerFactory.html) provide trust with JDK and most Java-based servers.

The [`TrustManagerFactories` API](/docs/org.apache.tuweni.net.tls/-trust-manager-factories/index.html) creates `TrustManagerFactory` objects for client and server communications.

As an example, the following method creates a `TrustManagerFactory` that records incoming connections into a `knownServersFile`, but only the ones that don't have a signed CA certificate.

```java
TrustManagerFactories.recordServerFingerprints(knownServersFile, false);
```

# VertxTrustOptions

The [`VertxTrustOptions` API](/docs/org.apache.tuweni.net.tls/-vertx-trust-options/index.html) is a quick drop-in API to configure Vert.x servers and clients for communications.

In the example below, we set a server to require client authentication and trust clients on first access (TOFU).
```java
HttpServerOptions options = new HttpServerOptions();
options.setSsl(true)
.setClientAuth(ClientAuth.REQUIRED)
.setPemKeyCertOptions(serverCert.keyCertOptions())
.setTrustOptions(VertxTrustOptions.trustClientOnFirstAccess(knownClientsFile))
.setIdleTimeout(1500)
.setReuseAddress(true)
.setReusePort(true);
httpServer = vertx.createHttpServer(options);
```

In this example, we set a HTTP client to communicate with servers that are whitelisted only (and not even trust CA-signed certificates):
```java
HttpClientOptions options = new HttpClientOptions();
options.setSsl(true)
.setTrustOptions(VertxTrustOptions.whitelistServers(knownServersFile, false))
.setConnectTimeout(1500)
.setReuseAddress(true)
.setReusePort(true);
client = vertx.createHttpClient(options);
```

[stable]:https://github.com/apache/incubator-tuweni/tree/main/docs#stable
[library]:https://github.com/apache/incubator-tuweni/tree/main/docs#library