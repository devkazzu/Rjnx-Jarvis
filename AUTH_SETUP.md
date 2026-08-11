# Anu Thapa Authentication Setup

The app now uses authentication as a hard gate: the main assistant UI is only shown after a real authenticated Firebase user exists.

## Required Firebase setup
1. Add your Firebase Android project configuration as `app/google-services.json`.
2. Enable Email/Password, Google, Phone, and GitHub providers in Firebase Authentication.
3. For GitHub, configure the GitHub OAuth app/client ID and secret in Firebase.
4. For Phone authentication, configure the allowed regions and SMS/verification settings in Firebase.
5. Configure the SHA-1/SHA-256 fingerprints for the Android app in Firebase/Google where required.

## Important
The UI contains Google, GitHub, phone, and email entry points. Google OAuth requires the Firebase project's Android/Web client configuration; GitHub and phone require their Firebase provider configuration. No guest bypass is provided.

The current source ZIP does not include secrets or `google-services.json`.
