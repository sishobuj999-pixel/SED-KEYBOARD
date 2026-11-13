# SED Battle Survival — Godot 4 prototype

An offline, third-person 3D survival-shooter prototype.

## Current implementation
- Procedural low-poly arena with cover and boundary walls
- Third-person player capsule and camera
- Keyboard movement and preliminary touch movement/look input
- Hitscan shooting, ammo, reload, health and kill HUD
- Six basic chasing AI bots
- No network requests in the gameplay scene

## Android build
The repository includes an Android export preset and a GitHub Actions workflow. Push a change under `BattleSurvival/` or manually run **Build SED Battle Survival Android APK** from the Actions tab. The APK is uploaded as a workflow artifact named `SED-Battle-Survival-Android`.

## Important status
This is an early prototype, not a complete battle-royale game. The mobile controls, combat/AI balance, loot, weapons, safe zone, missions, settings, persistence, updater, and on-device testing still need work. An APK build must pass in GitHub Actions before it can be treated as installable.
