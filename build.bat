@echo off
REM Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
REM file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
REM to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
REM License. You may obtain a copy of the License at
REM
REM http://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
REM an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
REM specific language governing permissions and limitations under the License.
@echo on
bitsadmin /transfer myDownloadJob https://services.gradle.org/distributions/gradle-6.5.1-bin.zip d:\gradle.zip
unzip d:\gradle.zip -d gradle_download
gradle_download/gradle-6.5.1/bin/gradle.bat setup
./gradlew.bat assemble
