MindFocus

MindFocus is an Android application designed to monitor the user’s concentration level in real time using facial analysis. The app computes a focus score based on facial metrics such as eye openness, head position, and yawning, and provides visual alerts and haptic feedback to help users maintain attention during focus sessions.

📱 Features:
- Real-time facial analysis using the device camera
- Focus score computation based on facial metrics (EAR, MAR, head pose)
- Personalized calibration to adapt to each user
- Focus monitoring sessions with pause and stop controls
- Visual alerts and haptic feedback (vibration) when focus decreases
- Session history with stored results for later review
- Local data storage using a built-in database (offline usage)

⬇️ Download & Installation:
The application can be downloaded directly as an APK from the GitHub Releases page.

👉 Steps to install:
1. Go to Releases → MindFocus v1.0.0
2. Download the app-debug.apk file
3. Enable “Install from unknown sources” on your Android device (if required)
4. Install the APK
5. Launch the app – no additional configuration is required

Once installed, the application works offline and does not require a server connection.

✅ Device Requirements & Tested Environment:
The application was developed and tested under the following conditions:
- Operating System: Android 10 or higher
- Device Type: Smartphone with front-facing camera
- Camera: Front camera required for facial analysis
- Permissions:
    -> Camera (mandatory)
    -> Location (optional, if enabled in settings)
- Tested on:
    -> Physical Android device (not emulator)
    -> ARM-based smartphone
- Performance: Real-time processing at interactive frame rates on mid-range devices

⚠️ Note: Using the app without completing the calibration step may lead to higher sensitivity and more frequent alerts.

▶️ How to Use:
1. Launch the application
2. Log in or create a new user profile
3. Perform calibration to establish personal baseline metrics
4. Start a focus session from the Home screen
5. Monitor the real-time focus score and alerts
6. Stop the session to save results
7. View previous sessions in the History section

🛠️ Technologies Used:
- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM
- Camera: CameraX
- Facial Analysis: MediaPipe Face Landmarker
- Data Storage: Room (SQLite)
- State Management: ViewModel + StateFlow
- Asynchronous Processing: Coroutines

📂 Project Status:
This project was developed as part of an academic laboratory assignment and represents a functional prototype focused on real-time focus monitoring and user feedback.

Possible future improvements include:
- Long-term statistical analysis
- Personalized focus recommendations
- Environmental noise analysis using microphone input
