# MyTorch

MyTorch is a simple, premium, and fully functional Android flashlight application built with pure Kotlin and Jetpack Compose. It allows users to toggle their device's camera flash LED using an intuitive UI or via a double-shake gesture that works even when the app is in the background or the screen is off.

## Features

- **Double-Shake Gesture**: Shake your phone twice to quickly turn the torch on or off. The gesture is debounced and tuned for reliability.
- **Always-on Background Service**: The shake detection runs continuously in the background (via a Foreground Service) so you can toggle the torch anytime without having to open the app.
- **Boot Persistence**: The background shake service automatically restarts when you reboot your phone (if you had it enabled).
- **Persistent Notification**: A clean notification shows the current torch state and includes a quick "Toggle" action button.
- **Premium Dark UI**: Built with Jetpack Compose, the user interface features a sleek dark mode layout with an amber glowing, pulsing power button that dynamically syncs with the torch's hardware state.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: Service-Driven (Foreground Services, WakeLocks, Sensors)
- **Hardware Integration**: Android Camera2 API (`CameraManager`) for seamless and state-synced hardware control.

## Installation

1. Clone this repository: `git clone https://github.com/AMKalyan/MyTorch.git`
2. Open the project in **Android Studio**.
3. Build and run the app on a physical Android device (emulators do not have a real flash LED or a physical accelerometer to test the shake features).

## Permissions Required

The application requests the following permissions for its core functionality:
- `CAMERA` (To control the flash LED)
- `VIBRATE` (For haptic feedback on toggle)
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` (To keep shake detection alive in the background)
- `POST_NOTIFICATIONS` (To show the persistent toggle notification)
- `WAKE_LOCK` (To keep accelerometer active when the screen turns off)
- `RECEIVE_BOOT_COMPLETED` (To automatically start the service on reboot)

## License

This project is open-source. Feel free to fork and modify!
