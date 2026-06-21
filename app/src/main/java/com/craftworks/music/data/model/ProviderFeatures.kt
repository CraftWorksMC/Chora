package com.craftworks.music.data.model

@JvmInline
value class ProviderFeatures(val mask: Long) {
    companion object {
        val REPORT_PLAYBACK = ProviderFeatures(1L shl 0)
        val FAVORITES = ProviderFeatures(1L shl 1)
        val DOWNLOADS = ProviderFeatures(1L shl 2)
    }

    fun has (flags: ProviderFeatures): Boolean =
        (flags.mask and mask) == flags.mask

    fun any (flags: ProviderFeatures): Boolean =
        (flags.mask and mask) != 0L

    operator fun plus(other: ProviderFeatures) = ProviderFeatures(mask or other.mask)
}