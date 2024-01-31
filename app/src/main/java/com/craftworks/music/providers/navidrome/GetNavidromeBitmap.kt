package com.craftworks.music.providers.navidrome

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.playingSong

suspend fun getNavidromeBitmap(context: Context): Bitmap {
    if (navidromeServersList.isEmpty()) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
        navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return Bitmap.createBitmap(
        1,
        1,
        Bitmap.Config.ARGB_8888
    )

    val loading = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .data("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/getCoverArt.view?&id=${playingSong.selectedSong?.navidromeID}&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora")
        .build()
    val result = (loading.execute(request) as SuccessResult).drawable
    println("GOT NAVIDROME BITMAP!!!")
    return (result as BitmapDrawable).bitmap.copy(Bitmap.Config.RGBA_F16, true)
}