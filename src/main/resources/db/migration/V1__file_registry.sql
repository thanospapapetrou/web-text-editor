CREATE TABLE IF NOT EXISTS fileRegistry (
    name VARCHAR PRIMARY KEY,
    lastUpdated BIGINT NOT NULL,
    content VARCHAR NOT NULL
);
