# team-cheesecake
SESAxWDCC Hackathon

## Doomscroll Pet (Android)

A virtual pet whose health reacts to your screen time — see [NOTES.md](NOTES.md) for the full
concept brainstorm.

### Project structure

```
app/src/main/java/com/teamcheesecake/doomscrollpet/
  MainActivity.kt        # bottom-nav shell (Pet / Friends / Stats tabs)
  model/                 # PetUiState, PetViewModel (in-memory placeholder state)
  screens/                # PetHomeScreen, FriendsScreen, StatsScreen
  ui/theme/               # Compose theme, colors, type
```

Kotlin + Jetpack Compose, minSdk 26. Screen-time and location integrations are stubbed as
TODOs — see NOTES.md open questions.

### Getting started

Open the repo root in Android Studio (Gradle project) and let it sync — it will generate the
Gradle wrapper automatically. Then run the `app` configuration on a device or emulator.
