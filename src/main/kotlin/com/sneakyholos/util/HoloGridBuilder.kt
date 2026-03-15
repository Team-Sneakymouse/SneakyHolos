package com.sneakymouse.sneakyholos.util

import com.sneakymouse.sneakyholos.HoloButton
import org.bukkit.entity.Player

/** Utility for building grids of holographic buttons. */
class HoloGridBuilder(
        val originX: Float,
        val originY: Float,
        val originZ: Float,
        var cellSpacingX: Float,
        var cellSpacingY: Float,
        val yawOffset: Float = 0f,
        val pitch: Float = 0f,
        val playerRelative: Boolean = true
) {
        private val buttons = mutableListOf<HoloButton>()

        /** Add a button at a specific grid position. */
        fun addButton(
                id: String,
                textMM: String,
                column: Int,
                row: Int,
                bgDefault: Int = 0,
                bgHighlight: Int = 0,
                lineWidth: Int = 200,
                scaleX: Float = 1f,
                scaleY: Float = 1f,
                interactionWidth: Float? = null,
                interactionHeight: Float? = null,
                onClick: (Player, Boolean) -> Unit = { _, _ -> },
                onHover: (Player, Boolean) -> Unit = { _, _ -> }
        ) {
                val dx = column * cellSpacingX
                val dy = -row * cellSpacingY
                val rotated = rotateOffset(dx, dy)

                buttons.add(
                        HoloButton.fromMM(
                                id = id,
                                textMM = textMM,
                                tx = originX + rotated.first,
                                ty = originY + rotated.second,
                                tz = originZ + rotated.third,
                                lineWidth = lineWidth,
                                bgDefault = bgDefault,
                                bgHighlight = bgHighlight,
                                pitch = pitch,
                                yawOffset = yawOffset,
                                scaleX = scaleX,
                                scaleY = scaleY,
                                interactionWidth = interactionWidth,
                                interactionHeight = interactionHeight,
                                playerRelative = playerRelative,
                                onClick = onClick,
                                onHover = onHover
                        )
                )
        }

        /** Add a button with manual offsets relative to the origin. */
        fun addButtonManual(
                id: String,
                textMM: String,
                offsetX: Float,
                offsetY: Float,
                bgDefault: Int = 0,
                bgHighlight: Int = 0,
                lineWidth: Int = 200,
                scaleX: Float = 1f,
                scaleY: Float = 1f,
                interactionWidth: Float? = null,
                interactionHeight: Float? = null,
                onClick: (Player, Boolean) -> Unit = { _, _ -> },
                onHover: (Player, Boolean) -> Unit = { _, _ -> }
        ) {
                val rotated = rotateOffset(offsetX, offsetY)

                buttons.add(
                        HoloButton.fromMM(
                                id = id,
                                textMM = textMM,
                                tx = originX + rotated.first,
                                ty = originY + rotated.second,
                                tz = originZ + rotated.third,
                                lineWidth = lineWidth,
                                bgDefault = bgDefault,
                                bgHighlight = bgHighlight,
                                pitch = pitch,
                                yawOffset = yawOffset,
                                scaleX = scaleX,
                                scaleY = scaleY,
                                interactionWidth = interactionWidth,
                                interactionHeight = interactionHeight,
                                playerRelative = playerRelative,
                                onClick = onClick,
                                onHover = onHover
                        )
                )
        }

        private fun rotateOffset(dx: Float, dy: Float): Triple<Float, Float, Float> {
                val cosP = Math.cos(pitch.toDouble()).toFloat()
                val sinP = Math.sin(pitch.toDouble()).toFloat()

                // Offset starts at (dx, dy, 0)
                // Rotate around X-axis (Pitch) only.
                // HoloHUD handles the Yaw rotation (orbiting) around the origin.
                val dy1 = dy * cosP
                val dz1 = dy * sinP

                return Triple(dx, dy1, dz1)
        }

        fun build(): List<HoloButton> = buttons
}
