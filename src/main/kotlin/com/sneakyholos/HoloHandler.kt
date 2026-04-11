package com.sneakymouse.sneakyholos

import org.bukkit.entity.Player

/** Version-independent interface for holographic UI operations. */
interface HoloHandler {
    fun allocateEntityId(): Int

    fun spawnTextDisplay(
            viewer: Player,
            entityId: Int,
            x: Double,
            y: Double,
            z: Double,
            textJson: String,
            bgColor: Int,
            tx: Float,
            ty: Float,
            tz: Float,
            yaw: Float,
            lineWidth: Int,
            textLightBlock: Int,
            textLightSky: Int,
            pitch: Float = 0f,
            yawOffset: Float = 0f,
            scaleX: Float = 1f,
            scaleY: Float = 1f,
            playerRelative: Boolean = false
    )

    fun updateTextDisplay(
            viewer: Player,
            entityId: Int,
            textJson: String,
            bgColor: Int,
            tx: Float,
            ty: Float,
            tz: Float,
            yaw: Float,
            lineWidth: Int,
            interpolationTicks: Int,
            textLightBlock: Int,
            textLightSky: Int,
            pitch: Float = 0f,
            yawOffset: Float = 0f,
            scaleX: Float = 1f,
            scaleY: Float = 1f,
            playerRelative: Boolean = false
    )

    fun updateBackground(viewer: Player, entityId: Int, bgColor: Int)

    fun spawnItemDisplay(
            viewer: Player,
            entityId: Int,
            x: Double,
            y: Double,
            z: Double,
            item: String,
            customModelData: Int,
            displayContext: String,
            tx: Float,
            ty: Float,
            tz: Float,
            sx: Float,
            sy: Float,
            sz: Float,
            yaw: Float,
            yawOffset: Float = 0f,
            playerRelative: Boolean = false
    )

    fun updateItemDisplay(
            viewer: Player,
            entityId: Int,
            item: String,
            customModelData: Int,
            displayContext: String,
            tx: Float,
            ty: Float,
            tz: Float,
            sx: Float,
            sy: Float,
            sz: Float,
            yaw: Float,
            yawOffset: Float = 0f,
            interpolationTicks: Int,
            playerRelative: Boolean = false
    )

    fun spawnInteraction(
            viewer: Player,
            entityId: Int,
            x: Double,
            y: Double,
            z: Double,
            width: Float,
            height: Float,
            tx: Float,
            ty: Float,
            tz: Float,
            yaw: Float,
            yawOffset: Float = 0f,
            playerRelative: Boolean = false
    )

    fun updateInteraction(
            viewer: Player,
            entityId: Int,
            x: Double,
            y: Double,
            z: Double,
            width: Float,
            height: Float,
            tx: Float,
            ty: Float,
            tz: Float,
            yaw: Float,
            yawOffset: Float = 0f,
            playerRelative: Boolean = false
    )

    fun destroyEntities(viewer: Player, entityIds: IntArray)

    fun injectPacketListener(
            player: Player,
            callback: (entityId: Int, isLeftClick: Boolean) -> Unit
    )
    fun removePacketListener(player: Player)
}
