package com.craftworks.music.data.providers.media.subsonic

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastJoinToString
import com.craftworks.music.data.model.AlbumArtistInfo
import com.craftworks.music.data.model.GainInfo
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.data.model.LyricSource
import com.craftworks.music.data.model.Lyrics
import com.craftworks.music.data.model.LyricsLine
import com.craftworks.music.data.model.LyricsRole
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.ProviderType
import com.craftworks.music.data.model.SyncType
import com.craftworks.music.data.model.SyncedWord
import com.craftworks.music.utils.separateBackgroundLyrics
import com.craftworks.music.utils.splitMultipleBg
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale.getDefault

@Serializable
data class SubsonicProviderData(
    var url: String,
    var username: String,
    var password: String,
    var allowSelfSignedCert: Boolean = false,
)

@Serializable
data class SubsonicResponse(
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicBody
)

@Serializable
data class SubsonicBody(
    val status: String,
    val type: String,
    val version: String,
    val openSubsonic: Boolean? = false,
    val error: SubsonicError? = null,

    val user: SubsonicUser? = null,
    val musicFolders: SubsonicMusicFolders? = null,

    val searchResult3: SearchResult3? = null,
    val starred: Starred? = null,

    // Songs
    val song: SubsonicSong? = null,
    val songsByGenre: SubsonicSongList? = null,

    // Albums
    val albumList: SubsonicAlbumList? = null,
    val albumList2: SubsonicAlbumList? = null,
    val album: SubsonicAlbum? = null,

    // Artists
    val artists: SubsonicArtistIndexList? = null,
    val artist: SubsonicArtist? = null,
    val artistInfo: SubsonicArtistInfo? = null,

    // Internet radio station
    val internetRadioStations: SubsonicInternetRadioStationList? = null,

    // Playlist
    val playlist: SubsonicPlaylist? = null,
    val playlists: SubsonicPlaylistList? = null,

    // Lyrics
    val lyricsList: SubsonicLyricsList? = null,
)

@Serializable
data class SubsonicMusicFolders(
    val musicFolder: List<SubsonicMusicFolder>
)

@Serializable
data class SubsonicMusicFolder(
    val id: Int,
    val name: String
)

@Serializable
data class SearchResult3(
    val artist: List<SubsonicArtist>? = null,
    val album: List<SubsonicAlbum>? = null,
    val song: List<SubsonicSong>? = null
)
@Serializable
data class Starred(
    val artist: List<SubsonicArtist>? = null,
    val album: List<SubsonicAlbum>? = null,
    val song: List<SubsonicSong>? = null
)

@Serializable
data class SubsonicSongList(
    val song: List<SubsonicSong>? = null
)
@Serializable
data class SubsonicSong(
    val id: String,
    val isDir: Boolean? = false,
    val title: String,
    val parent: String? = null,
    val album: String? = null,
    val artist: String = "",
    val artists: List<SubsonicArtist> = listOf(),
    val track: Int? = 0,
    val year: Int? = 0,
    val genre: String? = "",
    @SerialName("coverArt")
    var imageUrl: String? = "",
    val size: Int? = 0,
    val contentType: String? = "audio/flac",
    @SerialName("suffix")
    val format: String? = null,
    val duration: Int = 0,
    @SerialName("bitRate")
    val bitrate: Int? = 0,
    val bitDepth: Int? = 0,
    val path: String? = null,
    @SerialName("playCount")
    var timesPlayed: Int? = 0,
    val discNumber: Int? = 1,
    @SerialName("created")
    val dateAdded: String? = null,
    val albumId: String = "",
    val artistId: String? = "",
    val type: String? = "music",
    val isVideo: Boolean? = false,
    @SerialName("played")
    val lastPlayed: String? = "",
    val bpm: Int? = 0,
    val comment: String? = "",
    val sortName: String? = "",
    val mediaType: String? = "song",
    val musicBrainzId: String? = "",
    val isrc: List<String>? = null,
    val genres: List<SubsonicItemGenre>? = listOf(),
    val replayGain: SubsonicReplayGain? = null,
    val channelCount: Int? = 2,
    val samplingRate: Int? = 0,
    val explicitStatus: String? = null,
    val displayArtist: String? = null,
    val displayAlbumArtist: String? = null,

    val isRadio: Boolean? = false,
    var media: String? = null,
    val trackIndex: Int? = 0,
    var starred: String? = null,
    val userRating: Int? = null,
) {
    fun toMediaModel(providerId: String): MediaModel.Song = MediaModel.Song(
        id = id,
        providerId = providerId,
        providerType = ProviderType.SUBSONIC,
        album = this.album,
        albumArtistName = this.artist,
        albumArtists = this.artists.map { it.toMediaModel(providerId) },
        albumId = this.albumId,
        artistName = this.artist,
        artists = this.artists.map { it.toMediaModel(providerId) },
        bitDepth = this.bitDepth,
        bitRate = this.bitrate,
        format = format?.uppercase(getDefault()),
        bpm = this.bpm,
        channels = this.channelCount,
        comment = this.comment,
        compilation = null,
        container = this.contentType,
        createdAt = this.dateAdded,
        discNumber = this.discNumber ?: 1,
        discSubtitle = null,
        durationMs = this.duration * 1000,
        explicit = this.explicitStatus?.let { it == "explicit"},
        gain = GainInfo(replayGain?.albumPeak, replayGain?.trackGain),
        genres = this.genres?.map { MediaModel.Genre(name = it.name) } ?: listOf(),
        imageId = this.imageUrl,
        imageUrl = this.imageUrl,
        lastPlayedAt = this.lastPlayed,
        lyrics = null,
        mbzRecordingId = this.musicBrainzId,
        mbzTrackId = null,
        isrc = this.isrc,
        name = this.title,
        participants = null,
        path = this.path,
        peak = null,
        playCount = this.timesPlayed,
        playlistItemId = null,
        releaseDate = null,
        releaseYear = this.year,
        sampleRate = this.samplingRate,
        size = this.size,
        sortName = this.sortName,
        tags = null,
        trackNumber = this.track ?: 0,
        trackSubtitle = null,
        updatedAt = null,
        userFavorite = !this.starred.isNullOrEmpty(),
        userRating = this.userRating
    )
}

