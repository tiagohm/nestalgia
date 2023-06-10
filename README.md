<p align="center">
<img src="https://github.com/tiagohm/nestalgia/blob/main/docs/mario-256.png?raw=true" height="128" alt="Nestalgia" />
</p>

<h1 align="center">Nestalgia</h1>

[![Active Development](https://img.shields.io/badge/Maintenance%20Level-Actively%20Developed-brightgreen.svg)](https://gist.github.com/cheerfulstoic/d107229326a01ff0f333a1d3476e068d)
[![CI](https://github.com/tiagohm/nestalgia/actions/workflows/ci.yml/badge.svg)](https://github.com/tiagohm/nestalgia/actions/workflows/ci.yml)
[![CodeFactor](https://www.codefactor.io/repository/github/tiagohm/nestalgia/badge/main)](https://www.codefactor.io/repository/github/tiagohm/nestalgia/overview/main)

Nestalgia is a cross-platform and high-accuracy NES/Famicom emulator built in Kotlin. It's being ported from
discontinued [Mesen](https://github.com/SourMesen/Mesen) NES/Famicom emulator built in C++ and C#.

It implements only the emulator (no debugging tools).

## Features

* High Accuracy
* High Compatibility: Over 290 mappers supported
* NES, Famicom, Famicom Disk System, Dendy, VS System, NSF and NSFe emulation is supported
* Save States, Battery, Overclocking and Cheat Codes
* Configurable Region and Speed
* Fullscreen and Screenshot support
* Audio, Video, Controller and Emulation config support
* Zapper, Ascii Turbo File and Battle Box is supported
* Gamepad Support

<p align="center">
<img src="https://github.com/tiagohm/nestalgia/blob/main/docs/1.png?raw=true" height="400" alt="Nestalgia" />
</p>

## Library

### Install

Add it to your build.gradle:

```gradle
repositories {
    maven { url "https://jitpack.io" }
}
```

and:

```gradle
dependencies {
    implementation "com.github.tiagohm:nestalgia:core:main-SNAPSHOT"
}
```

## Desktop

* [Windows](https://github.com/tiagohm/nestalgia/releases/download/0.11.0/nestalgia-0.11.0-windows.jar)
* [Linux](https://github.com/tiagohm/nestalgia/releases/download/0.11.0/nestalgia-0.11.0-linux.jar)

> Before running, check if you have Java 17 or newer installed.

### Player 1 Controller Keys

* D-Pad: Arrow keys
* B Button: S
* A Button: A
* Select: Space
* Start: Enter

## Gamepad Support

* The gamepad support for Desktop version uses [Jamepad](https://github.com/williamahartman/Jamepad)
* The first detected gamepad will be used as Standard Controller 1, and the second will be Standard Controller 2
* Y Button will take a screenshot
* X Button will save the game's state
* LB/RB restore the previous/next saved game's state
* Left thumbstick can be used as D-Pad
