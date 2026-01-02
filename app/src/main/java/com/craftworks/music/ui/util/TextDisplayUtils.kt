package com.craftworks.music.ui.util

/**
 * Utility functions for text display formatting in the UI.
 */
object TextDisplayUtils {

    /**
     * Checks if an artist name represents an unknown/missing artist.
     * Catches variations like:
     * - "Unknown Artist"
     * - "[Unknown Artist]"
     * - "(Unknown Artist)"
     * - "Unknown"
     * - "<Unknown>"
     * - Empty/blank strings
     * - Null values
     */
    fun isUnknownArtist(name: String?): Boolean {
        if (name.isNullOrBlank()) return true

        val normalized = name.trim()
            .removeSurrounding("[", "]")
            .removeSurrounding("(", ")")
            .removeSurrounding("<", ">")
            .trim()
            .lowercase()

        return normalized.isBlank() ||
               normalized == "unknown" ||
               normalized == "unknown artist" ||
               normalized == "various artists" ||
               normalized == "?" ||
               normalized.contains("unknown artist")
    }

    /**
     * Formats an artist name for display, returning a fallback for unknown artists.
     * @param name The artist name to format
     * @param fallback The fallback string to use for unknown artists (default: single space)
     * @return The formatted artist name or fallback
     */
    fun formatArtistName(name: String?, fallback: String = " "): String {
        return if (isUnknownArtist(name)) fallback else name!!.trim()
    }

    /**
     * Track number prefix pattern.
     * Matches patterns like:
     * - "12 Scar" → "Scar"
     * - "12-Scar" → "Scar"
     * - "12 - Scar" → "Scar"
     * - "12. Scar" → "Scar"
     * - "12.Scar" → "Scar"
     * - "01 Song Name" → "Song Name"
     * - "1 - Song" → "Song"
     * - "03-Track" → "Track"
     * - "1-03 Hard Times" → "Hard Times" (disc-track format)
     * - "0-23 Dance Now" → "Dance Now" (disc-track format)
     */
    private val TRACK_NUMBER_PREFIX_REGEX = Regex(
        """^\d{1,3}([-./]\d{1,3})?\s*[-.\s]+\s*"""
    )

    /**
     * Strips track number prefix from a song title if present.
     * @param title The song title to process
     * @param stripTrackNumbers Whether to strip track numbers (setting check)
     * @return The title with track number prefix removed, or original if no match
     */
    fun stripTrackNumberPrefix(title: String?, stripTrackNumbers: Boolean = true): String {
        if (title.isNullOrBlank()) return title ?: ""
        if (!stripTrackNumbers) return title

        return TRACK_NUMBER_PREFIX_REGEX.replaceFirst(title, "").ifBlank { title }
    }

    /**
     * Formats a song title for display, optionally stripping track numbers.
     * @param title The song title
     * @param stripTrackNumbers Whether to strip track number prefixes
     * @return The formatted title
     */
    fun formatSongTitle(title: String?, stripTrackNumbers: Boolean = false): String {
        if (title.isNullOrBlank()) return title ?: ""
        return if (stripTrackNumbers) stripTrackNumberPrefix(title) else title
    }

    /**
     * Pattern to strip leading quotes, apostrophes, and punctuation for sorting.
     * This allows songs like 'Till I Collapse and "Beat Junkies Intro" to sort
     * by their first actual letter instead of the quote character.
     * Includes many Unicode variants of quotes and apostrophes.
     */
    private val LEADING_PUNCTUATION_REGEX = Regex("""^['""`''""«»„‚ʼʻˈ′ʹ‛‵´῾᾿\[\](){}<>]+\s*""")

    /**
     * Gets a sortable key from a string by:
     * - Stripping leading quotes/apostrophes and brackets
     * - Converting to lowercase for case-insensitive sorting
     * - Putting items starting with non-letters (like # or numbers) at the bottom
     * @param text The text to get a sort key for
     * @return A normalized lowercase string suitable for sorting
     */
    fun getSortKey(text: String?): String {
        if (text.isNullOrBlank()) return "\uFFFF" // Sort empty at very end
        val stripped = LEADING_PUNCTUATION_REGEX.replaceFirst(text.trim(), "").lowercase()
        if (stripped.isEmpty()) return "\uFFFF"
        // If first char is not a letter, prefix with ~ to sort after z
        return if (stripped[0].isLetter()) stripped else "~~~$stripped"
    }
}
