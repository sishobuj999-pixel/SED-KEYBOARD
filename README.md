# SED Keyboard

A lightweight native Android keyboard built with Android's built-in UI and input-method APIs. No third-party runtime libraries.

## Included in this build
- English QWERTY layout with one-shot shift
- Basic Bangla character layout and manual English/Bangla switching
- Local word-completion suggestions from a small built-in word list
- Number and symbol page
- Small emoji panel
- Dark keyboard preference
- Optional key vibration
- Enter key respects common editor actions
- No internet permission, ads, analytics, or cloud service

## Setup
1. Install the APK.
2. Open SED Keyboard and enable it in Android keyboard settings.
3. Choose SED Keyboard from the input-method picker.
4. Use the language key to switch English/Bangla. Long-press the space key to switch too.

## Build
GitHub Actions builds a debug APK on pushes to `main`. Open the latest successful workflow run and download the `SED-Keyboard-debug` artifact.

## Current limitations
This is an early lightweight keyboard, not Gboard feature parity. Bangla is currently a direct character layout, not phonetic/transliteration typing. Suggestions are a small static on-device list, not a learned predictive model. Voice typing, glide typing, clipboard history, full emoji search, themes, multilingual transliteration, and cloud sync are not implemented.
