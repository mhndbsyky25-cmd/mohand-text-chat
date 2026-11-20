# Mohand Chat for Android

This is a text-only Android client for the existing `/chat` JSON interface. It uses a right-to-left Arabic UI and has `minSdk 23` (Android 6.0), with no image-generation screen.

The app does not contain an AI key. Before replies can work, deploy the server from `app.py`, configure `OPENROUTER_API_KEY` on that server, and enter the server's HTTPS URL ending in `/chat` in the app's settings. The public Netlify page is separate from that server endpoint.

To build locally, install Android SDK platform 35 and Gradle 8.9, then run:

```sh
gradle -p android assembleDebug
```

GitHub Actions also builds `app-debug.apk` on pull requests that change this project and uploads it as the `mohand-chat-debug-apk` artifact when the build succeeds.
