#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
# file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
# to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
# License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
# an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
# specific language governing permissions and limitations under the License.
pushd gradle/docker
docker build -t tuweni_test -f test.Dockerfile ../..
popd
docker run -v `pwd`/build/libs:/home/gradle/build/libs -v `pwd`/dist/build:/home/gradle/dist/build tuweni_test bash -c 'gradle -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m" assemble'
