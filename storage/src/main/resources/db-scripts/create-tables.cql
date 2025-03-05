-- This CQL script will create the keyspace and all tables needed for the service.
-- It includes the schemas to save users, groups and tracking information.

-- NOTE: the keyspace as created here is not what we need in a production environment.
-- This is good enough for local development though.
-- For production see the location-service-chart (https://github.com/position-pal/location-service-chart)

CREATE KEYSPACE IF NOT EXISTS locationservice
    WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':1 };

USE locationservice;

CREATE TABLE IF NOT EXISTS ScopedUserInfo (
    GroupId TEXT,
    UserId TEXT,
    Status TEXT,
    Latitude DOUBLE,
    Longitude DOUBLE,
    LastUpdated TIMESTAMP,
    PRIMARY KEY (GroupId, UserId)
);

CREATE TABLE IF NOT EXISTS ScopedUserRoutes (
    GroupId TEXT,
    UserId TEXT,
    Timestamp TIMESTAMP,
    Latitude DOUBLE,
    Longitude DOUBLE,
    PRIMARY KEY (GroupId, UserId, Timestamp)
) WITH CLUSTERING ORDER BY (UserId ASC, Timestamp DESC);

CREATE TABLE IF NOT EXISTS UserGroupsByUserId (
    UserId TEXT,
    GroupId TEXT,
    Name TEXT,
    Surname TEXT,
    Email TEXT,
    PRIMARY KEY (UserId, GroupId)
);

CREATE TABLE IF NOT EXISTS UserGroupsByGroupId (
    GroupId TEXT,
    UserId TEXT,
    Name TEXT,
    Surname TEXT,
    Email TEXT,
    PRIMARY KEY (GroupId, UserId)
);
