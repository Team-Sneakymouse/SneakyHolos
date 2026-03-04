package com.sneakymouse.holoui

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import kotlin.math.*

/**
 * Manages the holographic HUD for a single player relative to a location.
 */
class HoloHUD(
    val viewer: Player,
    val mannequinId: UUID,
    val origin: Location,
    val buttons: MutableList<HoloButton>,
    val onClose: (Player) -> Unit,
    private val handler: HoloHandler,
    private val frameItem: String? = null,
    private val frameCustomModelData: Int = 0
) {
    private val buttonEntityIds = mutableMapOf<String, Int>()
    private val interactionEntityIds = mutableMapOf<String, Int>()
    private val buttonFlyInTicks = mutableMapOf<String, Int>()
    private val buttonFlyAwayTicks = mutableMapOf<String, Int>()
    private val wasAnimating = mutableSetOf<String>()
    
    private var lastYaw = 0f
    private var lastDistSq = -1.0
    var hoverTargetId: String? = null
        private set
    private var ready = false
    private var spawned = false
    
    val isAnyButtonHovered: Boolean
        get() = hoverTargetId != null

    private companion object {
        const val BUTTON_TOLERANCE = 0.25
        const val INTERACTION_WIDTH = 0.8f
        const val INTERACTION_HEIGHT = 0.4f
        const val HUD_FLY_INTERP_TICKS = 8
        const val HUD_FLY_OUT_TICKS = 10
        const val HUD_FLY_Z_OFFSET = -10.0f
    }

    fun spawn() {
        if (spawned) return
        spawned = true
        
        val eyeVec = viewer.eyeLocation.toVector()
        val originVec = origin.toVector()
        val dx = eyeVec.x - originVec.x
        val dz = eyeVec.z - originVec.z
        lastYaw = atan2(dx, dz).toFloat()

        buttons.forEach { spawnButton(it, lastYaw) }
        ready = true
    }

    private fun spawnButton(btn: HoloButton, yaw: Float, instant: Boolean = false) {
        val eid = handler.allocateEntityId()
        buttonEntityIds[btn.id] = eid

        val zOff = if (instant) 0f else HUD_FLY_Z_OFFSET
        val (finalTx, finalTz) = applyDistanceTrig(btn.tx, btn.tz + zOff, btn.yawOffset, btn.playerRelative)

        handler.spawnTextDisplay(
            viewer, eid, origin.x, origin.y, origin.z,
            btn.textJson, btn.bgDefault,
            finalTx, btn.ty, finalTz,
            yaw, btn.lineWidth,
            btn.pitch, btn.yawOffset, btn.scaleX, btn.scaleY,
            btn.playerRelative
        )
        
        val iid = handler.allocateEntityId()
        interactionEntityIds[btn.id] = iid
        handler.spawnInteraction(
            viewer, iid, origin.x, origin.y, origin.z,
            INTERACTION_WIDTH, INTERACTION_HEIGHT,
            finalTx, btn.ty, finalTz,
            yaw, btn.yawOffset,
            btn.playerRelative
        )

        if (!instant) {
            buttonFlyInTicks[btn.id] = HUD_FLY_INTERP_TICKS
            wasAnimating.add(btn.id)
        }
    }

    fun tick() {
        if (!ready) return

        val eyePrecise = viewer.eyeLocation
        val eyeVec = eyePrecise.toVector()
        val originVec = origin.toVector()
        val dx = eyeVec.x - originVec.x
        val dz = eyeVec.z - originVec.z
        val yaw = atan2(dx, dz).toFloat()

        val dy = eyeVec.y - originVec.y
        val distSq = dx * dx + dy * dy + dz * dz
        
        var distChanged = false
        if (lastDistSq >= 0 && abs(distSq - lastDistSq) > 0.01) {
            distChanged = true
        }

        val toRemove = mutableListOf<String>()

        buttons.forEach { btn ->
            val outTicks = buttonFlyAwayTicks[btn.id] ?: 0
            val inTicks = buttonFlyInTicks[btn.id] ?: 0
            val isSteady = outTicks == 0 && inTicks == 0

            // Skip if steady and not in transitional state and head hasn't moved (or distance changed for relative buttons)
            if (isSteady && !wasAnimating.contains(btn.id) && abs(yaw - lastYaw) < 0.005f && (!distChanged || !btn.playerRelative)) return@forEach

            val zOff = if (outTicks > 0) {
                val progress = outTicks.toFloat() / HUD_FLY_OUT_TICKS
                HUD_FLY_Z_OFFSET * progress
            } else if (inTicks > 0) {
                val progress = 1.0f - inTicks.toFloat() / HUD_FLY_INTERP_TICKS
                HUD_FLY_Z_OFFSET * (1.0f - progress)
            } else 0f

            val eid = buttonEntityIds[btn.id] ?: return@forEach
            val (finalTx, finalTz) = applyDistanceTrig(btn.tx, btn.tz + zOff, btn.yawOffset, btn.playerRelative)

            handler.updateTextDisplay(
                viewer, eid,
                btn.textJson, if (hoverTargetId == btn.id) btn.bgHighlight else btn.bgDefault,
                finalTx, btn.ty, finalTz,
                yaw, btn.lineWidth,
                if (isSteady) 1 else 2,
                btn.pitch, btn.yawOffset, btn.scaleX, btn.scaleY,
                btn.playerRelative
            )

            interactionEntityIds[btn.id]?.let { iid ->
                handler.updateInteraction(
                    viewer, iid, origin.x, origin.y, origin.z,
                    INTERACTION_WIDTH, INTERACTION_HEIGHT,
                    finalTx, btn.ty, finalTz,
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
        lastDistSq = distSq

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
                if (btn != null && eid != null) {
                    handler.updateBackground(viewer, eid, btn.bgDefault)
                    btn.onHover(viewer, false)
                }
            }
            newHover?.let { id ->
                val btn = buttons.find { it.id == id }
                val eid = buttonEntityIds[id]
                if (btn != null && eid != null) {
                    handler.updateBackground(viewer, eid, btn.bgHighlight)
                    btn.onHover(viewer, true)
                }
            }
        }
    }

    private fun applyDistanceTrig(tx: Float, tz: Float, yawOffset: Float, playerRelative: Boolean): Pair<Float, Float> {
        if (!playerRelative) return Pair(tx, tz)
        val dx = origin.x - viewer.location.x
        val dz = origin.z - viewer.location.z
        val horizDist = sqrt(dx * dx + dz * dz).toFloat()
        return Pair(tx - horizDist * sin(yawOffset), tz + horizDist * cos(yawOffset))
    }

    fun flyAway() {
        buttons.forEach { btn ->
            buttonFlyAwayTicks[btn.id] = HUD_FLY_OUT_TICKS
        }
    }

    fun destroy() {
        ready = false
        val allIds = (buttonEntityIds.values + interactionEntityIds.values).toIntArray()
        if (allIds.isNotEmpty()) {
            handler.destroyEntities(viewer, allIds)
        }
        buttonEntityIds.clear()
        interactionEntityIds.clear()
    }

    /** Add new buttons dynamically (e.g., opening colour / config submenu). */
    fun addButtons(newButtons: List<HoloButton>, instant: Boolean = false) {
        val newIds = newButtons.map { it.id }.toSet()
        val overlapping = buttons.filter { it.id in newIds }.map { it.id }
        if (overlapping.isNotEmpty()) {
            removeButtons(overlapping, instant = true)
        }

        newButtons.forEach { btn ->
            buttons.add(btn)
            if (spawned) {
                spawnButton(btn, lastYaw, instant)
            }
        }
    }

    /** Remove buttons by id (e.g., closing colour / config submenu). */
    fun removeButtons(ids: List<String>, instant: Boolean = false) {
        if (instant) {
            val idSet = ids.toSet()
            idSet.forEach { id ->
                val eid = buttonEntityIds.remove(id)
                if (eid != null) handler.destroyEntities(viewer, intArrayOf(eid))
                val iid = interactionEntityIds.remove(id)
                if (iid != null) handler.destroyEntities(viewer, intArrayOf(iid))
                buttonFlyAwayTicks.remove(id)
                buttonFlyInTicks.remove(id)
                wasAnimating.remove(id)
                if (hoverTargetId == id) hoverTargetId = null
            }
            buttons.removeIf { it.id in idSet }
        } else {
            ids.forEach { id ->
                buttonFlyAwayTicks[id] = HUD_FLY_OUT_TICKS
            }
        }
    }

    /** Update the text of a button already in the HUD. */
    fun updateButtonText(id: String, textJson: String) {
        val btn = buttons.find { it.id == id } ?: return
        btn.textJson = textJson
        val eid = buttonEntityIds[id] ?: return
        
        val (finalTx, finalTz) = applyDistanceTrig(btn.tx, btn.tz, btn.yawOffset, btn.playerRelative)
        handler.updateTextDisplay(
            viewer, eid,
            btn.textJson, if (hoverTargetId == id) btn.bgHighlight else btn.bgDefault,
            finalTx, btn.ty, finalTz,
            lastYaw, btn.lineWidth,
            1,
            btn.pitch, btn.yawOffset, btn.scaleX, btn.scaleY,
            btn.playerRelative
        )
    }

    /** Update the background colour of a button already in the HUD. */
    fun updateButtonBg(id: String, bgColor: Int) {
        val btn = buttons.find { it.id == id } ?: return
        btn.bgDefault = bgColor
        val eid = buttonEntityIds[id] ?: return
        if (hoverTargetId != id) {
            handler.updateBackground(viewer, eid, bgColor)
        }
    }

    private fun computeHoverTarget(): String? {
        val eyeLoc = viewer.eyeLocation
        val rayOrigin = eyeLoc.toVector()
        val rayDir = eyeLoc.direction
        
        var bestId: String? = null
        var bestDist = Double.MAX_VALUE
        
        val originVec = origin.toVector()
        val viewerVec = eyeLoc.toVector()

        for (btn in buttons) {
            val worldPos = buttonWorldPos(originVec, viewerVec, btn.tx, btn.ty, btn.tz, btn.yawOffset, btn.playerRelative)
            val dist = distanceFromRay(rayOrigin, rayDir, worldPos)
            if (dist < BUTTON_TOLERANCE && dist < bestDist) {
                bestDist = dist
                bestId = btn.id
            }
        }
        
        return bestId
    }

    fun buttonWorldPos(
        originPos: Vector, viewerPos: Vector,
        tx: Float, ty: Float, tz: Float,
        yawOffset: Float = 0f, playerRelative: Boolean = false
    ): Vector {
        val yaw = atan2(viewerPos.x - originPos.x, viewerPos.z - originPos.z).toFloat()
        val (finalTx, finalTz) = if (playerRelative) {
            val dx = originPos.x - viewerPos.x
            val dz = originPos.z - viewerPos.z
            val horizDist = sqrt(dx * dx + dz * dz).toFloat()
            Pair(tx - horizDist * sin(yawOffset), tz + horizDist * cos(yawOffset))
        } else Pair(tx, tz)

        val rotated = Vector3f(finalTx, ty, finalTz).also { 
            Quaternionf().rotationY(yaw + yawOffset).transform(it) 
        }
        return originPos.clone().add(Vector(rotated.x.toDouble(), rotated.y.toDouble(), rotated.z.toDouble()))
    }

    private fun distanceFromRay(rayOrigin: Vector, rayDir: Vector, point: Vector): Double {
        val v = point.clone().subtract(rayOrigin)
        val dot = v.dot(rayDir)
        if (dot < 0) return Double.MAX_VALUE
        val projection = rayOrigin.clone().add(rayDir.clone().multiply(dot))
        return projection.distance(point)
    }

    fun getButtonByInteractionId(eid: Int): HoloButton? {
        val id = interactionEntityIds.entries.find { it.value == eid }?.key ?: return null
        return buttons.find { it.id == id }
    }

    fun getButtonById(id: String): HoloButton? = buttons.find { it.id == id }
}
