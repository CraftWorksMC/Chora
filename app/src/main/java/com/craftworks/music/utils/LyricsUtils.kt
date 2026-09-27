package com.craftworks.music.utils

import com.craftworks.music.data.model.Lyric
import com.craftworks.music.data.model.LyricsLine
import com.craftworks.music.data.model.LyricsRole
import com.craftworks.music.data.model.SyncedWord

fun separateBackgroundLyrics(
    lyricsText: String,
    startMs: Int,
    endMs: Int? = null
): LyricsLine {
    val bgRegex = Regex("\\(([^()]+)\\)")
    val bgMatches = bgRegex.findAll(lyricsText).toList()

    val lines = mutableListOf<Lyric>()
    var lastIndex = 0

    bgMatches.forEach { match ->
        val mainSegment = lyricsText.substring(lastIndex, match.range.first).trim()
        if (mainSegment.isNotBlank()) {
            lines.add(Lyric(text = mainSegment, role = LyricsRole.MAIN))
        }

        val bgSegment = match.groupValues[1].trim()
        if (bgSegment.isNotBlank()) {
            lines.add(Lyric(text = bgSegment, role = LyricsRole.BG))
        }

        lastIndex = match.range.last + 1

        // Reattach punctuation right after the closing parenthesis to the previous main line
        val punctuation = Regex("^[,;:]").find(lyricsText.substring(lastIndex))?.value
        if (punctuation != null) {
            val lastMainIdx = lines.indexOfLast { it.role == LyricsRole.MAIN }
            if (lastMainIdx != -1) {
                lines[lastMainIdx] = lines[lastMainIdx].copy(
                    text = lines[lastMainIdx].text + punctuation
                )
            }
            lastIndex += punctuation.length
        }
    }

    val tailMain = lyricsText.substring(lastIndex).trim()
    if (tailMain.isNotBlank()) {
        lines.add(Lyric(text = tailMain, role = LyricsRole.MAIN))
    }

    return LyricsLine(
        startMs = startMs,
        endMs = endMs,
        lines = lines.ifEmpty { listOf(Lyric(text = lyricsText, role = LyricsRole.MAIN)) }
    )
}

fun separateBackgroundLyrics(
    wordsList: List<SyncedWord>,
    startMs: Int,
    endMs: Int?
): LyricsLine {
    val words = wordsList.mapIndexed { index, word ->
        val nextWordStart = wordsList.getOrNull(index + 1)?.startMs ?: endMs
        word.copy(endMs = word.endMs ?: nextWordStart)
    }

    val lines = mutableListOf<Lyric>()
    var currentRole: LyricsRole? = null
    var currentWords = mutableListOf<SyncedWord>()
    var inBg = false

    for (word in words) {
        val trimmedText = word.text.trim()
        val startsWithOpenParen = trimmedText.startsWith("(")

        if (startsWithOpenParen && inBg) {
            val lastWordIndex = currentWords.lastIndex
            if (lastWordIndex >= 0)
                currentWords[lastWordIndex] = currentWords[lastWordIndex].copy(
                    text = currentWords[lastWordIndex].text.trimEnd()
                )

            lines.add(
                Lyric(
                    text = currentWords.joinToString("") { it.text },
                    words = currentWords.toList(),
                    role = currentRole ?: LyricsRole.BG
                )
            )
        }

        if (startsWithOpenParen) inBg = true

        val role = if (inBg) LyricsRole.BG else LyricsRole.MAIN

        // Strip parenthesis and drop any punctuation immediately following closing parenthesis
        val clean = word.copy(
            text = word.text
                .replace(Regex("\\s+\\("), "")
                .replace(Regex("\\)[,;:]?"), "")
                .replace("(", "")
        )

        if (Regex("\\)[,;:]?$").containsMatchIn(trimmedText)) inBg = false

        if (clean.text.isBlank()) continue

        if (role != currentRole) {
            if (currentWords.isNotEmpty() && currentRole != null) {
                val lastWordIndex = currentWords.lastIndex
                if (lastWordIndex >= 0)
                    currentWords[lastWordIndex] = currentWords[lastWordIndex].copy(
                        text = currentWords[lastWordIndex].text.trimEnd()
                    )

                lines.add(
                    Lyric(
                        text = currentWords.joinToString("") { it.text },
                        words = currentWords.toList(),
                        role = currentRole
                    )
                )
            }
            currentWords = mutableListOf()
            currentRole = role
        }
        currentWords.add(clean)
    }
    if (currentWords.isNotEmpty() && currentRole != null) {
        val lastWordIndex = currentWords.lastIndex
        if (lastWordIndex >= 0)
            currentWords[lastWordIndex] = currentWords[lastWordIndex].copy(
                text = currentWords[lastWordIndex].text.trimEnd()
            )

        lines.add(
            Lyric(
                text = currentWords.joinToString("") { it.text },
                words = currentWords.toList(),
                role = currentRole
            )
        )
    }

    return LyricsLine(
        startMs = startMs,
        endMs = endMs,
        lines = lines.ifEmpty {
            listOf(Lyric(text = wordsList.joinToString("") { it.text }, words = wordsList, role = LyricsRole.MAIN))
        }
    )
}

fun splitMultipleBg(input: List<SyncedWord>): List<List<SyncedWord>> {
    val result = mutableListOf<MutableList<SyncedWord>>()

    for (raw in input) {
        var token = raw

        // A new "(" starts a new group
        if (token.text.startsWith("(")) {
            result.add(mutableListOf())
            token = token.copy(text = token.text.removePrefix("("))
        }

        // Strip closing ")"
        if (token.text.trim().endsWith(")")) {
            token = token.copy(text = token.text.trim().removeSuffix(")"))
            if (result.isEmpty()) result.add(mutableListOf())
            if (token.text.isNotEmpty()) result.last().add(token)
        } else {
            if (result.isEmpty()) result.add(mutableListOf())
            if (token.text.isNotEmpty()) result.last().add(token)
        }
    }

    return result
}

fun mmssToMilliseconds(timeStr: String?): Int? {
    if (timeStr.isNullOrBlank()) return null
    val parts = timeStr.trim().split(":")
    return when (parts.size) {
        3 -> {
            val h = parts[0].toIntOrNull() ?: 0
            val m = parts[1].toIntOrNull() ?: 0
            val s = parts[2].toDoubleOrNull() ?: 0.0
            ((h * 3600 + m * 60 + s) * 1000).toInt()
        }
        2 -> {
            val m = parts[0].toIntOrNull() ?: 0
            val s = parts[1].toDoubleOrNull() ?: 0.0
            ((m * 60 + s) * 1000).toInt()
        }
        1 -> ((parts[0].toDoubleOrNull() ?: 0.0) * 1000).toInt()
        else -> 0
    }
}

fun getTimeStamps(input: String): List<String> {
    val regex = Regex("\\[(.*?)]")
    val matches = regex.findAll(input)

    val result = mutableListOf<String>()
    for (match in matches) {
        result.add(match.groupValues[1])
    }

    return result
}