package com.sneakymouse.holoui.v1_21_4

import com.sneakymouse.holoui.HoloHandler
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.entity.Display.TextDisplay
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Interaction
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.phys.Vec3
import org.bukkit.Material
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack as BukkitItemStack
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import net.minecraft.network.chat.Component
import com.mojang.math.Transformation
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import java.lang.reflect.Field

class HoloHandler1214 : HoloHandler {
    private var nextEntityId = 2000000

    override fun allocateEntityId(): Int = nextEntityId++

    override fun spawnTextDisplay(
        viewer: Player, entityId: Int,
        x: Double, y: Double, z: Double,
        textJson: String, bgColor: Int,
        tx: Float, ty: Float, tz: Float,
        yaw: Float, lineWidth: Int,
        pitch: Float,
        scaleX: Float, scaleY: Float,
        playerRelative: Boolean
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val level = handle.serverLevel()
        val connection = handle.connection

        val dx = x - viewer.location.x
        val dz = z - viewer.location.z
        val horizDist = Math.sqrt(dx * dx + dz * dz).toFloat()
        val finalTz = if (playerRelative) tz + horizDist else tz

        val display = buildTextDisplay(level, textJson, bgColor, tx, ty, finalTz, yaw, lineWidth, pitch, scaleX, scaleY)
        display.setPos(x, y, z)

        val spawnPacket = ClientboundAddEntityPacket(
            entityId,
            UUID.randomUUID(),
            x, y, z,
            0f, 0f,
            EntityType.TEXT_DISPLAY,
            0,
            Vec3.ZERO,
            0.0
        )
        connection.send(spawnPacket)
        connection.send(ClientboundSetEntityDataPacket(entityId, display.entityData.packAll()))
    }

    override fun updateTextDisplay(
        viewer: Player, entityId: Int,
        textJson: String, bgColor: Int,
        tx: Float, ty: Float, tz: Float,
        yaw: Float, lineWidth: Int,
        interpolationTicks: Int,
        pitch: Float,
        scaleX: Float, scaleY: Float,
        playerRelative: Boolean
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val connection = handle.connection

        val dx = handle.x - viewer.location.x
        val dz = handle.z - viewer.location.z
        val horizDist = Math.sqrt(dx * dx + dz * dz).toFloat()
        val finalTz = if (playerRelative) tz + horizDist else tz

        val display = buildTextDisplay(handle.serverLevel(), textJson, bgColor, tx, ty, finalTz, yaw, lineWidth, pitch, scaleX, scaleY)
        display.setTransformationInterpolationDelay(0)
        display.setTransformationInterpolationDuration(interpolationTicks)

        connection.send(ClientboundSetEntityDataPacket(entityId, display.entityData.packAll()))
    }

    override fun updateBackground(viewer: Player, entityId: Int, bgColor: Int) {
        val connection = (viewer as CraftPlayer).handle.connection
        val data = mutableListOf<SynchedEntityData.DataValue<*>>()
        data.add(SynchedEntityData.DataValue(TextDisplay.DATA_BACKGROUND_COLOR_ID.id, EntityDataSerializers.INT, bgColor))
        connection.send(ClientboundSetEntityDataPacket(entityId, data))
    }

    override fun spawnItemDisplay(
        viewer: Player, entityId: Int,
        x: Double, y: Double, z: Double,
        item: String, customModelData: Int, displayContext: String,
        tx: Float, ty: Float, tz: Float,
        sx: Float, sy: Float, sz: Float,
        yaw: Float,
        playerRelative: Boolean
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val level = handle.serverLevel()
        val connection = handle.connection

        val dx = x - viewer.location.x
        val dz = z - viewer.location.z
        val horizDist = Math.sqrt(dx * dx + dz * dz).toFloat()
        val finalTz = if (playerRelative) tz + horizDist else tz

        val display = buildItemDisplay(level, item, customModelData, displayContext, tx, ty, finalTz, sx, sy, sz, yaw)
        display.setPos(x, y, z)

        val spawnPacket = ClientboundAddEntityPacket(
            entityId,
            UUID.randomUUID(),
            x, y, z,
            0f, 0f,
            EntityType.ITEM_DISPLAY,
            0,
            Vec3.ZERO,
            0.0
        )
        connection.send(spawnPacket)
        connection.send(ClientboundSetEntityDataPacket(entityId, display.entityData.packAll()))
    }

