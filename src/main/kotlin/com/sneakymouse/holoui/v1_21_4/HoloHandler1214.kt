package com.sneakymouse.holoui.v1_21_4

import com.sneakymouse.holoui.HoloHandler
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Display.ItemDisplay
import net.minecraft.world.entity.Display.TextDisplay
import net.minecraft.world.entity.Interaction
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.phys.Vec3
import org.bukkit.Material
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack as BukkitItemStack
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.joml.Quaternionf
import org.joml.Vector3f
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class HoloHandler1214 : HoloHandler {
    private val entityIdCounter = AtomicInteger(2_000_000)

    override fun allocateEntityId(): Int = entityIdCounter.getAndIncrement()

    override fun spawnTextDisplay(
        viewer: Player, entityId: Int,
        x: Double, y: Double, z: Double,
        textJson: String, bgColor: Int,
        tx: Float, ty: Float, tz: Float,
        yaw: Float, lineWidth: Int,
        pitch: Float,
        scaleX: Float, scaleY: Float
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val level = handle.serverLevel()
        val connection = handle.connection

        val display = buildTextDisplay(level, textJson, bgColor, tx, ty, tz, yaw, lineWidth, pitch, scaleX, scaleY)
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
        scaleX: Float, scaleY: Float
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val level = handle.serverLevel()
        val connection = handle.connection

        val display = buildTextDisplay(level, textJson, bgColor, tx, ty, tz, yaw, lineWidth, pitch, scaleX, scaleY)
        display.setTransformationInterpolationDelay(0)
        display.setTransformationInterpolationDuration(interpolationTicks)

        connection.send(ClientboundSetEntityDataPacket(entityId, display.entityData.packAll()))
    }

    override fun updateBackground(viewer: Player, entityId: Int, bgColor: Int) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val dataItems = listOf(
            net.minecraft.network.syncher.SynchedEntityData.DataValue.create(
                TextDisplay.DATA_BACKGROUND_COLOR_ID, bgColor
            )
        )
        handle.connection.send(ClientboundSetEntityDataPacket(entityId, dataItems))
    }

    override fun spawnItemDisplay(
        viewer: Player, entityId: Int,
        x: Double, y: Double, z: Double,
        item: String, customModelData: Int, displayContext: String,
        tx: Float, ty: Float, tz: Float,
        sx: Float, sy: Float, sz: Float,
        yaw: Float
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val level = handle.serverLevel()
        val connection = handle.connection

        val display = buildItemDisplay(level, item, customModelData, displayContext, tx, ty, tz, sx, sy, sz, yaw)
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
        interpolationTicks: Int
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val level = handle.serverLevel()
        val connection = handle.connection

        val display = buildItemDisplay(level, item, customModelData, displayContext, tx, ty, tz, sx, sy, sz, yaw)
        display.setTransformationInterpolationDelay(0)
        display.setTransformationInterpolationDuration(interpolationTicks)

        connection.send(ClientboundSetEntityDataPacket(entityId, display.entityData.packAll()))
    }

    override fun destroyEntities(viewer: Player, entityIds: IntArray) {
        if (entityIds.isEmpty()) return
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        handle.connection.send(ClientboundRemoveEntitiesPacket(*entityIds))
    }

    override fun spawnInteraction(
        viewer: Player, entityId: Int,
        x: Double, y: Double, z: Double,
        width: Float, height: Float,
        tx: Float, ty: Float, tz: Float,
        yaw: Float
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val level = handle.serverLevel()
        val connection = handle.connection

        val inter = Interaction(EntityType.INTERACTION, level)
        inter.width = width
        inter.height = height

        val rotatedTranslation = Vector3f(tx, ty, tz).also { Quaternionf().rotationY(yaw).transform(it) }
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
        tx: Float, ty: Float, tz: Float,
        yaw: Float
    ) {
        val handle = (viewer as CraftPlayer).handle as ServerPlayer
        val rotatedTranslation = Vector3f(tx, ty, tz).also { Quaternionf().rotationY(yaw).transform(it) }
        val wx = x + rotatedTranslation.x
        val wy = y + rotatedTranslation.y
        val wz = z + rotatedTranslation.z

        val teleportPacket = ClientboundTeleportEntityPacket(
            entityId,
            net.minecraft.world.entity.PositionMoveRotation(Vec3(wx, wy, wz), Vec3.ZERO, 0f, 0f),
            emptySet(),
            false
        )
        handle.connection.send(teleportPacket)
    }

    override fun injectPacketListener(player: Player, callback: (Int, Boolean) -> Unit) {
        val handle = (player as CraftPlayer).handle
        val pipeline = handle.connection.connection.channel.pipeline()
        if (pipeline.get("holoui_listener") != null) return

        pipeline.addBefore("packet_handler", "holoui_listener", object : ChannelDuplexHandler() {
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                if (msg is ServerboundInteractPacket) {
                    val entityId = msg.entityId
                    // In 1.21.4, dispatch is often used, but we can access action via reflection if needed
                    // For now, let's assume we can get it or just treat all as clicks
                    // Actually, let's try to get the action type.
                    val isAttack = msg.isAttack
                    callback(entityId, isAttack)
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

    private val ServerboundInteractPacket.isAttack: Boolean
        get() = ServerboundInteractPacket::class.java.getDeclaredField("action").let {
            it.isAccessible = true
            val action = it.get(this)
            action.javaClass.simpleName == "AttackAction"
        }

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
        val rotatedTranslation = Vector3f(tx, ty, tz).also { yawQ.transform(it) }

        display.setTransformation(
            com.mojang.math.Transformation(
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

        val context = try {
            ItemDisplayContext.valueOf(displayContext.uppercase())
        } catch (_: Exception) {
            ItemDisplayContext.FIXED
        }
        display.setItemTransform(context)

        val rotQ = Quaternionf().rotationY(yaw)
        val rotatedTranslation = Vector3f(tx, ty, tz).also { rotQ.transform(it) }

        display.setTransformation(
            com.mojang.math.Transformation(
                rotatedTranslation,
                rotQ,
                Vector3f(sx, sy, sz),
                Quaternionf()
            )
        )
        return display
    }
}
