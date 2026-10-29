# Agent instructions (SneakyHolos)

Paper plugin in Team-Sneakymouse / Lords of Minecraft. Follow the shared rules in EmptyPlugin's `AGENTS.md` and the team development policy.

## Repo-specific rules

- NMS/Netty code lives only in the versioned package (`v26_2/HoloHandler262.kt`); for a new Minecraft version add a new package and select the handler by version.
- Packet (Netty) callbacks run off the main thread; hop with `runTask` before invoking `onClick` (`HoloController.kt`).
- Remove the `sneakyholos_listener` Netty handler on quit and on disable.
