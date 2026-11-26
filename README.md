# Coin Flip

A simple, fair coin-flipping app for Android, built with Kotlin and Jetpack Compose.
It's set up as a starting point for publishing on Google Play.

## Features
- 3D flip-and-toss animation, with haptic feedback when the coin lands
- Fair 50/50 results from `SecureRandom`
- Stats: heads and tails counts with percentages, current streak, longest streak
- The last 30 flips as a history row
- Stats are saved on the device, and you can reset them
- Material 3 with dynamic color (Android 12+), light and dark themes, edge-to-edge
- No internet permission, no ads, no analytics, no data collected

## Project layout
```
app/src/main/java/com/coinflipper/app/
  MainActivity.kt          entry point
  CoinFlipViewModel.kt     holds state, decides results
  data/FlipStats.kt        pure stats logic + Coin RNG (unit tested)
  data/StatsRepository.kt  SharedPreferences persistence
  ui/CoinFlipScreen.kt     main screen
  ui/CoinView.kt           the animated coin
  ui/Theme.kt              Material 3 theme
app/src/test/...           JUnit tests
```

## Build and run
1. Install the latest **Android Studio** and open this folder.
2. Let Gradle sync. If Studio offers to update AGP, Kotlin or library versions, accept.
3. Press **Run** to start it on an emulator or a phone.

From the command line: `./gradlew assembleDebug` and `./gradlew test`.

Without Android Studio: push to GitHub and the **Build release bundle** workflow
(`.github/workflows/build.yml`) produces a signed `.aab` you can download.

> **Start here:** `PUBLISH-STEPS.md` has the remaining steps in order.

## Before publishing: checklist (reference)
1. **Pick your app ID.** In `app/build.gradle.kts`, change `applicationId` (and
   `namespace`) from `com.coinflipper.app` to something unique you own, for example
   `com.yourname.coinflip`. Once you publish, you **can't change it**. If you change
   `namespace`, move the Kotlin package folders to match, or use Studio's
   *Refactor → Rename* on the package.
2. **App name and icon.** Edit `app_name` in `res/values/strings.xml`. The icon is
   a vector adaptive icon in `res/drawable/ic_launcher_foreground.xml`; you can swap it
   with *File → New → Image Asset* in Studio.
3. **Create an upload key** (one time only, and keep it backed up safely):
   ```
   keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
   Put the `.jks` file in the project root. Copy `keystore.properties.example` to
   `keystore.properties` and fill in the passwords. Both files are git-ignored.
4. **Build the bundle:** `./gradlew bundleRelease` writes
   `app/build/outputs/bundle/release/app-release.aab`.
   (Or use *Build → Generate Signed App Bundle* in Studio.)
5. **Every update:** raise `versionCode` by 1 and update `versionName`.

## Google Play Console steps
1. Create a developer account at https://play.google.com/console (one-time $25 fee).
   New **personal** accounts have to run a closed test with at least 12 testers for
   14 days before they can publish to production.
2. **Create app**: enter the name and default language, choose App and Free.
3. Under **App content**, complete:
   - **Privacy policy**: host `PRIVACY.md` publicly (for example on GitHub Pages or
     Google Sites) and paste the URL.
   - **Data safety**: answer *No* to collecting or sharing data.
   - **Ads**: No. **Content rating**: fill in the questionnaire (it rates as Everyone).
   - **Target audience**: pick the age groups. Don't include under-13s unless
     you're ready for the Families policy requirements.
4. **Store listing**: you need a 512×512 icon, a 1024×500 feature graphic, at least
   2 phone screenshots, a short description (80 characters max) and a full
   description (4000 characters max).
5. **Testing → Internal / Closed testing**: upload the `.aab`, add testers, then
   promote the release to **Production** when it's ready.

Example short description: *Flip a fair coin with a realistic 3D animation and track your streaks.*

## Target SDK
The project targets API 36. Google Play raises the minimum target API level every
year (usually in August), so update `compileSdk` and `targetSdk` when it does.

## Ideas for next steps
- Sound effect on landing (`SoundPool`)
- Shake to flip (accelerometer)
- Custom coin skins and labels (for example Yes/No)
- Multi-coin mode, or "best of N"
- Translations (`res/values-xx/strings.xml`)
