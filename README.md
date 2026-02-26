# HoloUI

A high-performance holographic UI library for Minecraft (Paper), designed to render complex interactive interfaces using NMS-based displays with zero-raycast click detection.

## Features

- **NMS Display Entities**: Smooth, packet-based rendering using `TextDisplay` and `ItemDisplay`.
- **Zero-Raycast Interaction**: Per-player virtual `Interaction` entities allow for precise, high-performance click detection without server-side raycasting.
- **HoloTrigger System**: A generalized world-based trigger system to manage interaction points for HUDs and other virtual elements.
- **Netty Packet Interception**: Low-level packet listeners handle interactions with virtual entities efficiently.
- **Declarative API**: Build HUDs with simple, lambda-based click callbacks.
- **Version Independent API**: Core logic is abstracted from NMS, allowing for easy version support.

## Usage

### 1. Initialize Controller
```kotlin
val holoController = HoloController(plugin, HoloHandler1214())
holoController.start()
```

### 2. Create a Button
```kotlin
val button = HoloButton(
    text = "<gold>Click Me!",
    location = player.location.add(0.0, 2.0, 0.0),
    onClick = { player, backwards -> 
        player.sendMessage("Button clicked! Backwards: $backwards")
    }
)
```

### 3. Open a HUD
```kotlin
val hud = HoloHUD(player, mannequinId, holoController.handler)
hud.addButtons(listOf(button))
holoController.openHud(hud)
```

## Module Structure

- **`HoloHandler`**: Interface for version-specific NMS operations.
- **`HoloButton`**: Definition of individual UI elements.
- **`HoloHUD`**: Manager for a collection of buttons visible to a single player.
- **`HoloController`**: Central hub for HUD lifecycle, triggers, and packet routing.
- **`HoloTrigger`**: World-based interaction points.

## Standalone Development

To build the library on its own:
```bash
./gradlew build
```

## Integration

Add this module as a project dependency in your Gradle build:
```kotlin
dependencies {
    implementation(project(":HoloUI"))
}
```
