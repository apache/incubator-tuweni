# Releasing Tuweni

This guide comprises of all the steps to prepare, run a release and follow up on the next steps.

## One-time setup

Add your gpg keys to KEYS before you do anything else.

Open the file `~/.gradle/gradle.properties` and add the following information:

```
nexusUsername=${Nexus username}
nexusPassword=${Nexus password}
signing.keyId=${GPG key ID}
signing.gnupg.keyName=${GPG key ID}
```

Make sure your computer has gpg utilities installed. Follow the [Gradle signing plugin's GPG agent](https://docs.gradle.org/current/userguide/signing_plugin.html#sec:using_gpg_agent) instructions to set up.

## Branch into a release branch

Create a new release branch formed with the `major.minor` version numbers (see [semver](https://semver.org/) for glossary). For `1.0.0-SNAPSHOT` version, the release branch will therefore be `1.0`.

```
git checkout -b 1.0
git push origin 1.0
```

For patch releases, use the existing release branch.

## Prepare the release

Change the version number in the main branch in a commit and push.

Example: https://github.com/apache/incubator-tuweni/commit/0330d134f579034b9d943870ea31316899632a11

## Running the release

In your release branch:

```
export BUILD_RELEASE=true
export ENABLE_SIGNING=true
./gradlew stage
```

### Close the staged repository for the release

Go to oss.sonatype.org and find the open repository that was created during the upload.

Click "Close" in the workflow buttons at the top.

### Make release notes

Go to github.com, click tags and find your release.

Edit the tag and add release notes.

## Release on github

Go to github and mark the release as released. Change the tag name to drop the `-rc` at the end.

### Release the artifacts to maven central

Go to oss.sonatype.org to the closed repository you closed during the RC process.

Click on it and press the release workflow button at the top.

### Publish the site

Go to the repository here and perform this change:

https://github.com/tmio/tuweni-website/commit/77066736df2997991e3a1954f41ced1c7a52999d

Run ./publish.sh in the repository to publish the changes.

### Clean up github issues

If applicable mark the version as released.

# License

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
