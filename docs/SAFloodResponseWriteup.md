# SAFloodResponse - San Antonio Flood Response App

**Team Members:**
- Enrique Matta-Rodriguez

## App Overview
SAFloodResponse is a community-driven Android application for real-time flood reporting, tracking, and response coordination in San Antonio, TX. It enables residents to report floods with location data, view reports on interactive maps, and participate in community discussions during emergency situations.

## APIs & Key Android Features Used

1. **Google Maps API**: Core to our application for displaying flood reports geospatially with custom markers and info windows. 

2. **Firebase APIs**:
   - Firebase Authentication API
   - Firestore Database API
   - Firebase Storage API
   - Firebase App Check API

3. **Android Features**:
   - **Jetpack Compose** used extensively throughout the application
   - Custom navigation drawer for app-wide navigation
   - Location services and GPS integration
   - Camera integration for photo capture
   - Material Design 3 components and animations

## App Images and Movie

<table>
  <tr>
    <td><img src="./Admin.png" alt="Admin screen" width="300" /></td>
    <td><img src="./Dashboard.png" alt="Dashboard" width="300" /></td>
    <td><img src="./Discussion.png" alt="Discussion" width="300" /></td>
  </tr>
  <tr>
    <td><img src="./Profile.png" alt="Profile" width="300" /></td>
    <td><img src="./Report.png" alt="Report" width="300" /></td>
    <td></td>
  </tr>
</table>

## Third-Party Libraries

1. **Kotlin Coroutines & Flow**: Used for asynchronous programming throughout the application. This library significantly simplified our background operations for data fetching and processing, though we faced some challenges with proper scoping in certain ViewModels where lifecycle management required careful implementation.

2. **MockK**: Used for our testing framework to create mock objects. This library greatly enhanced our testing capabilities, particularly when simulating Firebase responses, although the learning curve was initially steep when working with coroutine-based suspend functions.

3. **Coil**: Use for our image management and manipulation. This library simplifies the process of handling images in the Android OS. We did face issues with posting to Firestore Storage which has not been resolved as of this writing. 

## Third-Party Services

1. **Firebase Authentication**: Provides user account management and security. Firebase Auth simplified our login workflow considerably, though we faced some challenges implementing proper error handling for edge cases like account linking and session timeouts.

2. **Firebase Firestore**: Real-time NoSQL database that stores all flood reports, discussions, and user data. The real-time capabilities were excellent for our use case, but we had to develop careful indexing strategies to manage query performance as our data grew.

3. **Firebase Storage**: Used for storing user-submitted photos of flood incidents. The integration with Firestore was seamless, though we had to implement custom compression and size limiting to optimize upload performance on varying network conditions.

5. **Google Maps Platform**: Used beyond just the Android SDK, leveraging their geocoding and place search APIs. These were critical for accurate location reporting, though cost management required implementing caching strategies.

## UI/UX Design Highlights

Our application employs a disaster-centric design philosophy focused on clarity and accessibility during emergency situations:

1. We implemented a high-contrast color system for flood severity indicators that works across accessibility needs, including color blindness considerations.

2. The app features offline capabilities for the map interface, storing recently viewed reports locally with clear indicators for possibly outdated information.

3. Our navigation structure prioritizes rapid reporting with a prominent floating action button that remains accessible throughout the app.

4. We implemented a custom location selection interface that combines map-based and text-based input methods to accommodate various user preferences and abilities.

## Backend Architecture Highlights

1. The application implements a multi-layer validation system for flood reports:
   - Client-side validation on submission
   - Server-side Firebase functions that verify location data
   - Community verification through confirmations/denials

2. We developed a custom geospatial simulation system that models flood spread based on topographical data and existing reports, using a modified cellular automaton approach.

3. Our data synchronization strategy implements intelligent background syncing that prioritizes critical reports over discussion updates, optimizing for limited bandwidth during emergency situations.

4. The authentication layer includes role-based permissions with administrator capabilities for official emergency responders.

## Most Valuable Learning Experience

1. **Full Project Lifecycle**: Completing an app from start to finish was an invaluable experience. Reaching an alpha release milestone provided unique insights into the entire development process. Since most of my professional work involves existing codebases, building something from scratch offered a fresh perspective on software development.

2. **Android Tech Stack**: Gaining proficiency with Android development has significantly expanded my capabilities as a developer. I now feel confident in my ability to develop various mobile applications for clients. While the course introduced fundamental concepts, my deeper exploration of the Android documentation revealed additional tools and processes that proved particularly valuable.

## Most Difficult Challenge

During development, I faced two significant challenges that impacted the project timeline and quality:

1. **Compressed Timeline Management**: The strict project deadline forced difficult trade-offs in development priorities. Most notably, I had to reduce UI testing coverage beyond what I consider professionally acceptable. This compromise, while necessary to meet deadlines, highlighted the importance of establishing realistic development scopes that accommodate proper testing phases.

2. **AI-Generated Code Accidents**: In an attempt to accelerate development, I experimented with using a specialized AI agent (DeepSeek v3 0324) for code generation. While initially promising, the generated code required extensive refactoring and debugging due to quality issues. What was intended as a productivity boost ultimately cost me three days of debugging time, teaching me valuable lessons about the current limitations of AI coding assistants and the importance of careful evaluation before integrating generated code.

## Build & Run Instructions

1. Clone the repository from GitHub
2. Create a `local.properties` file in the project root with the following:
   ```
   sdk.dir=/path/to/your/Android/sdk
   MAPS_API_KEY=your_google_maps_api_key
   ```
3. Create a Firebase project and add an Android app with package name `edu.utap.safloodresponse`
4. Download the `google-services.json` file and place it in the `app/` directory
5. In the Firebase console, enable:
   - Authentication (Email and Google sign-in)
   - Firestore Database
   - Storage
   - App Check
6. Enable the Maps SDK for Android in the Google Cloud Console associated with your Firebase project
7. Build and run the application using Android Studio

For testing purposes, you can use the debug mode which has sample data populated automatically.