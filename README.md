# SED Keyboard

A small native Android keyboard built without third-party runtime libraries.

## Current features
- English QWERTY and a basic Bangla character layout
- Shift, delete, space, enter, number/symbol page
- Lightweight on-device word completion suggestions
- Dark key styling
- No internet permission, ads, analytics, or external SDKs

## Build
GitHub Actions builds a debug APK on pushes to `main`. Download it from the Actions run's Artifacts section.

## Install
Install the APK, open SED Keyboard, enable it in Android keyboard settings, then choose it from the input-method picker.

## Limitations
This is a lightweight foundation, not feature parity with Gboard. Bangla input is a direct character layout, not phonetic transliteration. Emoji button currently opens the symbol page; glide typing, voice input, cloud sync, and advanced predictive typing are not included.
