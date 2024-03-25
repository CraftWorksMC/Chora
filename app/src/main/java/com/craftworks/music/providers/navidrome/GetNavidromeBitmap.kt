package com.craftworks.music.providers.navidrome

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.craftworks.music.SongHelper
import com.craftworks.music.data.navidromeServersList

suspend fun getNavidromeBitmap(context: Context): Bitmap {

    try {
        if (navidromeServersList.isEmpty()) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
            navidromeServersList[selectedNavidromeServerIndex.intValue].url == "" ||
            SongHelper.currentSong.imageUrl == Uri.EMPTY) return Bitmap.createBitmap(
            1,
            1,
            Bitmap.Config.ARGB_8888
        )

        println("Getting Navidrome Bitmap")

        val loading = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/getCoverArt.view?&id=${SongHelper.currentSong.navidromeID}&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&size=512&v=1.12.0&c=Chora")
            .build()
        val result = (loading.execute(request) as SuccessResult).drawable
        return (result as BitmapDrawable).bitmap.copy(Bitmap.Config.RGBA_F16, true)
    }
    catch (_: Exception){
        return Bitmap.createBitmap(
            1,
            1,
            Bitmap.Config.ARGB_8888)
    }
}
