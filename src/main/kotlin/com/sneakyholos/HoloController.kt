package com.sneakymouse.sneakyholos

import java.util.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

/** Main entry point for managing holographic UIs. */
class HoloController(private val plugin: JavaPlugin, val handler: HoloHandler) : Listener {
    private val activeHuds = mutableMapOf<UUID, HoloHUD>()
    private val triggers = mutableMapOf<String, HoloTrigger>()
    private val playerTriggersSeenId =
            mutableMapOf<UUID, MutableMap<String, Int>>() // Player UUID -> Trigger ID -> Entity ID
    private val playerTriggersSeen =
            mutableMapOf<UUID, MutableSet<String>>() // Player UUID -> Set of Trigger IDs
    private val processedInteractions =
            mutableMapOf<UUID, Pair<Int, MutableSet<Int>>>() // player -> (tick, entityIds)
    private val processedFallbackClicks =
            mutableMapOf<UUID, Int>() // player -> tick (prevents double fire alongside packets)

    fun start() {
        plugin.server.pluginManager.registerEvents(this, plugin)
        plugin.server.scheduler.scheduleSyncRepeatingTask(
                plugin,
                Runnable {
                    activeHuds.values.forEach { it.tick() }
                    if (plugin.server.currentTick % 20 == 0) {
                        updateTriggers()
                    }
                },
                0L,
                1L
        )

        plugin.server.onlinePlayers.forEach { inject(it) }
    }

