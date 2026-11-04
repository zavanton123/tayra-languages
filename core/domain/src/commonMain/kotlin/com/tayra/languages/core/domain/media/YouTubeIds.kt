package com.tayra.languages.core.domain.media

/** YouTube video ids in the links people paste. */
object YouTubeIds {
    private val ID = Regex("""^[A-Za-z0-9_-]{11}$""")
    private val HOSTS = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtube-nocookie.com", "www.youtube-nocookie.com")

    /** The video id in [input]: a youtube.com or youtu.be link of any common shape, or a bare id. */
    fun parse(input: String): String? {
        val text = input.trim()
        if (ID.matches(text)) return text
        val noScheme = text.substringAfter("://", text)
        val host = noScheme.substringBefore('/').substringBefore('?').lowercase()
        val path = noScheme.substringAfter('/', "").substringBefore('#')
        val candidate = when {
            host == "youtu.be" || host == "www.youtu.be" -> path.substringBefore('?').substringBefore('/')
            host in HOSTS -> {
                val route = path.substringBefore('?')
                val query = path.substringAfter('?', "")
                when {
                    route == "watch" || route.isEmpty() -> query.split('&').firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
                    route.startsWith("shorts/") || route.startsWith("embed/") || route.startsWith("live/") || route.startsWith("v/") ->
                        route.substringAfter('/').substringBefore('/')
                    else -> null
                }
            }
            else -> null
        }
        return candidate?.takeIf { ID.matches(it) }
    }

    /** The `source_uri` a YouTube video is stored under. */
    fun uri(id: String): String = "youtube:$id"

    /** The id in a stored [uri], or null for anything else. */
    fun ofUri(uri: String?): String? = uri?.takeIf { it.startsWith("youtube:") }?.removePrefix("youtube:")?.takeIf { ID.matches(it) }

    fun watchUrl(id: String): String = "https://www.youtube.com/watch?v=$id"

    fun thumbnailUrl(id: String): String = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
}
