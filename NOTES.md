# Concept notes — Doomscroll Pet

A virtual pet that reacts to your phone habits.

- Pet gets sick the more you doomscroll; gets healthy when you take steps/breaks
  (connect to Health Connect / screen time APIs; perks for using Canvas or Notes apps).
- Being physically close to friends (GPS/location) restores pet health.
- More time off phone/social media → more perks (food/water/toys for the pet).
- If the pet's too sick, trigger a screen-downtime nudge ("save your pet...") — guilt trip mechanic.
- Need to define what counts as "doomscrolling time" (which apps, how it's measured).
- Social layer: pet status can be shared with friends
  ("I'm so sick!! Emily's been on TikTok for 5 hours!! :(((( ").
- Duolingo-style engagement tactics: streaks, badges.
- Platform: started as iOS idea, now building the Android version first.

## Screen time + health (implemented)

`ScreenTimeRepository` reads real per-app foreground minutes today via `UsageStatsManager`,
scoped to the packages picked in onboarding (`avoidApps`/`moreApps`, matched by package name via
`AppOption` in `PetState.kt` — best-effort package names for the common builds of each app, not
verified against what's actually installed). `HealthRepository` reads today's step count via
Health Connect. Both refresh on app resume and once on launch (`MainActivity.MainAppScreen`);
the Connect screen's buttons do the real permission flows (usage-access settings intent,
Health Connect's permission contract, Play Store fallback if Health Connect isn't installed).

Health formula (`PetViewModel.recomputeHealth`): 80 − (doomscroll minutes × 2) + (good-app
minutes × 1) + (steps ÷ 500) + proximity bonus, clamped 0–100. Shown on the Pet home screen along
with today's doomscroll/good-app minutes and step count, plus which sources aren't connected yet.

Open question: the app-to-package-name mapping is a guess (e.g. TikTok →
`com.zhiliaoapp.musically`) — worth verifying against what's actually on the demo phones before
relying on it.

## Backend/sharing
- Needs some server or realtime store if pet status is shared between friends beyond location.

## Location tracking (implemented)

Firestore-backed, no login — each install generates a local 6-character "friend code"
(`DeviceIdentity`), publishes its lat/lng to `locations/{code}` in Firestore every 15s while the
app is foregrounded (`LocationRepository`), and listens to friends' docs by code to compute
distance. Within 100m triggers a local notification + a small pet-health bump
(`ProximityNotifier`, `PetViewModel.addFriend`).

Scoped deliberately: foreground-only (no background service/location permission), no Firebase
Auth (Firestore is wide open — fine for a hackathon demo, must be locked down with security rules
before this goes further), no push notifications between devices (the "nearby" alert only fires
locally on each device that's polling, not sent to the other person).

Needs a Firebase project with Firestore enabled and `app/google-services.json` in place to build.
