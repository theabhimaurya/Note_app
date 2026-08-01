# Notes App 📝

A modern, clean, and feature-rich Notes application built with **Jetpack Compose** and **Supabase**. This app follows clean architecture principles and provides a seamless user experience for managing daily thoughts, tasks, and real-time communication, now enhanced with AI and WebRTC.

## 🚀 Key Features

### 💬 Real-time Communication
- **Instant Messaging**: Seamless real-time chat functionality powered by Supabase Realtime.
- **Video & Audio Calling**: High-quality peer-to-peer video calls using **WebRTC**, featuring:
    - Dedicated call screen with duration tracking.
    - Front/Rear camera switching.
    - Audio toggle and hands-free options.
    - Incoming call notifications.

### 🤖 AI & Smart Tools
- **AI Text Recognition (OCR)**: Extract text from images using **Google ML Kit**. Capture photos via CameraX or select from gallery to instantly generate notes.
- **Smart QR Scanner**: Integrated barcode and QR code scanner for quick interactions and user connections.
- **Advanced Search**: Real-time, fuzzy search across all your notes to find information instantly.

### 📝 Smart Notes Management
- **Dynamic Grid Layout**: Beautiful staggered grid for notes, optimized for various screen sizes.
- **Full CRUD Operations**: Create, Read, Update, and Delete notes with real-time sync.
- **Category Filters**: Organize and filter your notes by types (e.g., Personal, Work, Voice, Food).
- **Interactive Previews**: Quick-view notes using polished Bottom Sheets.

### 🎨 Personalization & UX
- **Theme Engine**: Full support for **Dark Mode**, Light Mode, and System Default, persisted with **Jetpack DataStore**.
- **User Profiles**: Personalized user profiles with customizable details and QR-based identification.
- **Smooth Animations**: Transitions and custom-designed components for a premium feel.
- **Custom Feedback**: Animated progress bar toasts for success and error states.

## 🛠 Tech Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) - Declarative UI toolkit.
- **Backend**: [Supabase](https://supabase.com/) - Auth, Postgrest (Database), and Realtime.
- **Communication**: [WebRTC](https://webrtc.org/) & [Stream WebRTC Compose](https://github.com/getstream/stream-webrtc-android) - P2P Video/Audio calls.
- **AI/ML**: [Google ML Kit](https://developers.google.com/ml-kit) - OCR and Barcode Scanning.
- **Architecture**: MVVM with Clean Architecture principles.
- **DI**: [Hilt](https://dagger.dev/hilt/) - Dependency injection.
- **Networking**: [Ktor](https://ktor.io/) - HTTP client and WebSockets.
- **Storage**: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) - Preference persistence.
- **Camera**: [CameraX](https://developer.android.com/training/camerax) - Camera integration.
- **Serialization**: [KotlinX Serialization](https://github.com/Kotlin/kotlinx.serialization) - JSON parsing.

## 🏗 Architecture

The project follows the **Clean Architecture** pattern:
- **Domain Layer**: Business logic, use cases, and repository interfaces.
- **Data Layer**: Repository implementations, API services (Supabase/Ktor), and local storage.
- **Presentation Layer**: UI (Compose), ViewModels, and State management.

## 📸 Screenshots

*(Add your screenshots here)*

## 📦 Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/theabhimaurya/Note_app.git
   ```
2. **Open in Android Studio**: Use Ladybug (2024.2.1) or newer.
3. **Configure Supabase**: 
   - Create a project at [supabase.com](https://supabase.com).
   - Add your `SUPABASE_URL` and `SUPABASE_ANON_KEY` in `com.live.notesapp.util.Constants.kt`.
4. **Build and Run**: Deploy to a physical device for the best experience (especially for Camera and WebRTC features).

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an issue.

## 📄 License

This project is licensed under the MIT License.
