package com.craftworks.music.providers.navidrome

fun extractNavidromeSongName(contentDisposition: String?): String? {
    if (contentDisposition == null) return null

    val startIndex = contentDisposition.indexOf("filename=")
    if (startIndex == -1) return null

    var endIndex = contentDisposition.indexOf(";", startIndex)
    if (endIndex == -1) {
        endIndex = contentDisposition.length
    }

    var fileName = contentDisposition.substring(startIndex + 9, endIndex).trim('\"')

    // Remove any path or folder structure and keep only the base filename
    fileName = fileName.substringAfterLast('/')

    return fileName
}