# Module tuweni

In the spirit of [Google Guava](https://github.com/google/guava/), Tuweni is a set of libraries and other tools to aid development of blockchain and other decentralized software in Java and other JVM languages.

# Package org.apache.tuweni.bytes

Classes and utilities for working with byte arrays.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-bytes` (`tuweni-bytes.jar`).

# Package org.apache.tuweni.concurrent

Classes and utilities for working with concurrency.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-concurrent` (`tuweni-concurrent.jar`).

# Package org.apache.tuweni.concurrent.coroutines

Extensions for mapping [AsyncResult][org.apache.tuweni.concurrent.AsyncResult] and [AsyncCompletion][org.apache.tuweni.concurrent.AsyncCompletion] objects to and from Kotlin coroutines.

# Package org.apache.tuweni.config

A general-purpose library for managing configuration data.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-config` (`tuweni-config.jar`).

# Package org.apache.tuweni.crypto

Classes and utilities for working with cryptography.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-crypto` (`tuweni-crypto.jar`).

# Package org.apache.tuweni.crypto.sodium

Classes and utilities for working with the sodium native library.

Classes and utilities in this package provide an interface to the native Sodium crypto library (https://www.libsodium.org/), which must be installed on the same system as the JVM. It will be searched for in common library locations, or its it can be loaded explicitly using [org.apache.tuweni.crypto.sodium.Sodium.loadLibrary].

Classes in this package depend upon the JNR-FFI library, which is not automatically included when using the complete Tuweni distribution. See https://github.com/jnr/jnr-ffi. JNR-FFI can be included using the gradle dependency `com.github.jnr:jnr-ffi`.

# Package org.apache.tuweni.devp2p

Kotlin coroutine based implementation of the Ethereum ÐΞVp2p protocol.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-devp2p` (`tuweni-devp2p.jar`).

# Package org.apache.tuweni.eth

Classes and utilities for working in the Ethereum domain.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-eth` (`tuweni-eth.jar`).

# Package org.apache.tuweni.io

Classes and utilities for handling file and network IO.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-io` (`tuweni-io.jar`).

# Package org.apache.tuweni.io.file

General utilities for working with files and the filesystem.

# Package org.apache.tuweni.junit

Utilities for better junit testing.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-junit` (`tuweni-junit.jar`).

# Package org.apache.tuweni.kademlia

An implementation of the kademlia distributed hash (routing) table.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-kademlia` (`tuweni-kademlia.jar`).

# Package org.apache.tuweni.kv

Classes and utilities for working with key/value stores.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-kv` (`tuweni-kv.jar`).

# Package org.apache.tuweni.net

Classes and utilities for working with networking.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-net` (`tuweni-net.jar`).

# Package org.apache.tuweni.net.coroutines

Classes and utilities for coroutine based networking.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-net-coroutines` (`tuweni-net-coroutines.jar`).

# Package org.apache.tuweni.net.tls

Utilities for doing fingerprint based TLS certificate checking.

# Package org.apache.tuweni.rlp

Recursive Length Prefix (RLP) encoding and decoding.

An implementation of the Ethereum Recursive Length Prefix (RLP) algorithm, as described at https://github.com/ethereum/wiki/wiki/RLP.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-rlp` (`tuweni-rlp.jar`).

# Package org.apache.tuweni.toml

A parser for Tom's Obvious, Minimal Language (TOML).

A parser and semantic checker for Tom's Obvious, Minimal Language (TOML), as described at https://github.com/toml-lang/toml/.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-toml` (tuweni-toml.jar).

# Package org.apache.tuweni.trie

Merkle Trie implementations.

Implementations of the Ethereum Patricia Trie, as described at https://github.com/ethereum/wiki/wiki/Patricia-Tree.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-merkle-trie` (`tuweni-merkle-trie.jar`).

# Package org.apache.tuweni.trie

Merkle Trie implementations using Kotlin coroutines.

# Package org.apache.tuweni.units

Classes and utilities for working with 256 bit integers and Ethereum units.

These classes are included in the complete Tuweni distribution, or separately when using the gradle dependency `org.apache.tuweni:tuweni-units` (`tuweni-units.jar`).

# Package org.apache.tuweni.units.bigints

Classes and utilities for working with 256 bit integers.

# Package org.apache.tuweni.units.ethereum

Classes and utilities for working with Ethereum units.

# License

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.