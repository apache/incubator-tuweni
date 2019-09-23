# Releasing Tuweni

This guide comprises of all the steps to prepare, run a release and follow up on the next steps.

## One-time setup

Open the file `~/.gradle/gradle.properties` and add the following information:

```
asfNexusUsername=${asf LDAP id}
asfNexusPassword=${asf LDAP password}
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

Change the README file to update version numbers to the new release. Make a commit ($commitId) to the release branch and push.
TODO : point to commit example

Change the version number in the master branch in a commit and push.
TODO : point to commit example

## Running the release

### Build the artifacts

Set up the version strategy:
```
export BUILD_RELEASE=true
```

Build the artifacts
```
./gradlew build
```

Check the sha512 signatures match
```
shasum -a 512 dist/build/distributions/tuweni-bin-${RELEASE VERSION}.tgz
shasum -a 512 dist/build/distributions/tuweni-bin-${RELEASE VERSION}.zip
shasum -a 512 dist/build/distributions/tuweni-src-${RELEASE VERSION}.tgz
shasum -a 512 dist/build/distributions/tuweni-src-${RELEASE VERSION}.zip
shasum -a 512 dist/build/distributions/tuweni-gossip-${RELEASE VERSION}.tgz
shasum -a 512 dist/build/distributions/tuweni-gossip-${RELEASE VERSION}.zip
shasum -a 512 dist/build/distributions/tuweni-relayer-${RELEASE VERSION}.tgz
shasum -a 512 dist/build/distributions/tuweni-relayer-${RELEASE VERSION}.zip
```

Sign and publish
```
export ENABLE_SIGNING=true
./gradlew publish
```

This checks the code, the licenses, runs all the tests and creates all the artifacts (binaries, sources, javadoc).

This also publishes the artifacts to repository.apache.org in a staging repository.

### Push the distribution to dist.apache.org staging area

Delete the folder if necessary:
```
svn delete https://dist.apache.org/repos/dist/dev/incubator/tuweni/${RELEASE VERSION} -m "Deleting release candidate ${RELEASE_VERSION}"
```

Create the folder:
```
svn mkdir -m "Add new Tuweni folder for release ${RELEASE VERSION}" https://dist.apache.org/repos/dist/dev/incubator/tuweni/${RELEASE VERSION}
```

Check out the folder locally:
```
svn checkout https://dist.apache.org/repos/dist/dev/incubator/tuweni/${RELEASE VERSION} _staged
```

Copy the distribution artifacts to it - make sure to change the name to ${RELEASE VERSION}-incubating:

```
cp dist/build/distributions/tuweni-${RELEASE VERSION}.zip _staged/tuweni-${RELEASE VERSION}-incubating.zip
cp dist/build/distributions/tuweni-${RELEASE VERSION}.tgz _staged/tuweni-${RELEASE VERSION}-incubating.tgz
```

Commit the changes:
```
cd _staged
svn add tuweni-*
svn ci -m "Add Apache Tuweni ${RELEASE VERSION} release candidate"
```

### Tag the git repository

```
git tag -m "Release ${RELEASE VERSION}" v${RELEASE VERSION}
git push origin ${RELEASE BRANCH} --tags
```

### Close the staged repository for the release

Go to repository.apache.org and find the open repository that was created during the upload.

Click "Close" in the workflow buttons at the top.

### Open a thread for a vote

Send an email to dev@tuweni.apache.org with the following:

Subject:[VOTE] Apache Tuweni ${RELEASE VERSION} release
```
We're voting on the source distributions available here:
https://dist.apache.org/repos/dist/dev/incubator/tuweni/${RELEASE VERSION}/
The release tag is present here:
https://github.com/apache/incubator-tuweni/releases/tag/v${RELEASE VERSION}

Please review and vote as appropriate.

The following changes were made since ${PREVIOUS VERSION}:

${fill in changes}

```

The vote should be opened for at least 72 hours, longer if there is a week-end.

## Close the release

After the time of the vote has elapsed, close the vote thread with a recap showing the votes.

## Incubator general list

The next step is to email the general incubator list. If 3 IPMC votes were collected in the first vote, this is a notification.
If less than 3 votes were collected, this email is a new vote asking for more IPMC +1s.

## Close the vote

If a vote was called on the IPMC list, close it in the same fashion with a recap.

### Push the release to the dist final location

Move the files from `https://dist.apache.org/repos/dist/dev/incubator/tuweni/${RELEASE VERSION}/` to 
`https://dist.apache.org/repos/dist/release/incubator/tuweni/${RELEASE VERSION}/`:
```bash
svn move -m "Move Apache Tuweni ${RELEASE VERSION} to releases" https://dist.apache.org/repos/dist/dev/incubator/tuweni/${RELEASE VERSION} https://dist.apache.org/repos/dist/release/incubator/tuweni/${RELEASE VERSION}
```

Update the website:

Go to the repository here and perform this change:

https://github.com/apache/incubator-tuweni-website/commit/77066736df2997991e3a1954f41ced1c7a52999d

Test the downloads page (wait 24h for mirrors to update):


### Release the artifacts to maven central

Go to repository.apache.org to the closed repository you closed during the RC process.

Click on it and press the release workflow button at the top.

### Publish the site

Go to builds.apache.org, and run this build:

https://builds.apache.org/job/Apache%20Tuweni/job/website/

It takes an hour for the changes to be pushed out.

### Make release notes

Go to github.com, click tags and find your release.

Edit the tag and add release notes.

### Send an [ANNOUNCE] email

The email goes to dev@tuweni.apache.org, general@incubator.apache.org, and announce@apache.org.
This email must be sent from an @apache.org email.

```
Subject: [ANNOUNCE] Apache Tuweni (incubating) ${RELEASE VERSION} released

The Apache Tuweni team is proud to announce the release of Apache Tuweni
(incubating) ${RELEASE VERSION}.

Apache Tuweni is a set of libraries and other tools to aid development of
blockchain and other decentralized software in Java and other JVM 
languages. It includes a low-level bytes library, serialization and 
deserialization codecs (e.g. RLP), various cryptography functions 
and primatives, and lots of other helpful utilities. Tuweni is 
developed for JDK 1.8 or higher, and depends on various other FOSS libraries.

Source and binary distributions can be downloaded from:
https://tuweni.apache.org/download

Release notes are at:
https://github.com/apache/incubator-tuweni/releases/tag/v${RELEASE VERSION}

A big thank you to all the contributors in this milestone release!

To learn more about Tuweni and get started:
http://tuweni.apache.org/
Thanks!
The Apache Tuweni Team

----
Disclaimer: Apache Tuweni is an effort undergoing incubation at The Apache
Software Foundation (ASF), sponsored by the Apache Incubator. Incubation is
required of all newly accepted projects until a further review indicates
that the infrastructure, communications, and decision making process have
stabilized in a manner consistent with other successful ASF projects. While
incubation status is not necessarily a reflection of the completeness or
stability of the code, it does indicate that the project has yet to be
fully endorsed by the ASF.
```

### Clean up jira

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