    override fun updateItemDisplay(
        viewer: Player, entityId: Int,
        item: String, customModelData: Int, displayContext: String,
        tx: Float, ty: Float, tz: Float,
        sx: Float, sy: Float, sz: Float,
        yaw: Float,
        interpolationTicks: Int,
        playerRelative: Boolean
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val level = handle.serverLevel()
        val connection = handle.connection

        val dx = handle.x - viewer.location.x
        val dz = handle.z - viewer.location.z
        val horizDist = Math.sqrt(dx * dx + dz * dz).toFloat()
        val finalTz = if (playerRelative) tz + horizDist else tz

        val display = buildItemDisplay(level, item, customModelData, displayContext, tx, ty, finalTz, sx, sy, sz, yaw)
        display.setTransformationInterpolationDelay(0)
        display.setTransformationInterpolationDuration(interpolationTicks)

        connection.send(ClientboundSetEntityDataPacket(entityId, display.entityData.packAll()))
    }

    override fun spawnInteraction(
        viewer: Player, entityId: Int,
        x: Double, y: Double, z: Double,
        width: Float, height: Float,
        tx: Float, ty: Float, tz: Float,
        yaw: Float, yawOffset: Float,
        playerRelative: Boolean
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val level = handle.serverLevel()
        val connection = handle.connection

        val inter = Interaction(EntityType.INTERACTION, level)
        inter.width = width
        inter.height = height
        
        val dx = x - viewer.location.x
        val dz = z - viewer.location.z
        val horizDist = Math.sqrt(dx * dx + dz * dz).toFloat()
        val finalTz = if (playerRelative) tz + horizDist else tz
        val actualTz = -finalTz

        val rotatedTranslation = Vector3f(tx, ty, actualTz).also { Quaternionf().rotationY(yaw + yawOffset).transform(it) }
        val wx = x + rotatedTranslation.x
        val wy = y + rotatedTranslation.y
        val wz = z + rotatedTranslation.z

        val spawnPacket = ClientboundAddEntityPacket(
            entityId,
            UUID.randomUUID(),
            wx, wy, wz,
            0f, 0f,
            EntityType.INTERACTION,
            0,
            Vec3.ZERO,
            0.0
        )
        connection.send(spawnPacket)
        connection.send(ClientboundSetEntityDataPacket(entityId, inter.entityData.packAll()))
    }

    override fun updateInteraction(
        viewer: Player, entityId: Int,
        x: Double, y: Double, z: Double,
        width: Float, height: Float,
        tx: Float, ty: Float, tz: Float,
        yaw: Float, yawOffset: Float,
        playerRelative: Boolean
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val connection = handle.connection
        
        val dx = x - viewer.location.x
        val dz = z - viewer.location.z
        val horizDist = Math.sqrt(dx * dx + dz * dz).toFloat()
        val finalTz = if (playerRelative) tz + horizDist else tz
        val actualTz = -finalTz

        val rotatedTranslation = Vector3f(tx, ty, actualTz).also { Quaternionf().rotationY(yaw + yawOffset).transform(it) }
        val wx = x + rotatedTranslation.x
        val wy = y + rotatedTranslation.y
        val wz = z + rotatedTranslation.z

        connection.send(ClientboundTeleportEntityPacket(entityId, PositionMoveRotation(Vec3(wx, wy, wz), Vec3.ZERO, 0f, 0f), emptySet(), false))
        
        val data = mutableListOf<SynchedEntityData.DataValue<*>>()
        // Interaction width/height IDs are 8 and 9 usually.
        data.add(SynchedEntityData.DataValue(8, EntityDataSerializers.FLOAT, width))
        data.add(SynchedEntityData.DataValue(9, EntityDataSerializers.FLOAT, height))
        connection.send(ClientboundSetEntityDataPacket(entityId, data))
    }

    override fun destroyEntities(viewer: Player, entityIds: IntArray) {
        val connection = (viewer as CraftPlayer).handle.connection
        connection.send(ClientboundRemoveEntitiesPacket(*entityIds))
    }

