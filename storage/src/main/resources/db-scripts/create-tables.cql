USE locationservice;

CREATE TABLE UserInfo (
    UserId TEXT PRIMARY KEY,
    Status TEXT,
    Latitude DOUBLE,
    Longitude DOUBLE,
    LastUpdated TIMESTAMP
);

CREATE TABLE UserRoutes (
    UserId TEXT,
    Timestamp TIMESTAMP,
    Latitude DOUBLE,
    Longitude DOUBLE,
    PRIMARY KEY (UserId, Timestamp)
) WITH CLUSTERING ORDER BY (Timestamp DESC);
