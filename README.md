# Tuweni: Apache Core Libraries for Java (& Kotlin)

[![Build Status](https://builds.apache.org/job/Apache%20Tuweni/job/CI/badge/icon)](https://builds.apache.org/job/Apache%20Tuweni/job/CI/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/incubator-tuweni/blob/master/LICENSE)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.tuweni/tuweni/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/org.apache.tuweni/tuweni)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/repository.apache.org/org.apache.tuweni/tuweni.svg)](https://repository.apache.org/content/repositories/snapshots/org/apache/tuweni/tuweni/)

See our [web site](https://tuweni.apache.org) for details on the project.

Tuweni is a set of libraries and other tools to aid development of blockchain and other decentralized software in Java and other JVM languages.

It includes a low-level bytes library, serialization and deserialization codecs (e.g. [RLP](https://github.com/ethereum/wiki/wiki/RLP)), various cryptography functions and primatives, and lots of other helpful utilities.

Tuweni is developed for JDK 11 or higher.

## Build Instructions

You need to have [Gradle](https://www.gradle.org/installation) and [Java](https://www.oracle.com/technetwork/java/javase/downloads/index.html) installed.

Tuweni requires Gradle 5.0 or higher.

Java 8 should be used for building in order to support both Java 8 and Java 11 at runtime.

## Clone along with submodules ##
    git clone https://github.com/apache/incubator-tuweni.git tuweni
    cd tuweni
    git submodule update --init --recursive

### First bootstrap and download the wrapper ###
    gradle setup
    
This downloads gradle and installs helper scripts for the project under `./gradlew` and `./gradlew.bat`.

### Build the project ###
    ./gradlew build


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
