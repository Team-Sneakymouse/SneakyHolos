package com.sneakymouse.sneakyholos.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

/** Utility for text formatting within the SneakyHolos library. */
object TextUtility {
    private val mm = MiniMessage.miniMessage()
    private val gsonSer = GsonComponentSerializer.gson()

    /** Converts MiniMessage string to JSON. */
    fun mmToJson(miniMsg: String): String = gsonSer.serialize(mm.deserialize(miniMsg))

    /** Converts legacy color codes to MiniMessage and then to Component. */
    fun convertToComponent(message: String): Component {
        return mm.deserialize(replaceFormatCodes(message)).decoration(TextDecoration.ITALIC, false)
    }

    /** Replaces legacy color codes with MiniMessage format. */
    fun replaceFormatCodes(message: String): String {
        return message.replace("\u00BA", "&")
                .replace("\u00A7", "&")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&0", "<black>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obf>")
                .replace("&l", "<b>")
                .replace("&m", "<st>")
                .replace("&n", "<u>")
                .replace("&o", "<i>")
                .replace("&r", "<reset>")
                .replace("&#([A-Fa-f0-9]{6})".toRegex(), "<color:#$1>")
    }

    /** Splits text into lines of a maximum length. */
    fun splitIntoLines(text: String, maxLineLength: Int): List<String> {
        val words = text.split("\\s+".toRegex())
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        var currentLength = 0

        for (word in words) {
            if (currentLength + word.length + 1 > maxLineLength && currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
                currentLength = word.length
            } else {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
                currentLength += word.length + (if (currentLine.length > word.length) 1 else 0)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }
}
