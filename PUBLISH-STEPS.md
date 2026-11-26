# Publishing Coin Flip: what's left for you

Already done for you: the app code, the privacy policy, the signing key, the
GitHub build workflow, the store icon, the feature graphic and the listing text.

The steps below need your own logins, payment or phone, so you have to do them.

## 1. Put the project on GitHub (about 5 minutes)
1. Create a new repository at https://github.com/new, for example `coinflip`.
   Make it **Public** if you want free GitHub Pages for the privacy policy.
2. Upload the contents of the `CoinFlip` folder. Either drag the files into
   GitHub's web page ("uploading an existing file"), or run:
   ```
   git init && git add . && git commit -m "Coin Flip" && git branch -M main
   git remote add origin https://github.com/<you>/coinflip.git && git push -u origin main
   ```
   `keystore.properties` and `*.jks` are git-ignored, so the key stays private.
3. **Settings → Secrets and variables → Actions → New repository secret.** Add the
   4 secrets listed in `SIGNING-SECRETS.txt`: KEYSTORE_BASE64, KEYSTORE_PASSWORD,
   KEY_ALIAS and KEY_PASSWORD.
4. **Actions** tab → *Build release bundle* → **Run workflow**. When it goes green
   (about 5 minutes), open the run and download the **coinflip-release-aab**
   artifact. Unzip it to get `app-release.aab`.
5. **Settings → Pages** → Source: *Deploy from a branch*, branch `main`, folder `/docs`.
   Your privacy policy will be at `https://<you>.github.io/coinflip/privacy.html`.

## 2. Google Play Console
1. Sign up at https://play.google.com/console. It costs a one-time $25, and Google
   verifies your ID and phone number.
2. **Create app**: name *Coin Flip*, App, Free.
3. **App content**: copy the answers from `play-store/listing.txt`.
4. **Main store listing**: paste the text, then upload `play-store/icon-512.png`,
   `play-store/feature-graphic-1024x500.png`, and **at least 2 phone screenshots**.
   For screenshots, install the app on your phone (see step 5) and take them there.
5. **Testing → Closed testing**: create a track, upload `app-release.aab`, and add
   testers by email. Turn on **Play App Signing** when you're asked; that's the default.
   New personal accounts need **12+ testers opted in for 14 days** before
   production access unlocks.
6. After the test period: **Production → Create release** → upload the latest
   `.aab` → submit for review. Reviews usually take a few days.

## Updating the app later
Push to `main` and GitHub builds a new `.aab` with a higher version number
automatically. Upload that file in Play Console.

## Keep safe
Back up `upload-keystore.jks` and `SIGNING-SECRETS.txt` somewhere outside your
computer, such as a password manager. If you lose them, you can ask Google
support to reset the upload key, because Play App Signing is on.