@Serializable
data class SubsonicAlbumList(
    val album: List<SubsonicAlbum>? = null
)

@Serializable
data class SubsonicAlbum(
    val id: String,
    val name: String,
    val version: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int,
    val duration: Int,
    val playCount: Long? = null,
    val created: String,
    val starred: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val played: String? = null,
    val userRating: Int? = null,
    val recordLabels: List<SubsonicRecordLabel>? = null,
    val musicBrainzId: String? = null,
    val genres: List<SubsonicItemGenre>? = null,
    val artists: List<SubsonicArtist>? = null,
    val displayArtist: String? = null,
    val releaseTypes: List<String>? = null,
    val moods: List<String>? = null,
    val sortName: String? = null,
    val originalReleaseDate: SubsonicItemDate? = null,
    val releaseDate: SubsonicItemDate? = null,
    val isCompilation: Boolean? = null,
    val explicitStatus: String? = null,
    val discTitles: List<SubsonicDiscTitle>? = null,
    val song: List<SubsonicSong>? = null
) {
    fun toMediaModel(providerId: String): MediaModel.Album {
        return MediaModel.Album(
            id = id,
            providerId = providerId,
            providerType = ProviderType.SUBSONIC,
            albumArtistName = displayArtist,
            artists = this.artists?.map { it.toMediaModel(providerId) } ?: emptyList(),
            comment = null,
            createdAt = this.created,
            durationMs = this.duration * 1000,
            explicit = this.explicitStatus?.let { it == "explicit"},
            genres = this.genres?.map { MediaModel.Genre(name = it.name) } ?: listOf(),
            imageId = this.coverArt,
            imageUrl = this.coverArt,
            isCompilation = this.isCompilation,
            lastPlayedAt = this.played,
            mbzId = this.musicBrainzId,
            mbzReleaseGroupId = null,
            name = this.name,
            originalDate = this.originalReleaseDate?.year?.toString(),
            originalYear = this.originalReleaseDate?.year ?: this.year,
            participants = mapOf(),
            playCount = this.playCount?.toDouble(),
            recordLabels = this.recordLabels?.map { it.name } ?: listOf(),
            releaseDate = this.releaseDate?.year?.toString(),
            releaseType = this.releaseTypes?.firstOrNull(),
            releaseTypes = this.releaseTypes ?: listOf(),
            releaseYear = this.year,
            size = null,
            songCount = this.songCount,
            songs = this.song?.map { it.toMediaModel(providerId) } ?: emptyList(),
            sortName = this.sortName,
            tags = if (!this.moods.isNullOrEmpty()) mapOf("moods" to this.moods) else mapOf(),
            updatedAt = null,
            userFavorite = !this.starred.isNullOrEmpty(),
            userRating = this.userRating,
            version = this.version
        )
    }
}

@Immutable
@Serializable
data class SubsonicRecordLabel(
    val name: String
)

@Immutable
@Serializable
data class SubsonicItemGenre(
    val name: String
)

@Immutable
@Serializable
data class SubsonicItemDate(
    val year: Int? = 0,
    val month: Int? = 0,
    val day: Int? = 0
)

@Immutable
@Serializable
data class SubsonicDiscTitle(
    val disc: Int,
    val title: String,
    val coverArt: String? = null
)

@Immutable
@Serializable
data class SubsonicReplayGain(
    val trackGain: Float? = 0f,
    val trackPeak: Float? = 0f,
    val albumPeak: Float? = 0f
)

