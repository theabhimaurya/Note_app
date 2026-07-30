# Notes App 📝

A modern, clean, and feature-rich Notes application built with **Jetpack Compose** and **Supabase**. This app follows clean architecture principles and provides a seamless user experience for managing daily thoughts and tasks, now enhanced with AI-powered features.

## 🚀 Features

- **User Authentication**: Secure Sign In and Sign Up flows with real-time validation powered by Supabase Auth.
- **Splash Screen**: Professional entry with automatic session handling (remembers logged-in users).
- **AI Text Recognition (OCR)**: Extract text from images using **Google ML Kit**. Choose from gallery or capture from camera to instantly create notes.
- **Smart Search**: Quickly find your notes with real-time, local search functionality.
- **Dynamic Notes Grid**: A beautiful staggered grid layout for viewing notes, similar to Google Keep.
- **Interactive Notes Management**:
    - **View Notes**: Clean bottom sheet preview of note details.
    - **Add/Edit/Delete**: Full CRUD operations for your notes with real-time updates.
    - **Category Filters**: Quickly filter notes by categories (Notes, Voices, Food, etc.).
- **Settings & Themes**: Full support for **Dark Mode**, Light Mode, and System Default themes, persisted with **Jetpack DataStore**.
- **Modern UI/UX**:
    - Custom designed components and highly rounded inputs.
    - **Custom Toasts**: Polished error and success feedback using animated progress bar toasts.
    - **Responsive Layouts**: Full support for keyboard adjustments and various screen sizes.

## 🛠 Tech Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android toolkit for building native UI.
- **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture.
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/) - Standard way to incorporate Dagger DI into Android apps.
- **Backend**: [Supabase](https://supabase.com/) - Open source Firebase alternative (Auth & Database).
- **AI/ML**: [Google ML Kit](https://developers.google.com/ml-kit) - On-device Text Recognition (OCR).
- **Storage**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) - For persisting user preferences (Theme).
- **Networking**: [Ktor](https://ktor.io/) - Asynchronous client for HTTP requests.
- **Serialization**: [KotlinX Serialization](https://github.com/Kotlin/kotlinx.serialization) - Kotlin-first JSON parsing.
- **Camera**: [CameraX](https://developer.android.com/training/camerax) - For capturing images for OCR.
- **Feedback**: [CustomToast](https://github.com/Swapnil-J-Patil/CustomToast) - Elegant custom toast messages.

## 📸 Screenshots

*(Add your screenshots here later)*

## 📦 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/theabhimaurya/Note_app.git
   ```
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Set up your **Supabase** credentials in `Constants.kt`.
4. Build and run the app on your device or emulator.

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/theabhimaurya/Note_app/issues).

## 📄 License

This project is licensed under the MIT License.
