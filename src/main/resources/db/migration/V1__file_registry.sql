CREATE TABLE IF NOT EXISTS file_registry (
    name VARCHAR PRIMARY KEY,
    lastUpdated BIGINT NOT NULL,
    content VARCHAR NOT NULL
);
