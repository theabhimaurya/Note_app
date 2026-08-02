# Notes App 📝

A modern, clean, and feature-rich Notes application built with **Jetpack Compose**, **Supabase**, and **WebRTC**. This app follows Clean Architecture principles and delivers a fast, responsive user experience for personal note-taking, AI-assisted tools, and real-time communication.

---

## 🚀 Key Features

### 💬 Real-Time Communication & WebRTC Calling
- **1-on-1 Real-time Chat**: Instant messaging powered by **Supabase Realtime** channels and PostgREST.
- **Chat List & Contact Discovery**:
  - Search contacts dynamically in real-time.
  - **Dynamic Initial Fallback Avatars**: Automatically generates capitalized initial avatars for contacts without profile images.
  - Real-time online/offline presence indicators.
- **P2P Video & Audio Calling**:
  - High-performance peer-to-peer calling using **WebRTC** and **Stream WebRTC Compose**.
  - Integrated STUN & TURN servers for reliable NAT traversal and global relay connectivity.
  - Floating local video preview with drag support.
  - Audio and video toggles (Mute/Unmute, Camera On/Off).
  - Real-time call duration timer and connection state management.
- **Background Call Notifications & FCM**:
  - Incoming call push notifications triggered through **Firebase Cloud Messaging (FCM)** and **Supabase Edge Functions**.
  - Dedicated full-screen **Incoming Call Screen** with Accept/Decline actions and ringtone/vibration.
  - **Killed-State Call Handling**: Direct cold-start routing into the active video call when accepted from notification or screen lock.

---

### 📷 Smart QR Code Generation & Scanner System

The app features an integrated, high-performance QR code ecosystem for seamless user discovery and instant profile sharing:

#### 1. QR Code Generation (`QRCodeScreen` & `QRUtils`)
- **ZXing QR Engine**: Uses `com.google.zxing:core` (`QRCodeWriter`) to generate sharp, error-corrected QR code bitmaps (`512x512` resolution, `RGB_565` format) with UTF-8 character encoding.
- **Unique Profile Encoding**: Encodes the authenticated user's unique `userId` into a standardized format.
- **Modern Glassmorphic UI**: Rendered inside a rounded card with gradient borders (`#3652FF` to `#091650`) and translucent glassmorphism layers.
- **Quick Actions**:
  - Save/Download the QR code bitmap to local device storage.
  - Copy unique User ID to clipboard.
  - One-tap button to switch directly to the Scanner.

#### 2. QR Code Scanner (`QRScannerScreen`)
- **Google ML Kit Barcode Vision**: Uses `com.google.android.gms:play-services-mlkit-barcode-scanning` configured strictly for `Barcode.FORMAT_QR_CODE` for instantaneous detection and minimal CPU overhead.
- **CameraX Stream Pipeline**:
  - Built with `androidx.camera:camera-camera2` and `androidx.camera:camera-lifecycle`.
  - `ImageAnalysis` set to `STRATEGY_KEEP_ONLY_LATEST` running on a dedicated background `Executor` to avoid frame drops.
- **Instant Chat Handshake**:
  - Automatically requests runtime `CAMERA` permission with graceful fallback handling.
  - Upon scanning a valid contact QR code, parses the scanned `userId` and immediately navigates directly to `Screen.Chat` (`Screen.Chat.passUserId(userId)`), popping the scanner from the backstack for a smooth flow.

---

### ⚡ Optimized App Launch & Session Management
- **Instant Launch Performance**: Local auth token and user profile caching via `SessionManager` (`SharedPreferences`).
- **Zero-Network Splash Screen**: Fast local check with zero API delay on launch.
- **Single Custom Splash Screen**: Clean, branded splash screen displaying the document icon and "Notes App" title without duplicate system splash screen flicker.
- **Secure Data Wipe on Logout**: Fully clears local session data, cached tokens, and navigation backstack on logout.

---

### 🤖 AI & Smart Tools
- **AI Text Recognition (OCR)**: Extract editable text from images using **Google ML Kit Vision**. Capture photos via **CameraX** or pick from gallery to instantly populate note content.
- **Universal Search**: Fast local and remote search across all notes and chat contacts.

---

