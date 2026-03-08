package com.sneakymouse.sneakyholos

import com.sneakymouse.sneakyholos.util.TextUtility
import org.bukkit.entity.Player

/** Represents a single holographic button. */
data class HoloButton(
        val id: String,
        var textJson: String,
        val tx: Float,
        val ty: Float,
        val tz: Float,
        val lineWidth: Int = 200,
        var bgDefault: Int = 0,
        var bgHighlight: Int = 0,
        val pitch: Float = 0f,
        val yawOffset: Float = 0f,
        val scaleX: Float = 1f,
        val scaleY: Float = 1f,
        val interactionWidth: Float? = null,
        val interactionHeight: Float? = null,
        val playerRelative: Boolean = false,
        val onClick: (Player, Boolean) -> Unit = { _, _ -> },
        val onHover: (Player, Boolean) -> Unit = { _, _ -> }
) {
    companion object {
        /** Alternative way to build using MiniMessage for text. */
        fun fromMM(
                id: String,
                textMM: String,
                tx: Float,
                ty: Float,
                tz: Float,
                lineWidth: Int = 200,
                bgDefault: Int = 0,
                bgHighlight: Int = 0,
                pitch: Float = 0f,
                yawOffset: Float = 0f,
                scaleX: Float = 1f,
                scaleY: Float = 1f,
                interactionWidth: Float? = null,
                interactionHeight: Float? = null,
                playerRelative: Boolean = false,
                onClick: (Player, Boolean) -> Unit = { _, _ -> },
                onHover: (Player, Boolean) -> Unit = { _, _ -> }
        ): HoloButton =
                HoloButton(
                        id,
                        TextUtility.mmToJson(textMM),
                        tx,
                        ty,
                        tz,
                        lineWidth,
                        bgDefault,
                        bgHighlight,
                        pitch,
                        yawOffset,
                        scaleX,
                        scaleY,
                        interactionWidth,
                        interactionHeight,
                        playerRelative,
                        onClick,
                        onHover
                )
    }
}
