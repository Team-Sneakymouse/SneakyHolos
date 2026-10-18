package com.sneakymouse.sneakyholos.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import me.clip.placeholderapi.PlaceholderAPI
import com.sneakyutils.util.TextUtility as UtilsText

/** Utility for text formatting within the SneakyHolos library. */
object TextUtility {
    private val mm = MiniMessage.miniMessage()
    private val gsonSer = GsonComponentSerializer.gson()

    /** Converts MiniMessage string to JSON. */
    fun mmToJson(miniMsg: String): String = gsonSer.serialize(mm.deserialize(miniMsg))

    /** Parses placeholders and [playerName] if player is provided. */
    fun parsePlaceholders(text: String, player: Player?): String {
        var processedText = text
        if (player != null) {
            processedText = processedText.replace("[playerName]", player.name)
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                processedText = PlaceholderAPI.setPlaceholders(player, processedText)
            }
        }
        return processedText
    }

    /** Converts legacy color codes to MiniMessage and then to Component. */
    fun convertToComponent(message: String, player: Player? = null): Component {
        return UtilsText.convertToComponent(parsePlaceholders(message, player))
    }

    /** Wraps text in MiniMessage tags to make it clickable to copy. */
    fun clickableCopy(text: String, color: String = "&e", hover: String = "Click to copy"): String {
        return "<click:copy_to_clipboard:'$text'><hover:show_text:'$hover'>$color$text</hover></click>"
    }

    /**
     * Chat label that runs [command] when clicked (same as typing it in chat). Italic styling is
     * cleared so it matches other SneakyHolos chat lines.
     */
    fun clickableRunCommandComponent(
            label: String,
            command: String,
            labelColor: NamedTextColor = NamedTextColor.YELLOW,
            hoverText: String = "Click to run",
    ): Component =
            Component.text(label)
                    .color(labelColor)
                    .decoration(TextDecoration.ITALIC, false)
                    .clickEvent(ClickEvent.runCommand(command))
                    .hoverEvent(
                            HoverEvent.showText(
                                    Component.text(hoverText).decoration(TextDecoration.ITALIC, false)
                            )
                    )

    /** Replaces legacy color codes with MiniMessage format. */
    fun replaceFormatCodes(message: String): String = UtilsText.replaceFormatCodes(message)

    /** Splits text into lines of a maximum length. */
    fun splitIntoLines(text: String, maxLineLength: Int): List<String> =
            UtilsText.splitIntoLines(text, maxLineLength)
}
