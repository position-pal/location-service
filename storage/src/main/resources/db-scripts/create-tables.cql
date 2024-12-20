USE locationservice;

CREATE TABLE ScopedUserInfo (
    GroupId TEXT,
    UserId TEXT,
    Status TEXT,
    Latitude DOUBLE,
    Longitude DOUBLE,
    LastUpdated TIMESTAMP,
    PRIMARY KEY (GroupId, UserId)
);

CREATE TABLE ScopedUserRoutes (
    GroupId TEXT,
    UserId TEXT,
    Timestamp TIMESTAMP,
    Latitude DOUBLE,
    Longitude DOUBLE,
    PRIMARY KEY (GroupId, UserId, Timestamp)
) WITH CLUSTERING ORDER BY (UserId ASC, Timestamp DESC);

CREATE TABLE UserGroupsByUserId (
    UserId TEXT,
    GroupId TEXT,
    PRIMARY KEY (UserId, GroupId)
);

CREATE TABLE UserGroupsByGroupId (
    GroupId TEXT,
    UserId TEXT,
    PRIMARY KEY (GroupId, UserId)
);
