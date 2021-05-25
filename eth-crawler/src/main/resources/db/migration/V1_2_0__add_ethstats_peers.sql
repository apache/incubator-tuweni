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

CREATE TABLE IF NOT EXISTS latency (
    address varchar(255) not null,
    id varchar(255) not null,
    value int,
    createdAt timestamp DEFAULT now(),
    PRIMARY KEY(address, id)
);
CREATE UNIQUE INDEX latency_createdAt ON latency(createdAt);


CREATE TABLE IF NOT EXISTS pendingtx (
    address varchar(255) not null,
    id varchar(255) not null,
    value int,
    createdAt timestamp DEFAULT now(),
    PRIMARY KEY(address, id)
);
CREATE UNIQUE INDEX pendingtx_createdAt ON pendingtx(createdAt);


CREATE TABLE IF NOT EXISTS ethstats_peer(
    address varchar(255) not null,
    id varchar(255) not null,
    name text,
    client text,
    net text,
    api text,
    protocol text,
    os text,
    osVer text,
    node text,
    port int,
    createdAt timestamp DEFAULT now(),
    PRIMARY KEY(address, id)
);
CREATE UNIQUE INDEX ethstats_peer_createdAt ON ethstats_peer(createdAt);

CREATE TABLE IF NOT EXISTS ethstats_nodestats(
    address varchar(255) not null,
    id varchar(255) not null,
    gasPrice int,
    hashrate int,
    mining bool,
    syncing bool,
    active bool,
    uptime int,
    peers int,
    createdAt timestamp DEFAULT now(),
    PRIMARY KEY(address, id)
);

CREATE UNIQUE INDEX ethstats_nodestats_createdAt ON ethstats_nodestats(createdAt);

CREATE TABLE IF NOT EXISTS ethstats_block(
    address varchar(255) not null,
    id varchar(255) not null,
    number bytea,
    hash bytea,
    parentHash bytea,
    timestamp bigint,
    gasUsed bigint,
    gasLimit bigint,
    miner bytea,
    difficulty bytea,
    totalDifficulty bytea,
    transactions bytea array,
    transactionsRoot bytea,
    stateRoot bytea,
    uncles bytea array,
    createdAt timestamp DEFAULT now(),
    PRIMARY KEY(address, id)
);

CREATE UNIQUE INDEX ethstats_block_createdAt ON ethstats_block(createdAt);

