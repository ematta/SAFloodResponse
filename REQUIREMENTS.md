# **Scope and Requirements Document: Crowdsourced Flood Disaster Response Platform**

## **1. Project Overview**
### **1.1 Purpose**
The Crowdsourced Flood Disaster Response Platform is a mobile application designed to provide real-time flood tracking, community-driven discussions, and emergency assistance coordination for **San Antonio and Bexar County, Texas**. The platform integrates official flood data with **crowdsourced reports** and allows users to communicate in an interactive environment. The app supports **offline reporting** with SQLite storage and cloud synchronization via Firebase.

### **1.2 Goals and Objectives**
- Enable real-time user-reported **flood conditions**.
- Provide an **interactive map** overlaying flood reports and NOAA alerts.
- Support **community discussions** for active alerts.
- Allow users to **request and offer help**.
- Store data **locally using SQLite** with Firebase sync.
- Implement **authentication and user profile management**.
- Ensure **scalability** by leveraging Firebase Firestore for cloud-based storage.

## **2. Functional Requirements**
### **2.1 User Authentication & Profiles**
- **User Registration/Login**: Firebase Authentication with email/password.
- **Profile Management**: Users can set a display name, profile picture, and location preferences.
- **Roles & Permissions**:
  - **Regular Users**: Can report floods, participate in discussions, and request/offer help.
  - **Verified Volunteers**: Can coordinate rescue efforts.
  - **Admin Users**: Can moderate discussions and verify reports.

### **2.2 Flood Reporting**
- Users submit **flood reports** with:
  - **GPS location** (auto-detected or manual entry)
  - **Photos/videos**
  - **Flood severity rating** (low, medium, high)
  - **Water depth estimation**
  - **Road closure status**
- Reports are stored **locally in SQLite** and synced to **Firebase Firestore**.
- AI-based **duplicate detection** to prevent false reports.

### **2.3 Interactive Flood Map**
- Google Maps integration to display **live user reports**.
- NOAA/NWS API integration for **official flood alerts**.
- Heatmap visualization of **flood-prone areas**.
- Users can filter reports by **severity and recency**.

### **2.4 Community Discussion Forum**
- Each flood alert creates a **discussion thread**.
- Users can **post updates, share information, and request help**.
- **Voting system** to rank the most relevant posts.
- Moderation by admins to **remove false reports**.

### **2.5 Emergency Help Requests & Volunteer Coordination**
- Users can post **help requests** (e.g., need evacuation, supplies, shelter).
- Volunteers and responders can **offer assistance**.
- AI-based **matching system** to connect responders with those in need.

### **2.6 Offline Mode & Local Storage (SQLite)**
- Users can submit flood reports **without an internet connection**.
- Reports sync to Firebase **when connectivity is restored**.
- Cached flood alerts and discussions for offline viewing.

## **3. Database Schema**
### **3.1 Firebase Firestore Schema (Cloud Storage)**
```json
{
  "users": {
    "userId": {
      "name": "John Doe",
      "email": "johndoe@example.com",
      "profilePic": "url_to_profile_pic",
      "role": "regular | volunteer | admin",
      "locationPreferences": {
        "city": "San Antonio",
        "county": "Bexar"
      }
    }
  },
  "flood_reports": {
    "reportId": {
      "userId": "userId",
      "timestamp": "2025-03-09T12:00:00Z",
      "location": {
        "latitude": 29.4241,
        "longitude": -98.4936
      },
      "photoUrl": "url_to_photo",
      "severity": "low | medium | high",
      "waterDepth": "3 feet",
      "roadClosed": true,
      "verified": false
    }
  },
  "discussions": {
    "threadId": {
      "reportId": "reportId",
      "createdBy": "userId",
      "timestamp": "2025-03-09T12:30:00Z",
      "messages": {
        "messageId": {
          "userId": "userId",
          "text": "Flood is getting worse!",
          "timestamp": "2025-03-09T12:35:00Z",
          "upvotes": 5
        }
      }
    }
  }
}
```

### **3.2 SQLite Schema (Local Storage)**
```sql
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
```

## **4. Non-Functional Requirements**
- **Security:** Enforce Firebase Authentication & secure API calls.
- **Performance:** Optimize SQLite sync with Firebase.
- **Scalability:** Designed to transition to PostgreSQL for future cloud expansion.
- **User Experience:** Intuitive UI with minimal latency for real-time updates.
- **Reliability:** Offline mode ensures continuous access to reports.

## **5. Future Enhancements**
- **AI-Powered Flood Prediction System**.
- **SMS-Based Alert System** for non-smartphone users.
- **Live Video Streaming** for first responders.

---
This document defines the initial scope and requirements for the **Flood Disaster Response Platform**. Let me know if you need any adjustments! 🚀

