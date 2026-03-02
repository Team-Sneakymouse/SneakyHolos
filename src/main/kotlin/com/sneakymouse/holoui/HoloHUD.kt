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
    val mannequinId: UUID,
    val origin: Location,
    private val handler: HoloHandler,
    val buttons: MutableList<HoloButton>,
    val onClose: (Player) -> Unit,
    private val yawOffset: Float = 0f,
    private val frameItem: String? = null,
    private val frameCustomModelData: Int = 0
) {
    private val buttonEntityIds = mutableMapOf<String, Int>()
    private val interactionEntityIds = mutableMapOf<String, Int>()
    private var lastYaw = 0f

    private var hoverTargetId: String? = null
    private var ready = false
    private val buttonFlyInTicks = mutableMapOf<String, Int>()
    private val buttonFlyAwayTicks = mutableMapOf<String, Int>()
    // Tracks buttons that just finished animating so they get one final update
    private val wasAnimating = mutableSetOf<String>()

    private companion object {
        const val BUTTON_TOLERANCE = 0.25
        const val INTERACTION_WIDTH = 0.8f
        const val INTERACTION_HEIGHT = 0.4f
        const val HUD_FLY_INTERP_TICKS = 8
        const val HUD_FLY_OUT_TICKS = 10
        const val HUD_FLY_Z_OFFSET = 1.5f
    }

    fun spawn() {
        if (ready) return

        val eyeVec = viewer.eyeLocation.toVector()
        val originVec = origin.toVector()
        val dx = eyeVec.x - originVec.x
        val dz = eyeVec.z - originVec.z
        val yaw = atan2(dx, dz).toFloat()
        lastYaw = yaw

        buttons.forEach { btn ->
            spawnButton(btn, yaw)
        }

        ready = true
    }

    private fun spawnButton(btn: HoloButton, yaw: Float) {
        val eid = handler.allocateEntityId()
        buttonEntityIds[btn.id] = eid

        handler.spawnTextDisplay(
            viewer, eid, origin.x, origin.y, origin.z,
            btn.textJson, btn.bgDefault,
            btn.tx, btn.ty, btn.tz + HUD_FLY_Z_OFFSET,
            yaw + btn.yawOffset, btn.lineWidth,
            btn.pitch, btn.scaleX, btn.scaleY,
            btn.playerRelative
        )

        val iid = handler.allocateEntityId()
        interactionEntityIds[btn.id] = iid
        handler.spawnInteraction(
            viewer, iid, origin.x, origin.y, origin.z,
            INTERACTION_WIDTH, INTERACTION_HEIGHT,
            btn.tx, btn.ty, btn.tz + HUD_FLY_Z_OFFSET,
            yaw, btn.yawOffset,
            btn.playerRelative
        )

        buttonFlyInTicks[btn.id] = HUD_FLY_INTERP_TICKS
        wasAnimating.add(btn.id)
    }

    fun tick() {
        if (!ready) return

        val eyeVec = viewer.eyeLocation.toVector()
        val originVec = origin.toVector()
        val dx = eyeVec.x - originVec.x
        val dz = eyeVec.z - originVec.z
        val yaw = atan2(dx, dz).toFloat()

        val toRemove = mutableListOf<String>()

        buttons.forEach { btn ->
            val outTicks = buttonFlyAwayTicks[btn.id] ?: 0
            val inTicks = buttonFlyInTicks[btn.id] ?: 0
            val isSteady = outTicks == 0 && inTicks == 0

            // Skip if steady and not in transitional state and head hasn't moved
            if (isSteady && !wasAnimating.contains(btn.id) && abs(yaw - lastYaw) < 0.005f) return@forEach

            val zOff = if (outTicks > 0) {
                val progress = outTicks.toFloat() / HUD_FLY_OUT_TICKS
                HUD_FLY_Z_OFFSET * progress
            } else if (inTicks > 0) {
                val progress = 1.0f - inTicks.toFloat() / HUD_FLY_INTERP_TICKS
                HUD_FLY_Z_OFFSET * (1.0f - progress)
            } else 0f

            val eid = buttonEntityIds[btn.id] ?: return@forEach

            handler.updateTextDisplay(
                viewer, eid,
                btn.textJson, if (hoverTargetId == btn.id) btn.bgHighlight else btn.bgDefault,
                btn.tx, btn.ty, btn.tz + zOff,
                yaw + btn.yawOffset, btn.lineWidth,
                if (isSteady) 1 else 2,
                btn.pitch, btn.scaleX, btn.scaleY,
                btn.playerRelative
            )

            interactionEntityIds[btn.id]?.let { iid ->
                handler.updateInteraction(
                    viewer, iid, origin.x, origin.y, origin.z,
                    INTERACTION_WIDTH, INTERACTION_HEIGHT,
                    btn.tx, btn.ty, btn.tz + zOff,
                    yaw, btn.yawOffset,
                    btn.playerRelative
                )
            }

            if (isSteady) {
                wasAnimating.remove(btn.id)
            } else {
                wasAnimating.add(btn.id)
                if (outTicks > 0) {
                    val nt = outTicks - 1
                    buttonFlyAwayTicks[btn.id] = nt
                    if (nt == 0) toRemove.add(btn.id)
                } else if (inTicks > 0) {
                    buttonFlyInTicks[btn.id] = inTicks - 1
                }
            }
        }

        lastYaw = yaw

        if (toRemove.isNotEmpty()) {
            toRemove.forEach { id ->
                val eid = buttonEntityIds.remove(id)
                if (eid != null) handler.destroyEntities(viewer, intArrayOf(eid))
                val iid = interactionEntityIds.remove(id)
                if (iid != null) handler.destroyEntities(viewer, intArrayOf(iid))
                buttonFlyAwayTicks.remove(id)
                wasAnimating.remove(id)
                buttons.removeIf { it.id == id }
            }
        }

        // Update hover highlight
        val newHover = computeHoverTarget()
        if (newHover != hoverTargetId) {
            val oldId = hoverTargetId
            hoverTargetId = newHover

            oldId?.let { id ->
                val btn = buttons.find { it.id == id }
                val eid = buttonEntityIds[id]
                if (btn != null && eid != null) handler.updateBackground(viewer, eid, btn.bgDefault)
            }
            newHover?.let { id ->
                val btn = buttons.find { it.id == id }
                val eid = buttonEntityIds[id]
                if (btn != null && eid != null) handler.updateBackground(viewer, eid, btn.bgHighlight)
            }
        }
    }

    fun flyAway() {
        buttons.forEach { btn ->
            buttonFlyAwayTicks[btn.id] = HUD_FLY_OUT_TICKS
            buttonFlyInTicks.remove(btn.id)
        }
    }

    fun destroy() {
        if (!ready) return
        val allIds = buttonEntityIds.values.toIntArray() + interactionEntityIds.values.toIntArray()
        if (allIds.isNotEmpty()) handler.destroyEntities(viewer, allIds)
        buttonEntityIds.clear()
        interactionEntityIds.clear()
        ready = false
    }

    // ── Public API for dynamic button management ──────────────────────────────

    /** Add new buttons dynamically (e.g., opening colour / config submenu). */
    fun addButtons(newButtons: List<HoloButton>) {
        val eyeVec = viewer.eyeLocation.toVector()
        val originVec = origin.toVector()
        val dx = eyeVec.x - originVec.x
        val dz = eyeVec.z - originVec.z
        val yaw = atan2(dx, dz).toFloat()

        newButtons.forEach { btn ->
            if (buttons.none { it.id == btn.id }) {
                buttons.add(btn)
                if (ready) spawnButton(btn, yaw)
            }
        }
    }

    /** Remove buttons by id (e.g., closing colour / config submenu). */
    fun removeButtons(ids: List<String>) {
        ids.forEach { id ->
            // Animate them out
            buttonFlyAwayTicks[id] = HUD_FLY_OUT_TICKS
            buttonFlyInTicks.remove(id)
        }
    }

    /** Update the text of a button already in the HUD. */
    fun updateButtonText(id: String, textJson: String) {
        val btn = buttons.find { it.id == id } ?: return
        val eid = buttonEntityIds[id] ?: return
        btn.textJson = textJson
        val eyeVec = viewer.eyeLocation.toVector()
        val originVec = origin.toVector()
        val dx = eyeVec.x - originVec.x
        val dz = eyeVec.z - originVec.z
        val yaw = atan2(dx, dz).toFloat()
        handler.updateTextDisplay(
            viewer, eid,
            textJson, if (hoverTargetId == id) btn.bgHighlight else btn.bgDefault,
            btn.tx, btn.ty, btn.tz,
            yaw + btn.yawOffset, btn.lineWidth,
            1, btn.pitch, btn.scaleX, btn.scaleY,
            btn.playerRelative
        )
    }

    /** Update the background colour of a button already in the HUD. */
    fun updateButtonBg(id: String, bgColor: Int) {
        val btn = buttons.find { it.id == id } ?: return
        val eid = buttonEntityIds[id] ?: return
        if (hoverTargetId != id) btn.bgDefault = bgColor
        handler.updateBackground(viewer, eid, if (hoverTargetId == id) btn.bgHighlight else bgColor)
    }

    // ── Hover / ray-casting ───────────────────────────────────────────────────

    private fun computeHoverTarget(): String? {
        val eyeVec = viewer.eyeLocation.toVector()
        val lookDir = viewer.location.direction
        val originVec = origin.toVector()

        var bestDist = Double.MAX_VALUE
        var bestId: String? = null

        buttons.forEach { btn ->
            if (buttonFlyAwayTicks.getOrDefault(btn.id, 0) > 0) return@forEach

            val zOff = if (buttonFlyInTicks.getOrDefault(btn.id, 0) > 0) {
                val progress = 1.0f - buttonFlyInTicks[btn.id]!!.toFloat() / HUD_FLY_INTERP_TICKS
                HUD_FLY_Z_OFFSET * (1.0f - progress)
            } else 0f

            val worldPos = buttonWorldPos(originVec, eyeVec, btn.tx, btn.ty, btn.tz + zOff, btn.yawOffset, btn.playerRelative)
            val d = distanceFromRay(eyeVec, lookDir, worldPos)
            if (d < bestDist) {
                bestDist = d
                bestId = btn.id
            }
        }

        return if (bestDist <= BUTTON_TOLERANCE) bestId else null
    }

    private fun buttonWorldPos(
        originPos: Vector, viewerPos: Vector,
        tx: Float, ty: Float, tz: Float,
        yawOffset: Float = 0f, playerRelative: Boolean = false
    ): Vector {
        val dx = viewerPos.x - originPos.x
        val dz = viewerPos.z - originPos.z
        val horizDist = sqrt(dx * dx + dz * dz)
        if (horizDist < 0.001) return originPos.clone().add(Vector(0.0, ty.toDouble(), 0.0))
        val yaw = atan2(dx, dz).toFloat() + yawOffset
        val sinY = sin(yaw.toDouble())
        val cosY = cos(yaw.toDouble())
        val actualTz = if (playerRelative) tz + horizDist.toFloat() else tz
        return Vector(
            originPos.x + cosY * tx + sinY * actualTz,
            originPos.y + ty,
            originPos.z - sinY * tx + cosY * actualTz
        )
    }

    private fun distanceFromRay(rayOrigin: Vector, rayDir: Vector, point: Vector): Double {
        val diff = point.clone().subtract(rayOrigin)
        val t = diff.dot(rayDir)
        if (t < 0) return point.distance(rayOrigin)
        val proj = rayOrigin.clone().add(rayDir.clone().multiply(t))
        return point.distance(proj)
    }

    fun getButtonByInteractionId(eid: Int): HoloButton? {
        val id = interactionEntityIds.entries.find { it.value == eid }?.key ?: return null
        return buttons.find { it.id == id }
    }
}
