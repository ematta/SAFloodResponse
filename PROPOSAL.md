# FloodNet: Community-Powered Flood Response

## Group Members
- Enrique Matta-Rodriguez - enrique.j.matta@utexas.edu

## Application Overview
FloodNet is a mobile application aimed at delivering real-time, crowdsourced flood tracking, community discussion, and coordinated emergency response for San Antonio and Bexar County, Texas. It combines user-generated data, government alerts, and local weather information to help residents and emergency personnel effectively manage and respond to flood situations.

## Major Subsystems
- **User Authentication & Profile Management** (Firebase Authentication)
- **Real-Time Flood Reporting System** (SQLite for offline, Firebase Firestore sync)
- **Interactive Flood Map** (Google Maps API, NOAA, NWS, SAWS API integrations)
- **Community Discussion Threads** (Real-time discussion & moderation)
- **Emergency Help Coordination** (Request and response management)
- **Offline Support & Data Synchronization** (Local SQLite storage with Firebase sync)
- **Push Notifications** (Firebase Cloud Messaging)

## Major Challenges
- Ensuring data accuracy and preventing misinformation through verification methods.
- Real-time synchronization without performance degradation.
- Effective engagement and user retention through intuitive UX and community-driven features.

## UI Sketch (Key Elements)
Due to text limitations, please visualize the following sketches:
- **Main Dashboard:**
  - Map display with flood report markers
  - Floating action button to add new flood report
  - Bottom navigation bar (Home, Discussions, Emergency Requests, Profile)
- **Flood Report Submission:**
  - Form with location auto-detection or manual input
  - Severity rating and water depth entry
  - Photo upload via camera integration

## 8-Week Schedule
| Week | Task |
|------|------|
| 1    | Project setup, Firebase Authentication, SQLite DB setup |
| 2    | Flood reporting functionality (UI, offline and cloud sync) |
| 3    | Interactive flood map (Google Maps, API integrations) |
| 4    | Community discussions (thread creation, moderation, voting system) |
| 5    | Emergency request system & volunteer response feature |
| 6    | Notifications system (Firebase Cloud Messaging) |
| 7    | Offline mode synchronization and optimization |
| 8    | Comprehensive testing, debugging, UI/UX refinement, deployment preparation |

## APIs & Technologies
- Firebase (Firestore, Authentication, Cloud Messaging, Storage)
- Google Maps API (interactive mapping and user location)
- NOAA/NWS API (official flood alerts)
- OpenWeather API (weather forecast)
- SAWS API (water level data)

## Functional Specification
- Users authenticate and create personal profiles.
- Flood reports submitted with photos, severity, and geo-location.
- Interactive map displaying real-time flood data and government alerts.
- Community discussion threads per flood event, including voting mechanisms.
- Users can request emergency assistance, and volunteers can respond.
- Local SQLite storage enabling offline use with cloud synchronization when connectivity is restored.

## Comparison to Similar Apps
| App       | Features                                        | Limitations                     |
|-----------|-------------------------------------------------|---------------------------------|
| FEMA App  | Official alerts & evacuation routes             | No community-driven interactions|
| Waze      | Crowdsourced hazard reporting                   | Not specialized for flood events|
| Nextdoor  | Community discussion forums                     | Lacks real-time flood tracking  |

FloodNet uniquely integrates real-time flood tracking, emergency assistance coordination, and community engagement, distinguishing it from existing solutions.

