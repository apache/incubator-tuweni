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

# Stability levels

### Prototype

This component is still in development. It is likely that the component owners might not give attention to bugs or performance issues.
Configuration options might break often depending on how things evolve. The component should not be used in production.

### Alpha

The component is ready to be used for limited non-critical workloads and the authors of this component would welcome your feedback.
Bugs and performance problems should be reported, but component owners might not work on them right away.
The configuration options might change often without backwards compatibility guarantees.

### Beta

Same as Alpha, but the configuration options are deemed stable.
While there might be breaking changes between releases, component owners should try to minimize them.
A component at this stage is expected to have had exposure to non-critical production workloads already during its **Alpha** phase, making it suitable for broader usage.

### Stable

The component is ready for general availability.
Bugs and performance problems should be reported and there's an expectation that the component owners will work on them.
Breaking changes, including configuration options and the component's output are not expected to happen without prior notice, unless under special circumstances.

### Deprecated

The component is planned to be removed in a future version and no further support will be provided.
Note that new issues will likely not be worked on.
When a component enters "deprecated" mode, it is expected to exist for at least two minor releases.
See the component's readme file for more details on when a component will cease to exist.

### Unmaintained

A component identified as unmaintained does not have an active code owner.
Such component may have never been assigned a code owner or a previously active code owner has not responded to requests for feedback within 6 weeks of being contacted.
Issues and pull requests for unmaintained components will be labelled as such.
After 6 months of being unmaintained, these components will be removed from official distribution.
Components that are unmaintained are actively seeking contributors to become code owners.

# Component Type

### Library

The component is meant to be consumed as a library, and to be embedded into other applications. It should be consumed through its API.

### Example

The component is an example of usage of libraries or applications, and can be used as a tutorial or integration test.

### Application

The component is an application that can be consumed as an executable, and offers configuration through file and command line arguments.
