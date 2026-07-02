package com.craftworks.music.migrations

import android.content.Context

fun interface Migration {
    fun up(context: Context)
}