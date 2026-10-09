# SneakyHolos

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
val holoController = HoloController(plugin, HoloHandler262())
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

Versions use `yyyy.MM.dd-<12-character Git hash>`, with the date taken from the commit timestamp in UTC. Gradle derives this automatically and names the JAR `SneakyHolos-<version>.jar`. Builds require a Git checkout.

## Integration

Within the HoloUX workspace, keep the project dependency:
```kotlin
dependencies {
    implementation(project(":SneakyHolos"))
}
```

## Maven publication

Publishes `io.github.team-sneakymouse:sneakholos` (the library JAR) to `https://maven.sneakyrp.com/releases`.

In `~/.gradle/gradle.properties` (or via `ORG_GRADLE_PROJECT_*` env vars):

```properties
sneakyrpUsername=YOUR_UPLOAD_USERNAME
sneakyrpPassword=YOUR_UPLOAD_TOKEN
```

For Reposilite, `sneakyrpUsername` is the token name and `sneakyrpPassword` is its generated secret. Never commit the token.

```bash
./gradlew publishMavenPublicationToSneakyrpRepository
```

GitHub Actions builds and publishes on every push to `main` (Java 25). Credentials come from Infisical (`lords-of-minecraft` / `prod` / `/Maven`) via OIDC. Set `INFISICAL_IDENTITY_ID` and `INFISICAL_DOMAIN` as GitHub Actions secrets for this repository, and allow this repo's `main` branch on the identity's OIDC policy.
