/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

CREATE TABLE IF NOT EXISTS identity (
    id varchar(36) PRIMARY KEY,
    publickey bytea
);

CREATE TABLE IF NOT EXISTS endpoint (
    id varchar(36) PRIMARY KEY,
    lastSeen timestamp,
    lastVerified timestamp,
    host text,
    port int,
    identity varchar(36)
);

CREATE TABLE IF NOT EXISTS nodeInfo (
    id varchar(36) PRIMARY KEY,
    createdAt timestamp,
    host text,
    port int,
    publickey bytea,
    p2pversion int,
    clientId text,
    capabilities text,
    genesisHash text,
    bestHash text,
    totalDifficulty text,
    identity varchar(36)
);

ALTER TABLE endpoint ADD FOREIGN KEY (identity) REFERENCES identity(id);
ALTER TABLE nodeInfo ADD FOREIGN KEY (identity) REFERENCES identity(id);