package com.tayra.languages.feature.playlists

import tayra_languages.feature.playlists.generated.resources.Res

/** The cover the system's media controls show for a playlist: the app's tayra on its blue, as a PNG. */
internal suspend fun nowPlayingArtwork(): ByteArray? = runCatching { Res.readBytes("files/now_playing_artwork.png") }.getOrNull()
