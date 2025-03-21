# SA Flood Response App

An Android application providing crowdsourced flood tracking, community discussions, and emergency assistance coordination for San Antonio and Bexar County, Texas.

## Features

- **Firebase Authentication**: Email/password based user authentication
- **Real-time Flood Reporting**: User-reported flood conditions
- **Interactive Map**: Live user reports with official flood alerts
- **Community Discussion**: Forums for active flood alerts
- **Emergency Help**: Request and offer assistance
- **Offline Support**: Local storage with cloud synchronization

## Setup Instructions

### Prerequisites

- Android Studio Iguana | 2023.2.1 or newer
- Kotlin 2.0.21 or newer
- JDK 11 or newer
- Firebase account

### Firebase Configuration

1. Create a new Firebase project at [https://console.firebase.google.com/](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project
   - Use package name: `edu.utap.safloodresponse`
   - Register the app
3. Download the `google-services.json` file and place it in the `app` directory
4. Enable Email/Password Authentication in the Firebase Console:
   - Navigate to Authentication > Sign-in method
   - Enable Email/Password

### Build and Run

1. Clone the repository
2. Open the project in Android Studio
3. Sync project with Gradle files
4. Run the app on an emulator or physical device

## Testing

Unit tests are provided for the authentication components:

- `AuthRepositoryTest`: Tests for the Firebase authentication repository
- `AuthViewModelTest`: Tests for the authentication ViewModel

Run tests using:
```
./gradlew test
```

## Implementation Details

- **Authentication**: Firebase Authentication with email/password
- **UI**: Jetpack Compose for modern UI development
- **Architecture**: MVVM pattern with Repository layer
- **State Management**: Kotlin Flow for reactive state updates 