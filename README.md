# MCSR Discord Presence

![Discord Rich Presence preview](docs/presence-preview-fixed.jpg)

Discord Rich Presence client mod for **Minecraft Java 1.16.1 + Fabric Loader**, made for Minecraft speedrunning.

## Features

- Large Discord icon matching the current stage/location.
- Overworld, Nether, Bastion, Fortress, Second Portal, Searching for Stronghold, Stronghold and The End states.
- `Waiting for run...` outside a run.
- Native Discord elapsed timer using one stable run start timestamp, so changing location does not reset the timer.
- No SpeedRunIGT dependency.
- No Fabric API dependency.

## Install

Download the latest JAR from **Releases**, put it in your Minecraft `mods` folder, and launch Minecraft 1.16.1 with Fabric Loader while Discord Desktop is running.

## Build

Build with:

```powershell
.\gradlew.bat clean build
```

The built JAR is placed in `build/libs/`.

## Discord assets

Application ID: `1557417651234410516`

Asset keys: `overworld`, `nether`, `bastion`, `fortress`, `portal`, `eye_of_ender`, `strongholdportalroom`, `endstone`.

## Speedrun rules

This is a custom mod and is **not automatically allowed for official leaderboard submissions**. Check the current Minecraft speedrunning rules/mod whitelist before submitting a run.
