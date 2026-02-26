package com.sneakymouse.holoui

import org.bukkit.entity.Player

/**
 * Represents a single holographic button.
 */
data class HoloButton(
    val id: String,
    var textJson: String,
    val tx: Float,
    val ty: Float,
    val tz: Float,
    val lineWidth: Int,
    var bgDefault: Int,
    var bgHighlight: Int,
    val pitch: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val onClick: (Player, Boolean) -> Unit = { _, _ -> }
)