@Serializable
data class SubsonicArtistIndexList(
    val index: List<SubsonicArtistIndex>? = null
)
@Serializable
data class SubsonicArtistIndex(
    val name: String? = null,
    val artist: List<SubsonicArtist> = emptyList()
)
@Immutable
@Serializable
data class SubsonicArtist(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val artistImageUrl: String? = null,
    val albumCount: Int? = null,
    val album: List<SubsonicAlbum>? = null,
    val starred: String? = null,
    val musicBrainzId: String? = null,
    val sortName: String? = null,
    val roles: List<String>? = null
) {
    fun toMediaModel(providerId: String): MediaModel.Artist {
        return MediaModel.Artist(
            id = this@SubsonicArtist.id,
            providerId = providerId,
            providerType = ProviderType.SUBSONIC,
            albumCount = albumCount,
            imageId = coverArt,
            imageUrl = artistImageUrl,
            mbz = musicBrainzId,
            name = name,
            userFavorite = !this.starred.isNullOrEmpty()
        )
    }
}

@Immutable
@Serializable
data class SubsonicArtistInfo(
    val biography: String? = null,
    val musicBrainzId: String? = null,
    val lastFmUrl: String? = null,
    val smallImageUrl: String? = null,
    val mediumImageUrl: String? = null,
    val largeImageUrl: String? = null,
    val similarArtist: List<SubsonicArtist>? = null
) {
    fun toArtistInfo(providerId: String): AlbumArtistInfo =
        AlbumArtistInfo(
            biography = biography,
            imageUrl = largeImageUrl,
            similarArtists = similarArtist?.map { it.toMediaModel(providerId) }
        )
}

@Serializable
data class SubsonicError(val code: Int, val message: String)

@Serializable
data class SubsonicUser(
    val username: String,
    val adminRole: Boolean
)

@Serializable
data class SubsonicInternetRadioStationList(
    val internetRadioStation: List<SubsonicInternetRadioStation> = emptyList()
)
@Serializable
data class SubsonicInternetRadioStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val homePageUrl: String? = null,
    val coverArt: String? = null
) {
    fun toMediaModel(providerId: String): MediaModel.InternetRadioStation {
        return MediaModel.InternetRadioStation(
            id = id,
            providerId = providerId,
            providerType = ProviderType.SUBSONIC,
            homepageUrl = homePageUrl,
            imageId = coverArt,
            imageUrl = coverArt,
            name = name,
            streamUrl = streamUrl,
            uploadedImage = null
        )
    }
}

@Serializable
data class SubsonicPlaylistList(
    val playlist: List<SubsonicPlaylist> = emptyList()
)

@Serializable
data class SubsonicPlaylist(
    val id: String,
    val allowedUser: List<String>? = null,
    val changed: String,
    val comment: String? = null,
    val coverArt: String? = null,
    val created: String,
    val duration: Int,
    val entry: List<SubsonicSong> = emptyList(),
    val name: String,
    val owner: String? = null,
    val public: Boolean? = null,
    val readOnly: Boolean? = null,
    val songCount: Int,
    val validUntil: String? = null
) {
    fun toMediaModel(providerId: String): MediaModel.Playlist {
        return MediaModel.Playlist(
            id = id,
            providerId = providerId,
            providerType = ProviderType.SUBSONIC,
            name = this.name,
            description = this.comment,
            durationMs = this.duration * 1000,
            imageId = this.coverArt,
            imageUrl = this.coverArt,
            owner = this.owner,
            isPublic = this.public,
            songCount = this.songCount,
            uploadedImage = null
        )
    }
}

@Serializable
data class SubsonicLyricsList(
    val structuredLyrics: List<SubsonicStructuredLyrics> = emptyList()
)

