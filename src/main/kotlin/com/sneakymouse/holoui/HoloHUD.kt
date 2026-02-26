package com.sneakymouse.holoui

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.*

/**
 * Manages the holographic HUD for a single player relative to a location.
 */
class HoloHUD(
    val viewer: Player,
    val origin: Location,
    val mannequinId: UUID,
    val handler: HoloHandler,
    val buttons: MutableList<HoloButton>,
    val frameItem: String? = null,
    val frameCustomModelData: Int = 0,
    val frameTranslation: Vector? = null,
    val frameScale: Vector? = null
) {
    private val buttonEntityIds = mutableMapOf<String, Int>()
    private val interactionEntityIds = mutableMapOf<String, Int>()
    private var frameEntityId: Int? = null
    private var lastYaw: Float = 0f
    private var lastDist: Float = 0f
    private var hoverTargetId: String? = null
    private var ready = false
    private var flyInTicksLeft = 10
    private var flyingAway = false

    companion object {
        private const val HUD_FLY_Z_OFFSET = -10.0f
        private const val HUD_FLY_INTERP_TICKS = 10
        private const val BUTTON_TOLERANCE = 0.35
    }

    fun spawn() {
        val dx = viewer.location.x - origin.x
        val dz = viewer.location.z - origin.z
        lastYaw = atan2(dx, dz).toFloat()
        lastDist = sqrt((dx * dx + dz * dz).toFloat())

        for (btn in buttons) {
            val eid = handler.allocateEntityId()
            buttonEntityIds[btn.id] = eid
            handler.spawnTextDisplay(
                viewer, eid,
                origin.x, origin.y, origin.z,
                btn.textJson, btn.bgDefault,
                btn.tx, btn.ty, btn.tz + HUD_FLY_Z_OFFSET,
                lastYaw, btn.lineWidth, btn.pitch, btn.scaleX, btn.scaleY
            )
            val interId = handler.allocateEntityId()
            interactionEntityIds[btn.id] = interId
            handler.spawnInteraction(
                viewer, interId,
                origin.x, origin.y, origin.z,
                btn.scaleX * 0.8f, btn.scaleY * 0.4f,
                btn.tx, btn.ty, btn.tz + HUD_FLY_Z_OFFSET,
                lastYaw
            )
        }

        if (frameItem != null) {
            val eid = handler.allocateEntityId()
            frameEntityId = eid
            val ft = frameTranslation ?: Vector(0.0, 1.7, -2.0)
            val fs = frameScale ?: Vector(3.0, 3.0, 0.05)
            handler.spawnItemDisplay(
                viewer, eid,
                origin.x, origin.y, origin.z,
                frameItem, frameCustomModelData, "FIXED",
                ft.x.toFloat(), ft.y.toFloat(), ft.z.toFloat() + HUD_FLY_Z_OFFSET,
                fs.x.toFloat(), fs.y.toFloat(), fs.z.toFloat(),
                lastYaw
            )
        }
    }

    fun tick() {
        if (flyingAway) return

        val dx = viewer.location.x - origin.x
        val dz = viewer.location.z - origin.z
        val yaw = atan2(dx, dz).toFloat()
        val dist = sqrt((dx * dx + dz * dz).toFloat())

        if (flyInTicksLeft > 0) {
            flyInTicksLeft--
            val progress = 1.0f - flyInTicksLeft.toFloat() / HUD_FLY_INTERP_TICKS
            val zOff = HUD_FLY_Z_OFFSET * (1.0f - progress)

            for (btn in buttons) {
                val eid = buttonEntityIds[btn.id] ?: continue
                handler.updateTextDisplay(
                    viewer, eid,
                    btn.textJson, if (hoverTargetId == btn.id) btn.bgHighlight else btn.bgDefault,
                    btn.tx, btn.ty, btn.tz + zOff,
                    yaw, btn.lineWidth, 2, btn.pitch, btn.scaleX, btn.scaleY
                )
                interactionEntityIds[btn.id]?.let { iid ->
                    handler.updateInteraction(viewer, iid, origin.x, origin.y, origin.z, btn.tx, btn.ty, btn.tz + zOff, yaw)
                }
            }
            updateFrame(yaw, zOff, 2)
            lastYaw = yaw
            lastDist = dist
            if (flyInTicksLeft == 0) ready = true
            return
        }

        val yawChanged = abs(yaw - lastYaw) > 0.02f
        if (yawChanged) {
            for (btn in buttons) {
                val eid = buttonEntityIds[btn.id] ?: continue
                handler.updateTextDisplay(
                    viewer, eid,
                    btn.textJson, if (hoverTargetId == btn.id) btn.bgHighlight else btn.bgDefault,
                    btn.tx, btn.ty, btn.tz,
                    yaw, btn.lineWidth, 3, btn.pitch, btn.scaleX, btn.scaleY
                )
                interactionEntityIds[btn.id]?.let { iid ->
                    handler.updateInteraction(viewer, iid, origin.x, origin.y, origin.z, btn.tx, btn.ty, btn.tz, yaw)
                }
            }
            updateFrame(yaw, 0f, 3)
            lastYaw = yaw
        }

        // Hover detection
        val hovered = computeHoverTarget()
        if (hovered != hoverTargetId) {
            if (hoverTargetId != null) {
                val oldBtn = buttons.find { it.id == hoverTargetId }
                if (oldBtn != null) {
                    val eid = buttonEntityIds[oldBtn.id]
                    if (eid != null) handler.updateBackground(viewer, eid, oldBtn.bgDefault)
                }
            }
            if (hovered != null) {
                val newBtn = buttons.find { it.id == hovered }
                if (newBtn != null) {
                    val eid = buttonEntityIds[newBtn.id]
                    if (eid != null) handler.updateBackground(viewer, eid, newBtn.bgHighlight)
                }
            }
            hoverTargetId = hovered
        }
        lastDist = dist
    }

    private fun updateFrame(yaw: Float, zOff: Float, interp: Int) {
        val eid = frameEntityId ?: return
        val ft = frameTranslation ?: Vector(0.0, 1.7, -2.0)
        val fs = frameScale ?: Vector(3.0, 3.0, 0.05)
        handler.updateItemDisplay(
            viewer, eid,
            frameItem ?: "minecraft:glass_pane", frameCustomModelData, "FIXED",
            ft.x.toFloat(), ft.y.toFloat(), ft.z.toFloat() + zOff,
            fs.x.toFloat(), fs.y.toFloat(), fs.z.toFloat(),
            yaw, interp
        )
    }

    private fun computeHoverTarget(): String? {
        val eyeLoc = viewer.eyeLocation
        val lookDir = eyeLoc.direction.normalize()
        val eyeVec = eyeLoc.toVector()
        val originVec = origin.toVector()

        var bestId: String? = null
        var bestDist = Double.MAX_VALUE

        for (btn in buttons) {
            val worldPos = buttonWorldPos(originVec, eyeVec, btn.tx, btn.ty, btn.tz)
            val d = distanceFromRay(eyeVec, lookDir, worldPos)
            if (d < bestDist) {
                bestDist = d
                bestId = btn.id
            }
        }

        return if (bestDist <= BUTTON_TOLERANCE) bestId else null
    }

    private fun buttonWorldPos(originPos: Vector, viewerPos: Vector, tx: Float, ty: Float, tz: Float): Vector {
        val dx = viewerPos.x - originPos.x
        val dz = viewerPos.z - originPos.z
        val horizDist = sqrt(dx * dx + dz * dz)
        if (horizDist < 0.001) return originPos.clone().add(Vector(0.0, ty.toDouble(), 0.0))
        val yaw = atan2(dx, dz).toFloat()
        val sinY = sin(yaw.toDouble())
        val cosY = cos(yaw.toDouble())
        return Vector(
            originPos.x + cosY * tx + sinY * tz,
            originPos.y + ty,
            originPos.z - sinY * tx + cosY * tz
        )
    }

    private fun distanceFromRay(rayOrigin: Vector, rayDir: Vector, point: Vector): Double {
        val diff = point.clone().subtract(rayOrigin)
        val t = diff.dot(rayDir)
        if (t < 0) return Double.MAX_VALUE
        val closest = rayOrigin.clone().add(rayDir.clone().multiply(t))
        return closest.distance(point)
    }

    fun handleInteract(backwards: Boolean) {
        if (!ready || flyingAway) return
        val hovered = hoverTargetId ?: return
        val btn = buttons.find { it.id == hovered } ?: return
        btn.onClick(viewer, backwards)
    }

    fun destroy() {
        val ids = buttonEntityIds.values.toIntArray() + 
                 interactionEntityIds.values.toIntArray() +
                 (frameEntityId?.let { intArrayOf(it) } ?: intArrayOf())
        handler.destroyEntities(viewer, ids)
    }

    fun getButtonByInteractionId(entityId: Int): HoloButton? {
        val btnId = interactionEntityIds.entries.find { it.value == entityId }?.key ?: return null
        return buttons.find { it.id == btnId }
    }

    fun flyAway() {
        if (flyingAway) return
        flyingAway = true
        for (btn in buttons) {
            val eid = buttonEntityIds[btn.id] ?: continue
            handler.updateTextDisplay(
                viewer, eid,
                btn.textJson, btn.bgDefault,
                btn.tx, btn.ty, btn.tz + HUD_FLY_Z_OFFSET,
                lastYaw, btn.lineWidth, HUD_FLY_INTERP_TICKS, btn.pitch, btn.scaleX, btn.scaleY
            )
        }
        updateFrame(lastYaw, HUD_FLY_Z_OFFSET, HUD_FLY_INTERP_TICKS)
    }

    fun updateButtonText(buttonId: String, newTextJson: String) {
        val btn = buttons.find { it.id == buttonId } ?: return
        btn.textJson = newTextJson
        val eid = buttonEntityIds[buttonId] ?: return
        if (ready && !flyingAway) {
            handler.updateTextDisplay(
                viewer, eid,
                newTextJson, if (hoverTargetId == buttonId) btn.bgHighlight else btn.bgDefault,
                btn.tx, btn.ty, btn.tz,
                lastYaw, btn.lineWidth, 0, btn.pitch, btn.scaleX, btn.scaleY
            )
        }
    }

    fun updateButtonBg(id: String, bgColor: Int) {
        val eid = buttonEntityIds[id] ?: return
        handler.updateBackground(viewer, eid, bgColor)
        buttons.find { it.id == id }?.let { it.bgDefault = bgColor }
    }

    fun addButtons(newButtons: List<HoloButton>) {
        val addedButtons = mutableListOf<HoloButton>()
        for (btn in newButtons) {
            if (btn.id in buttonEntityIds) continue
            val eid = handler.allocateEntityId()
            buttonEntityIds[btn.id] = eid
            handler.spawnTextDisplay(
                viewer, eid,
                origin.x, origin.y, origin.z,
                btn.textJson, btn.bgDefault,
                btn.tx, btn.ty, btn.tz + (if (ready) 0f else HUD_FLY_Z_OFFSET),
                lastYaw, btn.lineWidth, btn.pitch, btn.scaleX, btn.scaleY
            )
            val interId = handler.allocateEntityId()
            interactionEntityIds[btn.id] = interId
            handler.spawnInteraction(
                viewer, interId,
                origin.x, origin.y, origin.z,
                btn.scaleX * 0.8f, btn.scaleY * 0.4f,
                btn.tx, btn.ty, btn.tz + (if (ready) 0f else HUD_FLY_Z_OFFSET),
                lastYaw
            )
            addedButtons.add(btn)
        }
        buttons.addAll(addedButtons)
    }

    fun removeButtons(ids: List<String>) {
        val toDestroy = mutableListOf<Int>()
        for (id in ids) {
            buttonEntityIds.remove(id)?.let { toDestroy.add(it) }
            interactionEntityIds.remove(id)?.let { toDestroy.add(it) }
            buttons.removeIf { it.id == id }
        }
        if (toDestroy.isNotEmpty()) {
            handler.destroyEntities(viewer, toDestroy.toIntArray())
        }
    }
}
