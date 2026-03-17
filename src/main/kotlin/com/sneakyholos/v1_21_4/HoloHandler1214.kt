package com.sneakymouse.sneakyholos.v1_21_4

import com.mojang.math.Transformation
import com.sneakymouse.sneakyholos.HoloHandler
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import java.lang.reflect.Field
import java.util.*
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.*
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

class HoloHandler1214 : HoloHandler {
    private var nextEntityId = 2000000

    private val entityIdField =
            ServerboundInteractPacket::class.java.getDeclaredField("entityId").apply {
                isAccessible = true
            }
    private var actionFieldCached: Field? = null

    override fun allocateEntityId(): Int = nextEntityId++

    override fun spawnTextDisplay(
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
            pitch: Float,
            yawOffset: Float,
            scaleX: Float,
            scaleY: Float,
            playerRelative: Boolean
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val level = handle.serverLevel()
        val connection = handle.connection

        val display =
                buildTextDisplay(
                        level,
                        textJson,
                        bgColor,
                        tx,
                        ty,
                        tz,
                        yaw,
                        lineWidth,
                        pitch,
                        yawOffset,
                        scaleX,
                        scaleY
                )
        display.setPos(x, y, z)

        val spawnPacket =
                ClientboundAddEntityPacket(
                        entityId,
                        UUID.randomUUID(),
                        x,
                        y,
                        z,
                        0f,
                        0f,
                        EntityType.TEXT_DISPLAY,
                        0,
                        Vec3.ZERO,
                        0.0
                )
        connection.send(spawnPacket)
        connection.send(ClientboundSetEntityDataPacket(entityId, display.entityData.packAll()))
    }

    override fun updateTextDisplay(
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
            pitch: Float,
            yawOffset: Float,
            scaleX: Float,
            scaleY: Float,
            playerRelative: Boolean
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val connection = handle.connection

        val display =
                buildTextDisplay(
                        handle.serverLevel(),
                        textJson,
                        bgColor,
                        tx,
                        ty,
                        tz,
                        yaw,
                        lineWidth,
                        pitch,
                        yawOffset,
                        scaleX,
                        scaleY
                )
        display.setTransformationInterpolationDelay(0)
        display.setTransformationInterpolationDuration(interpolationTicks)

        connection.send(ClientboundSetEntityDataPacket(entityId, display.entityData.packAll()))
    }

    override fun updateBackground(viewer: Player, entityId: Int, bgColor: Int) {
        val connection = (viewer as CraftPlayer).handle.connection
        val data = mutableListOf<SynchedEntityData.DataValue<*>>()
        data.add(
                SynchedEntityData.DataValue(
                        TextDisplay.DATA_BACKGROUND_COLOR_ID.id,
                        EntityDataSerializers.INT,
                        bgColor
                )
        )
        connection.send(ClientboundSetEntityDataPacket(entityId, data))
    }

    override fun spawnItemDisplay(
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
            yawOffset: Float,
            playerRelative: Boolean
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val level = handle.serverLevel()
        val connection = handle.connection

        val display =
                buildItemDisplay(
                        level,
                        item,
                        customModelData,
                        displayContext,
                        tx,
                        ty,
                        tz,
                        sx,
                        sy,
                        sz,
                        yaw,
                        yawOffset
                )
        display.setPos(x, y, z)

        val spawnPacket =
                ClientboundAddEntityPacket(
                        entityId,
                        UUID.randomUUID(),
                        x,
                        y,
                        z,
                        0f,
                        0f,
                        EntityType.ITEM_DISPLAY,
                        0,
                        Vec3.ZERO,
                        0.0
                )
        connection.send(spawnPacket)
        connection.send(ClientboundSetEntityDataPacket(entityId, display.entityData.packAll()))
    }

    override fun updateItemDisplay(
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
            yawOffset: Float,
            interpolationTicks: Int,
            playerRelative: Boolean
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val level = handle.serverLevel()
        val connection = handle.connection

        val display =
                buildItemDisplay(
                        level,
                        item,
                        customModelData,
                        displayContext,
                        tx,
                        ty,
                        tz,
                        sx,
                        sy,
                        sz,
                        yaw,
                        yawOffset
                )
        display.setTransformationInterpolationDelay(0)
        display.setTransformationInterpolationDuration(interpolationTicks)

        connection.send(ClientboundSetEntityDataPacket(entityId, display.entityData.packAll()))
    }

    override fun spawnInteraction(
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
            yawOffset: Float,
            playerRelative: Boolean
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val level = handle.serverLevel()
        val connection = handle.connection

        val inter = Interaction(EntityType.INTERACTION, level)
        inter.width = width
        inter.height = height

        val rotatedTranslation =
                Vector3f(tx, ty, tz).also { Quaternionf().rotationY(yaw + yawOffset).transform(it) }
        val wx = x + rotatedTranslation.x
        val wy = y + rotatedTranslation.y
        val wz = z + rotatedTranslation.z

        val spawnPacket =
                ClientboundAddEntityPacket(
                        entityId,
                        UUID.randomUUID(),
                        wx,
                        wy,
                        wz,
                        0f,
                        0f,
                        EntityType.INTERACTION,
                        0,
                        Vec3.ZERO,
                        0.0
                )
        connection.send(spawnPacket)
        connection.send(ClientboundSetEntityDataPacket(entityId, inter.entityData.packAll()))
    }

