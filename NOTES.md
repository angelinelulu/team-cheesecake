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

## Open questions
- Screen time source: Android `UsageStatsManager` (needs special access grant) vs. Health Connect.
- How proximity/location ties into "time with friends" — background location has strict Android
  policy requirements, worth scoping down for a hackathon demo (e.g. manual check-in instead of
  continuous background tracking).
- Backend/sharing: needs some server or realtime store if pet status is shared between friends.
