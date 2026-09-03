============================================
SALEX POS - GOOGLE DRIVE BACKUP SETUP
============================================

WHY YOU NEED THIS SETUP
-----------------------
The app uploads your data backups to Google Drive using the official
Google Drive API (no folder picker needed - it uses a fixed folder
named "salexpos_backups" on each user's own Drive).

This only works AFTER the app is registered with Google Cloud as a
Drive app for YOUR signing certificate. Without this step, tapping
"Sign in to Google Drive" fails with error code 10.

The app is NOT published on the Play Store. It is distributed as a
DEBUG APK that customers sideload (install the .apk file directly).
This is fine for Drive - it just means you must register the DEBUG
signing SHA-1 in Google Cloud (see PART B).

The sign-in uses Google Play services, so you MUST:
  1) Complete the one-time Google Cloud setup below (developer/admin only)
  2) Make sure Google Play services is up to date on each phone
  3) Make sure the user has a Google account added on the phone

------------------------------------------------------------
PART A - ONE-TIME GOOGLE CLOUD SETUP (DEVELOPER / ADMIN)
------------------------------------------------------------
These steps must be done by the app owner in the Google Cloud Console.
Customers CANNOT do these steps - they only sign in with their own
Google account afterwards.

1) Sign in to https://console.cloud.google.com and open/create the
   SAME project used by your google-services.json (Firebase).

2) Enable the Google Drive API:
   - Go to "APIs & Services" > "Library"
   - Search "Google Drive API"
   - Click it, then click "Enable"

3) Create an OAuth consent screen (once):
   - "APIs & Services" > "OAuth consent screen"
   - Choose "External"
   - App name: e.g. "Salex POS"
   - Add your email, then Save.
   - In "Test users", add the email(s) of people who will sign in.
     (Add every customer email here, OR you can leave it as a testing
     status - older sideloaded sign-ins generally work without the app
     being fully "published".)

4) Create an Android OAuth Client ID:
   - "APIs & Services" > "Credentials" > "+ Create Credentials"
   - Choose "OAuth client ID" > "Application type: Android"
   - Package name:  com.patechltd.salexfypos
   - SHA-1 signing certificate fingerprint: <YOUR_SIGNING_SHA1>
   - You MUST use the DEBUG SHA-1 (see PART B) because you ship a
     DEBUG apk.

------------------------------------------------------------
PART B - FIND YOUR DEBUG SIGNING SHA-1 (REQUIRED, DEBUG APK)
------------------------------------------------------------
Because you distribute a DEBUG apk, register the DEBUG keystore SHA-1.

The default debug keystore is:  ~/.android/debug.keystore

Get its SHA-1 with:
  keytool -list -v -keystore ~/.android/debug.keystore \
          -alias androiddebugkey -storepass android -keypass android \
          | grep SHA1

This machine's debug SHA-1:
  3B:6A:64:87:87:3C:78:D2:4B:E8:FD:50:2A:B3:06:EC:3D:23:BD:55

IMPORTANT: The debug keystore is per-machine. If you build the apk on
a DIFFERENT machine, that machine has its own debug keystore and SHA-1,
and you must register THAT machine's SHA-1 too. The debug apk built on
machine A will only sign in if machine A's SHA-1 is registered.

If you ever switch to a signed RELEASE apk, find its SHA-1 instead:
  keytool -list -v -keystore <your-release.keystore> -alias <alias> \
          | grep SHA1

------------------------------------------------------------
PART C - INSTALL + HOW A CUSTOMER CONNECTS (self-serve)
------------------------------------------------------------
Install (sideload), done once per phone:
  1) Allow "Install from unknown sources" for whatever app you use
     to open the .apk (WhatsApp, Files, Drive, etc.).
  2) Open the .apk and tap Install.
  3) If "Play Protect" warns about an unknown app, tap "Install anyway"
     / "More details > Install anyway".

Connect their Drive, done once per user:
  1) Make sure the phone has Google Play services updated and a Google
     account signed in (Settings > Accounts > Add account > Google).
  2) Open the app: Settings > Backup & Restore.
  3) Tap "Sign in to Google Drive" and approve the permission.
  4) Turn ON "Automatic Google Drive backup".
  5) Tap "Upload backup to Drive now" for an immediate backup.

The app stores everything in each user's own Drive folder:
  salexpos_backups
No folder picking required. Each customer's backups go to their own
Google account.

------------------------------------------------------------
TROUBLESHOOTING
------------------------------------------------------------
"Drive sign-in failed: 4"     -> Update Google Play services.
"Drive sign-in failed: 7"     -> No Google account on the phone.
                                Add one in Settings > Accounts.
"Drive sign-in failed: 8"     -> Sign-in screen didn't complete; retry.
"Drive sign-in failed: 10"    -> App not registered / wrong SHA-1
                                (check PART A + PART B), or missing
                                Google account / Play services.
"Drive sign-in failed: 12501" -> Cancelled, or scope not approved.
                                Use the same Google account that is on
                                the phone and approve all permissions.

============================================
END OF SETUP INSTRUCTIONS
============================================