    override fun updateInteraction(
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
            yawOffset: Float,
            playerRelative: Boolean
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val connection = handle.connection

        val rotatedTranslation =
                Vector3f(tx, ty, tz).also { Quaternionf().rotationY(yaw + yawOffset).transform(it) }
        val wx = x + rotatedTranslation.x
        val wy = y + rotatedTranslation.y
        val wz = z + rotatedTranslation.z

        connection.send(
                ClientboundTeleportEntityPacket(
                        entityId,
                        PositionMoveRotation(Vec3(wx, wy, wz), Vec3.ZERO, 0f, 0f),
                        emptySet(),
                        false
                )
        )

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

    override fun injectPacketListener(
            player: Player,
            callback: (entityId: Int, isLeftClick: Boolean) -> Unit
    ) {
        val handle = (player as CraftPlayer).handle
        val channel = handle.connection.connection.channel

        channel.pipeline()
                .addBefore(
                        "packet_handler",
                        "sneakyholos_listener",
                        object : ChannelDuplexHandler() {
                            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                                if (msg is ServerboundInteractPacket) {
                                    val entityId = msg.entityId
                                    val action = msg.actionInternal

                                    val type =
                                            try {
                                                val method =
                                                        action?.javaClass?.getDeclaredMethod(
                                                                "getType"
                                                        )
                                                                ?: action?.javaClass
                                                                        ?.getDeclaredMethod("a")
                                                method?.isAccessible = true
                                                method?.invoke(action)
                                            } catch (_: Exception) {
                                                null
                                            }

                                    val isAttack = type?.toString() == "ATTACK"

                                    if (isAttack) {
                                        callback(entityId, true)
                                    } else {
                                        val now = System.currentTimeMillis()
                                        val last = lastInteract[player.uniqueId] ?: 0L
                                        if (now - last > 150) {
                                            lastInteract[player.uniqueId] = now
                                            callback(entityId, false)
                                        }
                                    }
                                }
                                super.channelRead(ctx, msg)
                            }
                        }
                )
    }

    override fun removePacketListener(player: Player) {
        val handle = (player as CraftPlayer).handle
        val channel = handle.connection.connection.channel
        channel.eventLoop().submit {
            if (channel.pipeline().get("sneakyholos_listener") != null) {
                channel.pipeline().remove("sneakyholos_listener")
            }
        }
    }

    private val ServerboundInteractPacket.entityId: Int
        get() = entityIdField.getInt(this)

    private val ServerboundInteractPacket.actionInternal: Any?
        get() {
            actionFieldCached?.let {
                return it.get(this)
            }

            try {
                val f = ServerboundInteractPacket::class.java.getDeclaredField("action")
                f.isAccessible = true
                actionFieldCached = f
                return f.get(this)
            } catch (_: Exception) {}

            for (f in ServerboundInteractPacket::class.java.declaredFields) {
                if (f.type != Int::class.javaPrimitiveType &&
                                f.type != Boolean::class.javaPrimitiveType &&
                                f.type != UUID::class.java
                ) {
                    f.isAccessible = true
                    actionFieldCached = f
                    return f.get(this)
                }
            }
            return null
        }

    private val lastInteract = mutableMapOf<UUID, Long>()

    private fun buildTextDisplay(
            level: net.minecraft.server.level.ServerLevel,
            textJson: String,
            bgColor: Int,
            tx: Float,
            ty: Float,
            tz: Float,
            yaw: Float,
            lineWidth: Int,
            pitch: Float,
            yawOffset: Float,
            scaleX: Float,
            scaleY: Float
    ): TextDisplay {
        val display = TextDisplay(EntityType.TEXT_DISPLAY, level)
        display.setBillboardConstraints(Display.BillboardConstraints.FIXED)
        display.setShadowRadius(0f)
        display.setShadowStrength(0f)
        display.setViewRange(32f)

        val nmsText =
                try {
                    Component.Serializer.fromJson(textJson, level.registryAccess())
                } catch (_: Exception) {
                    null
                } ?: Component.literal("?")
        display.setText(nmsText)
        display.setTextOpacity((-1).toByte())
        display.entityData.set(TextDisplay.DATA_LINE_WIDTH_ID, lineWidth)
        display.entityData.set(TextDisplay.DATA_BACKGROUND_COLOR_ID, bgColor)

        val yawQ = Quaternionf().rotationY(yaw + yawOffset)
        val rotQ = if (pitch != 0f) Quaternionf(yawQ).rotateX(pitch) else yawQ
        val rotatedTranslation = Vector3f(tx, ty, tz).also { yawQ.transform(it) }

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
            yawOffset: Float
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
        display.setItemTransform(
                net.minecraft.world.item.ItemDisplayContext.valueOf(displayContext)
        )

        val yawQ = Quaternionf().rotationY(yaw + yawOffset)
        val rotatedTranslation = Vector3f(tx, ty, tz).also { yawQ.transform(it) }

        display.setTransformation(
                Transformation(rotatedTranslation, yawQ, Vector3f(sx, sy, sz), Quaternionf())
        )
        return display
    }
}
