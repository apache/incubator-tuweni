CREATE TABLE IF NOT EXISTS `identity` (
    `id` varchar(36) PRIMARY KEY,
    `publickey` bytea
);

CREATE TABLE IF NOT EXISTS `endpoint` (
    `id` varchar(36) PRIMARY KEY,
    `lastSeen` timestamp,
    `lastVerified` timestamp,
    `host` text,
    `port` int,
    `identity` varchar(36)
);

ALTER TABLE endpoint ADD FOREIGN KEY (identity) REFERENCES identity(id)