/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.net.tls;

import org.apache.tuweni.bytes.Bytes;

/**
 * Repository of remote peer fingerprints.
 *
 */
public interface FingerprintRepository {

  /**
   * Checks whether the identifier of the remote peer is present in the repository.
   * 
   * @param identifier the identifier of a remote peer
   * @return true if the remote peer identifier is present in the repository
   */
  boolean contains(String identifier);

  /**
   * Checks whether the identifier of the remote peer is present in the repository, and its fingerprint matches the
   * fingerprint present.
   * 
   * @param identifier the identifier of a remote peer
   * @param fingerprint the fingerprint of a remote peer
   * @return true if there is a peer in the repository associated with that fingerprint
   */
  boolean contains(String identifier, Bytes fingerprint);

  /**
   * Adds the fingerprint of a remote peer to the repository.
   * 
   * @param identifier the identifier of a remote peer
   * @param fingerprint the fingerprint of a remote peer
   */
  void addFingerprint(String identifier, Bytes fingerprint);
}
