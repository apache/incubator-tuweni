# Tuweni: Apache Core Libraries for Java (& Kotlin)

[![Discord](https://user-images.githubusercontent.com/7288322/34471967-1df7808a-efbb-11e7-9088-ed0b04151291.png)](https://discord.gg/zHHPRpT)
[![Slack](https://img.shields.io/badge/slack-%23tuweni-72eff8?logo=slack)](https://s.apache.org/slack-invite)
[![Github build](https://github.com/apache/incubator-tuweni/workflows/assemble/badge.svg)](https://github.com/apache/incubator-tuweni/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/incubator-tuweni/blob/main/LICENSE)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.tuweni/tuweni-tuweni/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/org.apache.tuweni/tuweni-tuweni)
[![Twitter](https://img.shields.io/twitter/url/https/twitter.com/ApacheTuweni.svg?style=social&label=Follow%20%40ApacheTuweni)](https://twitter.com/ApacheTuweni)

![logo](tuweni.png)

See our [web site](https://tuweni.apache.org) for details on the project.

Tuweni is a set of libraries and other tools to aid development of blockchain and other decentralized software in Java and other JVM languages.

It includes a low-level bytes library, serialization and deserialization codecs (e.g. [RLP](https://github.com/ethereum/wiki/wiki/RLP)), various cryptography functions and primatives, and lots of other helpful utilities.

Tuweni is developed for JDK 11 or higher.

## Clone along with submodules ##
    git clone https://github.com/apache/incubator-tuweni.git tuweni
    cd tuweni
    git submodule update --init --recursive

### Build the project ###
#### With Gradle and Java ####
Install JDK 11.

Run:

`$>./gradlew build`

After a successful build, libraries will be available in `build/libs`.

## Contributing

Your contributions are very welcome! Here are a few links to help you:

- [Issue tracker: Report a defect or feature request](https://issues.apache.org/jira/projects/TUWENI/issues)
- [StackOverflow: Ask "how-to" and "why-didn't-it-work" questions](https://stackoverflow.com/questions/ask?tags=tuweni)

## Mailing lists

- [users@tuweni.incubator.apache.org](users@tuweni.apache.org) is for usage questions, help, and announcements. [subscribe](users-subscribe@tuweni.apache.org?subject=send%20this%20email%20to%20subscribe), [unsubscribe](dev-unsubscribe@tuweni.apache.org?subject=send%20this%20email%20to%20unsubscribe), [archives](https://www.mail-archive.com/users@tuweni.apache.org/)
- [dev@tuweni.incubator.apache.org](dev@tuweni.apache.org) is for people who want to contribute code to Tuweni. [subscribe](dev-subscribe@tuweni.apache.org?subject=send%20this%20email%20to%20subscribe), [unsubscribe](dev-unsubscribe@tuweni.apache.org?subject=send%20this%20email%20to%20unsubscribe), [archives](https://www.mail-archive.com/dev@tuweni.apache.org/)
- [commits@tuweni.incubator.apache.org](commits@tuweni.apache.org) is for commit messages and patches to Tuweni. [subscribe](commits-subscribe@tuweni.apache.org?subject=send%20this%20email%20to%20subscribe), [unsubscribe](commits-unsubscribe@tuweni.apache.org?subject=send%20this%20email%20to%20unsubscribe), [archives](https://www.mail-archive.com/commits@tuweni.apache.org/)

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

# Crypto Notice

This distribution includes cryptographic software. The country in which you
currently reside may have restrictions on the import, possession, use, and/or
re-export to another country, of encryption software. BEFORE using any
encryption software, please check your country's laws, regulations and
policies concerning the import, possession, or use, and re-export of encryption
software, to see if this is permitted. See [http://www.wassenaar.org] for
more information.

The Apache Software Foundation has classified this software as Export Commodity
Control Number (ECCN) 5D002, which includes information security software using
or performing cryptographic functions with asymmetric algorithms. The form and
manner of this Apache Software Foundation distribution makes it eligible for
export under the "publicly available" Section 742.15(b) exemption (see the BIS
Export Administration Regulations, Section 742.15(b)) for both object code and
source code.
The following provides more details on the included cryptographic software:
* [Bouncy Castle](http://bouncycastle.org/)
* [Apache Tuweni crypto](./crypto)
