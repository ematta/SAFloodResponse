# SAFloodResponse - San Antonio Flood Response App

An Android application for real-time flood reporting, tracking, and community response coordination in San Antonio, TX.

## Overview

SAFloodResponse is a community-driven flood reporting and response coordination platform designed to help San Antonio residents stay informed during flood events. Built with Jetpack Compose and powered by Firebase services, this app enables users to:

- Report floods with exact location, severity, and photographic evidence
- View real-time flood reports on an interactive map
- Participate in community discussions about flood-related topics
- Confirm or deny existing flood reports to validate their accuracy
- Access comprehensive flood reports with detailed information

## Features

### Flood Reporting System
- Submit detailed flood reports with:
  - Location (automatic or manual coordinates)
  - Severity level (low, medium, high)
  - Water depth measurements
  - Road closure status
  - Photo evidence uploads
  - Additional contextual details

### Interactive Map Interface
- Real-time visualization of all active flood reports
- Color-coded markers indicating flood severity
- Detailed report view with confirmation statistics
- Focus on San Antonio region with appropriate zoom levels

### Community Discussion Forums
- Create and join discussion threads
- Link discussions to specific flood reports
- Categorize discussions (General, Flood Report, Help Request, Announcement)
- Tag system for better organization and searchability

### User Authentication & Profiles
- User account creation and management
- Profile customization with photos
- Role-based permissions (Regular users, Volunteers, Administrators)

### Report Verification System
- Community-driven report verification through confirm/deny actions
- Statistical tracking of confirmations and denials
- Status tracking (pending, active, resolved)

## Technical Architecture

### Backend
- **Firebase Authentication**: User authentication and account management
- **Cloud Firestore**: Real-time NoSQL database for reports and discussions
- **Firebase Storage**: Media storage for user-submitted photos
- **Firebase App Check**: Security for backend resources

### Frontend
- **Jetpack Compose**: Modern declarative UI toolkit for Android
- **MVVM Architecture**: Clean separation of UI, business logic, and data
- **Kotlin Coroutines**: Asynchronous programming for smooth user experience
- **Google Maps SDK**: Interactive mapping features

### Key Components
- **FloodReportViewModel**: Manages flood report data and operations
- **DiscussionViewModel**: Handles discussion threads and messages
- **FloodMapViewModel**: Controls map visualization and interaction
- **FirestoreRepositories**: Data access layer for Firebase communication

## Installation & Setup

### Prerequisites
- Android Studio Iguana (2023.2) or newer
- JDK 11 or higher
- Google Maps API key
- Firebase project with Authentication, Firestore, and Storage enabled

### Setup Instructions
1. Clone this repository
2. Create a `local.properties` file in the root directory with:
```
sdk.dir=YOUR_ANDROID_SDK_PATH
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
```
3. Create and download `google-services.json` from your Firebase project console and place it in the `app` directory
4. Build and run the application

## User Roles

The application supports three user roles with different permissions:

1. **Regular Users**: Can report floods, participate in discussions, and request/offer help
2. **Administrators**: Can moderate discussions, verify reports, and perform all user actions

## Testing

The project includes comprehensive test coverage:

- JUnit tests for repositories and ViewModels
- Mock-based testing using MockK library
- Coroutine testing with Turbine and kotlinx-coroutines-test

## License

[Your license information here]

## Contributors

[List of contributors]

## Acknowledgments

- University of Texas at Austin Project
- San Antonio Emergency Management Contributors
- [Any other acknowledgments]