CREATE TABLE users (
    userId TEXT PRIMARY KEY,
    name TEXT,
    email TEXT UNIQUE,
    profilePic TEXT,
    role TEXT CHECK(role IN ('regular', 'volunteer', 'admin')),
    city TEXT,
    county TEXT
);

CREATE TABLE flood_reports (
    reportId TEXT PRIMARY KEY,
    userId TEXT,
    timestamp TEXT,
    latitude REAL,
    longitude REAL,
    photoUrl TEXT,
    severity TEXT CHECK(severity IN ('low', 'medium', 'high')),
    waterDepth TEXT,
    roadClosed INTEGER CHECK(roadClosed IN (0, 1)),
    verified INTEGER CHECK(verified IN (0, 1)),
    FOREIGN KEY(userId) REFERENCES users(userId)
);

CREATE TABLE discussions (
    threadId TEXT PRIMARY KEY,
    reportId TEXT,
    createdBy TEXT,
    timestamp TEXT,
    FOREIGN KEY(reportId) REFERENCES flood_reports(reportId),
    FOREIGN KEY(createdBy) REFERENCES users(userId)
);

CREATE TABLE messages (
    messageId TEXT PRIMARY KEY,
    threadId TEXT,
    userId TEXT,
    text TEXT,
    timestamp TEXT,
    upvotes INTEGER DEFAULT 0,
    FOREIGN KEY(threadId) REFERENCES discussions(threadId),
    FOREIGN KEY(userId) REFERENCES users(userId)
); 