    override fun injectPacketListener(player: Player, callback: (entityId: Int, isLeftClick: Boolean) -> Unit) {
        val handle = (player as CraftPlayer).handle
        val channel = handle.connection.connection.channel
        
        channel.pipeline().addBefore("packet_handler", "holoui_listener", object : ChannelDuplexHandler() {
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                if (msg is ServerboundInteractPacket) {
                    val entityId = msg.entityId
                    val type = msg.actionType
                    
                    when (type) {
                        "ATTACK" -> callback(entityId, true)
                        "INTERACT", "INTERACT_AT" -> {
                            val now = System.currentTimeMillis()
                            val last = lastInteract[player.uniqueId] ?: 0L
                            if (now - last > 150) {
                                lastInteract[player.uniqueId] = now
                                callback(entityId, false)
                            }
                        }
                    }
                }
                super.channelRead(ctx, msg)
            }
        })
    }

    override fun removePacketListener(player: Player) {
        val handle = (player as CraftPlayer).handle
        val channel = handle.connection.connection.channel
        channel.eventLoop().submit {
            if (channel.pipeline().get("holoui_listener") != null) {
                channel.pipeline().remove("holoui_listener")
            }
        }
    }

    private val ServerboundInteractPacket.entityId: Int
        get() = ServerboundInteractPacket::class.java.getDeclaredField("entityId").let {
            it.isAccessible = true
            it.getInt(this)
        }

    private val ServerboundInteractPacket.actionType: String
        get() = try {
            val actionField = ServerboundInteractPacket::class.java.getDeclaredField("action")
            actionField.isAccessible = true
            val action = actionField.get(this)
            
            val type = try {
                val getTypeMethod = action.javaClass.getMethod("getType")
                getTypeMethod.invoke(action).toString()
            } catch (e: Exception) {
                val className = action.javaClass.name
                when {
                    className.contains("Attack") -> "ATTACK"
                    className.contains("AtAction") -> "INTERACT_AT"
                    className.contains("Interact") -> "INTERACT"
                    else -> "UNKNOWN"
                }
            }
            type
        } catch (e: Exception) {
            "UNKNOWN"
        }

    private val lastInteract = mutableMapOf<UUID, Long>()

    private fun buildTextDisplay(
        level: net.minecraft.server.level.ServerLevel,
        textJson: String, bgColor: Int,
        tx: Float, ty: Float, tz: Float,
        yaw: Float, lineWidth: Int,
        pitch: Float,
        scaleX: Float, scaleY: Float
    ): TextDisplay {
        val display = TextDisplay(EntityType.TEXT_DISPLAY, level)
        display.setBillboardConstraints(Display.BillboardConstraints.FIXED)
        display.setShadowRadius(0f)
        display.setShadowStrength(0f)
        display.setViewRange(32f)

        val nmsText = try {
            Component.Serializer.fromJson(textJson, level.registryAccess())
        } catch (_: Exception) { null } ?: Component.literal("?")
        display.setText(nmsText)
        display.setTextOpacity((-1).toByte())
        display.entityData.set(TextDisplay.DATA_LINE_WIDTH_ID, lineWidth)
        display.entityData.set(TextDisplay.DATA_BACKGROUND_COLOR_ID, bgColor)

        val yawQ = Quaternionf().rotationY(yaw)
        val rotQ = if (pitch != 0f) Quaternionf(yawQ).rotateX(pitch) else yawQ
        val actualTz = tz
        val rotatedTranslation = Vector3f(tx, ty, actualTz).also { yawQ.transform(it) }

        display.setTransformation(
            Transformation(
                rotatedTranslation,
                rotQ,
                Vector3f(scaleX, scaleY, 1f),
                Quaternionf()
            )
        )
        return display
    }

    private fun buildItemDisplay(
        level: net.minecraft.server.level.ServerLevel,
        item: String, customModelData: Int, displayContext: String,
        tx: Float, ty: Float, tz: Float,
        sx: Float, sy: Float, sz: Float,
        yaw: Float
    ): ItemDisplay {
        val display = ItemDisplay(EntityType.ITEM_DISPLAY, level)
        display.setBillboardConstraints(Display.BillboardConstraints.FIXED)
        display.setShadowRadius(0f)
        display.setShadowStrength(0f)
        display.setViewRange(32f)

        val material = Material.matchMaterial(item) ?: Material.GLASS_PANE
        val bukkitStack = BukkitItemStack(material)
        if (customModelData > 0) {
            val meta = bukkitStack.itemMeta
            meta?.setCustomModelData(customModelData)
            bukkitStack.itemMeta = meta
        }
        display.setItemStack(CraftItemStack.asNMSCopy(bukkitStack))

        val yawQ = Quaternionf().rotationY(yaw)
        val actualTz = tz
        val rotatedTranslation = Vector3f(tx, ty, actualTz).also { yawQ.transform(it) }

        display.setTransformation(
            Transformation(
                rotatedTranslation,
                yawQ,
                Vector3f(sx, sy, sz),
                Quaternionf()
            )
        )
        return display
    }
}