    private fun updateTriggers() {
        for (player in plugin.server.onlinePlayers) {
            val seen = playerTriggersSeen.getOrPut(player.uniqueId) { mutableSetOf() }
            val hasHud = activeHuds.containsKey(player.uniqueId)

            for (trigger in triggers.values) {
                val distSq = player.location.distanceSquared(trigger.location)
                val inRange = distSq <= trigger.radius * trigger.radius * 4.0 // 2x radius buffer

                // Triggers now persist even when HUD is open to allow "clicking through" to the
                // mannequin
                val shouldExist = inRange

                if (shouldExist && !seen.contains(trigger.id)) {
                    val eid = handler.allocateEntityId()
                    playerTriggersSeenId.getOrPut(player.uniqueId) { mutableMapOf() }[trigger.id] =
                            eid
                    handler.spawnInteraction(
                            player,
                            eid,
                            trigger.location.x,
                            trigger.location.y,
                            trigger.location.z,
                            trigger.radius * 2f,
                            trigger.radius * 2f,
                            0f,
                            0f,
                            0f,
                            0f,
                            0f,
                            false // Added yaw/yawOffset/playerRelative if needed
                    )
                    seen.add(trigger.id)
                } else if (!shouldExist && seen.contains(trigger.id)) {
                    val eid = playerTriggersSeenId[player.uniqueId]?.remove(trigger.id)
                    if (eid != null) {
                        handler.destroyEntities(player, intArrayOf(eid))
                    }
                    seen.remove(trigger.id)
                }
            }
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        inject(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        handler.removePacketListener(event.player)
        playerTriggersSeen.remove(event.player.uniqueId)
        val hud = activeHuds.remove(event.player.uniqueId)
        if (hud != null) {
            hud.destroy()
            hud.onClose(hud.viewer)
        }
    }

    private fun inject(player: Player) {
        handler.injectPacketListener(player) { entityId, isLeftClick ->
            onPacketClick(player, entityId, isLeftClick)
        }
    }

    private fun fireHoveredHudClick(player: Player, isLeftClick: Boolean, source: String): Boolean {
        val hud = activeHuds[player.uniqueId] ?: return false
        val hoveredId = hud.hoverTargetId ?: return false

        val currentTick = plugin.server.currentTick
        val lastTick = processedFallbackClicks[player.uniqueId]
        if (lastTick == currentTick) return true // Already handled a fallback click this tick
        processedFallbackClicks[player.uniqueId] = currentTick

        val btn = hud.getButtonById(hoveredId) ?: return false
        val backwards = !isLeftClick
        if (plugin.config.getBoolean("plugin.debug", false)) {
            plugin.logger.info(
                    "[DEBUG] HoloController fallbackClick: source=$source leftClick=$isLeftClick backwards=$backwards hoveredId=$hoveredId"
            )
        }
        plugin.server.scheduler.runTask(plugin, Runnable { btn.onClick(player, backwards) })
        return true
    }

    /**
     * Fallback for when the client doesn't send a ServerboundInteractPacket for our Interaction
     * entities (commonly when extremely close in survival and the crosshair targets a block instead).
     * If a HUD button is hovered (highlighted), treat the click as a HUD click.
     */
    @EventHandler(ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val isLeftClick =
                event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK
        val isRightClick =
                event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK
        if (!isLeftClick && !isRightClick) return

        if (fireHoveredHudClick(event.player, isLeftClick = isLeftClick, source = "PlayerInteractEvent")) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerAnimation(event: PlayerAnimationEvent) {
        // Covers left-click air swings that don't fire PlayerInteractEvent.
        if (fireHoveredHudClick(event.player, isLeftClick = true, source = "PlayerAnimationEvent")) {
            event.isCancelled = true
        }
    }

    private fun onPacketClick(player: Player, entityId: Int, isLeftClick: Boolean) {
        val currentTick = plugin.server.currentTick
        val pData =
                processedInteractions.getOrPut(player.uniqueId) { currentTick to mutableSetOf() }

        if (pData.first != currentTick) {
            pData.second.clear()
            processedInteractions[player.uniqueId] = currentTick to pData.second
        }

        if (!pData.second.add(entityId)) return // Already processed this entity this tick

        // If we already handled a fallback click this tick, don't double-trigger.
        if (processedFallbackClicks[player.uniqueId] == currentTick) return

        val backwards = !isLeftClick
        if (plugin.config.getBoolean("plugin.debug", false)) {
            plugin.logger.info(
                    "[DEBUG] HoloController onPacketClick: entity=$entityId leftClick=$isLeftClick backwards=$backwards"
            )
        }

        // 1. Check HUD buttons
        val hud = activeHuds[player.uniqueId]
        if (hud != null) {
            // SINGLE SOURCE OF TRUTH: If a button is hovered (visually highlighted), it wins the
            // click.
            val hoveredId = hud.hoverTargetId
            if (hoveredId != null) {
                hud.getButtonById(hoveredId)?.let { btn ->
                    plugin.server.scheduler.runTask(
                            plugin,
                            Runnable { btn.onClick(player, backwards) }
                    )
                    return
                }
            }

            // Fallback: Check if the specific entityId belongs to a HUD button.
            val btn = hud.getButtonByInteractionId(entityId)
            if (btn != null) {
                plugin.server.scheduler.runTask(plugin, Runnable { btn.onClick(player, backwards) })
                return
            }
        }

        // 2. Check Generic Triggers
        val triggerId =
                playerTriggersSeenId[player.uniqueId]?.entries?.find { it.value == entityId }?.key
        if (triggerId != null) {
            val h = activeHuds[player.uniqueId]
            if (h != null && h.isAnyButtonHovered) {
                // Large mannequin trigger shaded the button? Redirect based on hover target.
                h.getButtonById(h.hoverTargetId!!)?.let { btn ->
                    plugin.server.scheduler.runTask(
                            plugin,
                            Runnable { btn.onClick(player, backwards) }
                    )
                    return
                }
            }

            val trigger = triggers[triggerId]
            if (trigger != null) {
                plugin.server.scheduler.runTask(
                        plugin,
                        Runnable { trigger.onTrigger(player, backwards) }
                )
            }
        }
    }

    fun openHud(hud: HoloHUD) {
        val prev = activeHuds.remove(hud.viewer.uniqueId)
        if (prev != null) {
            prev.destroy()
            prev.onClose(prev.viewer)
        }
        hud.spawn()
        activeHuds[hud.viewer.uniqueId] = hud
    }

    fun getHud(viewerId: UUID): HoloHUD? = activeHuds[viewerId]

    fun closeHud(viewerId: UUID, animate: Boolean = true) {
        val hud = activeHuds[viewerId] ?: return
        if (animate) {
            hud.flyAway()
            plugin.server.scheduler.runTaskLater(
                    plugin,
                    Runnable {
                        if (activeHuds[viewerId] === hud) {
                            hud.destroy()
                            activeHuds.remove(viewerId)
                            hud.onClose(hud.viewer)
                        }
                    },
                    11L
            )
        } else {
            hud.destroy()
            activeHuds.remove(viewerId)
            hud.onClose(hud.viewer)
        }
    }

    fun destroyHUDs(mannequinId: UUID) {
        val toRemove = activeHuds.filterValues { it.mannequinId == mannequinId }.keys.toList()
        toRemove.forEach { vid -> activeHuds.remove(vid)?.destroy() }
    }

    fun addTrigger(trigger: HoloTrigger) {
        triggers[trigger.id] = trigger
        // In a real implementation we'd also spawn virtual entities for nearby players.
        // For Phase 2, we keep this as a placeholder for generalized interaction logic.
    }

    fun removeTrigger(id: String) {
        triggers.remove(id)
    }

    fun shutdown() {
        activeHuds.values.forEach { it.destroy() }
        activeHuds.clear()
        plugin.server.onlinePlayers.forEach { handler.removePacketListener(it) }
    }
}