### 📝 Notes Management
- **Dynamic Staggered Grid**: Clean responsive layout for notes across all screen sizes.
- **Full CRUD Support**: Create, read, update, and delete notes synchronized with Supabase database.
- **Category Filtering**: Organize notes by categories (Personal, Work, Voice, Food, etc.).
- **Interactive Note Previews**: Bottom sheet previews with instant actions.

---

### 🎨 Personalization & UI/UX
- **Material 3 Design System**: Curated color schemes, typography, and smooth micro-animations.
- **Dark & Light Mode**: Theme state managed via Jetpack DataStore and synchronized across the entire application.
- **Custom Toasts & Dialogs**: Animated feedback indicators and alert dialogs.

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 |
| **Backend & Auth** | [Supabase](https://supabase.com/) (Auth, PostgREST, Realtime, Edge Functions) |
| **P2P Audio/Video** | [WebRTC](https://webrtc.org/) & [Stream WebRTC Android](https://github.com/getstream/stream-webrtc-android) |
| **Push Notifications** | [Firebase Cloud Messaging (FCM)](https://firebase.google.com/docs/cloud-messaging) |
| **AI / OCR** | [Google ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition) |
| **QR Code Scanner** | [Google ML Kit Barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning) |
| **QR Code Generator**| [ZXing ("Zebra Crossing") Core](https://github.com/zxing/zxing) |
| **Camera** | [CameraX](https://developer.android.com/training/camerax) (`camera-camera2`, `camera-lifecycle`, `camera-view`) |
| **Dependency Injection** | [Hilt / Dagger](https://dagger.dev/hilt/) |
| **Local Storage** | Jetpack DataStore (Theme) & SharedPreferences (SessionManager) |
| **Networking** | [Ktor Client](https://ktor.io/) & Java HttpURLConnection |
| **Serialization** | [KotlinX Serialization](https://github.com/Kotlin/kotlinx.serialization) |

---

## 🏗 Architecture & Project Structure

```text
com.live.notesapp/
├── data/
│   ├── local/               # Local SessionManager & DataStore
│   ├── model/               # Data Transfer Objects (DTOs)
│   └── repository/          # Repository implementations (Auth, Notes, Chat, Call)
├── di/                      # Hilt Dependency Injection Modules
├── domain/
│   ├── manager/             # TokenManager & session domain logic
│   ├── model/               # Domain entities (Note, ChatUser, ChatRoom, CallSignal)
│   └── repository/          # Repository contracts / interfaces
├── notification/            # FirebaseMessagingService & CallNotificationHelper
├── presentation/
│   ├── ai/                  # AI Text Recognition & OCR Screen
│   ├── auth/                # Login & Sign-up screens
│   ├── call/                # IncomingCallActivity, VideoCallScreen & ViewModels
│   ├── chat/                # ChatListScreen, ChatScreen & ViewModels
│   ├── navigation/          # NavGraph, Bottom Navigation & Route definitions
│   ├── notes/               # Notes list, Add/Edit note & note detail screens
│   ├── profile/             # ProfileScreen, QRCodeScreen & QRScannerScreen
│   ├── settings/            # Settings & theme selector
│   └── splash/              # Custom branding SplashScreen
├── ui/theme/                # Material 3 Color palette, Typography, and Theme engine
├── utils/                   # Constants, QRUtils (ZXing), Date formatters & helpers
└── webrtc/                  # WebRtcSessionManager (PeerConnection, ICE & Audio setup)
```

---

## ⚙️ Setup & Configuration

1. **Clone the repository**:
   ```bash
   git clone https://github.com/theabhimaurya/Note_app.git
   ```

2. **Open in Android Studio**:
   - Open the project in Android Studio (Ladybug 2024.2.1 or newer recommended).

3. **Configure Supabase & Firebase**:
   - Add your `google-services.json` to the `/app` directory.
   - Configure your Supabase project credentials in [Constants.kt](file:///d:/Android%20Projects/Notesapp/app/src/main/java/com/live/notesapp/utils/Constants.kt):
     ```kotlin
     const val SUPABASE_URL = "https://<your-project>.supabase.co"
     const val SUPABASE_ANON_KEY = "<your-anon-key>"
     ```

4. **Build and Run**:
   - Build and install the debug APK:
     ```bash
     ./gradlew assembleDebug
     ```
   - Deploy to a physical device to test camera, QR code scanner/generator, OCR, and WebRTC calling.

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
