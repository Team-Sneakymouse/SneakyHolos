package com.sneakymouse.holoui

import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * A generic interaction point that can trigger a HUD or other actions.
 */
class HoloTrigger(
    val id: String,
    val location: Location,
    val radius: Float,
    val onTrigger: (Player, backwards: Boolean) -> Unit
)
