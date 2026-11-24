package com.idiomiko.feature.playlists

import idiomiko.feature.playlists.generated.resources.Res

/** The cover the system's media controls show for a playlist: the app icon's monkey on its cream, as a PNG. */
internal suspend fun nowPlayingArtwork(): ByteArray? = runCatching { Res.readBytes("files/now_playing_artwork.png") }.getOrNull()