@Serializable
data class SubsonicStructuredLyrics(
    val lang: String? = null,
    val synced: Boolean,
    val offset: Int? = null,
    val displayArtist: String? = null,
    val displayTitle: String? = null,
    val line: List<SubsonicLine> = emptyList(),
    // Added in Version 2 (songLyrics extension)
    val kind: String? = null,
    val agents: List<SubsonicAgent>? = null,
    val cueLine: List<SubsonicCueLine>? = null
) {
    fun toLyrics(): Lyrics {
        val lyricOffset = offset ?: 0

        // Unsynced
        if (!synced) {
            return Lyrics(
                syncType = SyncType.NONE,
                source = LyricSource.MEDIA_PROVIDER,
                lines = listOf(
                    LyricsLine(
                        startMs = -1,
                        lines = listOf(Lyric(text = line.fastJoinToString { it.value }))
                    )
                )
            )
        }

        // V2
        if (!cueLine.isNullOrEmpty()) {
            val explicitLines = mutableListOf<LyricsLine>()
            val toGroup = mutableListOf<Lyric>()

            cueLine.forEach { cLine ->
                val agent = agents?.find { it.id == cLine.agentId }
                val lyricsRole = when (agent?.role) {
                    "main" -> LyricsRole.MAIN
                    else -> LyricsRole.BG
                }

                val words = cLine.cue.mapIndexed { index, cue ->
                    val lineBytes = cLine.value.toByteArray(Charsets.UTF_8)
                    val cueByteEnd = cLine.cue.getOrNull(index + 1)?.byteStart?.minus(1) ?: (lineBytes.size - 1)
                    val safeByteStart = cue.byteStart.coerceIn(0, lineBytes.size)
                    val safeByteEnd = cueByteEnd.coerceIn(safeByteStart, lineBytes.size - 1)

                    val cueBytes = lineBytes.sliceArray(safeByteStart..safeByteEnd)

                    SyncedWord(
                        text = String(cueBytes, Charsets.UTF_8),
                        startMs = cue.start + lyricOffset,
                        endMs = cue.end?.plus(lyricOffset)
                    )
                }

                val lineStart = cLine.start + lyricOffset
                val lineEnd = cLine.end?.plus(lyricOffset)

                val needsBgFallback = lyricsRole == LyricsRole.MAIN &&
                        cLine.value.contains('(') && cLine.value.contains(')')

                when {
                    needsBgFallback -> {
                        val subLyrics = separateBackgroundLyrics(words, lineStart, lineEnd).lines.map { subLyric ->
                            subLyric.copy(
                                startMs = subLyric.words?.firstOrNull()?.startMs ?: lineStart,
                                endMs = subLyric.words?.lastOrNull()?.endMs ?: lineEnd
                            )
                        }

                        explicitLines.add(
                            LyricsLine(
                                startMs = subLyrics.minOf { it.startMs!! },
                                endMs = subLyrics.mapNotNull { it.endMs }.maxOrNull(),
                                lines = subLyrics
                            )
                        )
                    }
                    lyricsRole == LyricsRole.BG -> {
                        splitMultipleBg(words).forEach {
                            toGroup.add(
                                Lyric(
                                    text = it.fastJoinToString(""),
                                    words = it,
                                    startMs = lineStart,
                                    endMs = lineEnd,
                                    role = lyricsRole
                                )
                            )
                        }
                    }
                    else -> {
                        toGroup.add(
                            Lyric(
                                text = cLine.value,
                                words = words,
                                startMs = lineStart,
                                endMs = lineEnd,
                                role = lyricsRole
                            )
                        )
                    }
                }
            }

            val groupedLines = mutableListOf<MutableList<Lyric>>()
            var mainGroupIndex: Int? = null
            var lastMain: Lyric? = null

            toGroup.forEach { lyric ->
                if (lyric.role == LyricsRole.MAIN) {
                    groupedLines.add(mutableListOf(lyric))
                    mainGroupIndex = groupedLines.lastIndex
                    lastMain = lyric
                } else {
                    val main = lastMain
                    val withinMain = main != null &&
                            lyric.startMs!! >= main.startMs!! &&
                            lyric.startMs <= (main.endMs ?: main.startMs)

                    if (withinMain && mainGroupIndex != null) {
                        groupedLines[mainGroupIndex!!].add(lyric)
                    } else {
                        groupedLines.add(mutableListOf(lyric))
                    }
                }
            }

            val groupedFromTags = groupedLines.map { group ->
                LyricsLine(
                    startMs = group.minOf { it.startMs!! },
                    endMs = group.mapNotNull { it.endMs }.maxOrNull(),
                    lines = group
                )
            }

            val result = (explicitLines + groupedFromTags).sortedBy { it.startMs }

            return Lyrics(
                syncType = SyncType.WORD,
                source = LyricSource.MEDIA_PROVIDER,
                lines = result
            )
        }

        // V1
        return Lyrics(
            syncType = SyncType.LINE,
            source = LyricSource.MEDIA_PROVIDER,
            lines = line
                .groupBy { (it.start ?: 0) + lyricOffset }
                .map { (timestamp, lines) ->
                    val lyricText = lines.fastJoinToString(" ") { it.value }

                    separateBackgroundLyrics(lyricText, timestamp)
                }
        )
    }
}

@Serializable
data class SubsonicLine(
    val start: Int? = null,
    val value: String
)

@Serializable
data class SubsonicAgent(
    val id: String,
    val name: String? = null,
    val role: String? = null
)

@Serializable
data class SubsonicCueLine(
    val start: Int,
    val end: Int? = null,
    val index: Int,
    val value: String,
    val agentId: String? = null,
    val cue: List<SubsonicCue> = emptyList()
)

@Serializable
data class SubsonicCue(
    val start: Int,
    val end: Int? = null,
    val byteStart: Int,
    val byteEnd: Int
)
