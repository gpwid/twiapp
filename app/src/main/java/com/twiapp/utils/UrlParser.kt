package com.twiapp.utils

/**
 * Supported social media platforms.
 */
enum class Platform(val displayName: String) {
    TIKTOK("TikTok"),
    INSTAGRAM("Instagram"),
    YOUTUBE("YouTube"),
    TWITTER("Twitter / X"),
    FACEBOOK("Facebook");
}

/**
 * Detects the social media platform from a URL.
 */
object UrlParser {

    private val platformPatterns = mapOf(
        Platform.TIKTOK to listOf(
            Regex("""(?:https?://)?(?:www\.)?tiktok\.com/@[\w.-]+/video/\d+""", RegexOption.IGNORE_CASE),
            Regex("""(?:https?://)?(?:vm|vt)\.tiktok\.com/[\w]+""", RegexOption.IGNORE_CASE),
            Regex("""(?:https?://)?(?:www\.)?tiktok\.com/t/[\w]+""", RegexOption.IGNORE_CASE),
        ),
        Platform.INSTAGRAM to listOf(
            Regex("""(?:https?://)?(?:www\.)?instagram\.com/(?:p|reel|reels|tv)/[\w-]+""", RegexOption.IGNORE_CASE),
            Regex("""(?:https?://)?(?:www\.)?instagram\.com/stories/[\w.-]+/\d+""", RegexOption.IGNORE_CASE),
            Regex("""(?:https?://)?(?:www\.)?instagram\.com/[\w.-]+/(?:p|reel)/[\w-]+""", RegexOption.IGNORE_CASE),
        ),
        Platform.YOUTUBE to listOf(
            Regex("""(?:https?://)?(?:www\.)?youtube\.com/watch\?v=[\w-]+""", RegexOption.IGNORE_CASE),
            Regex("""(?:https?://)?youtu\.be/[\w-]+""", RegexOption.IGNORE_CASE),
            Regex("""(?:https?://)?(?:www\.)?youtube\.com/shorts/[\w-]+""", RegexOption.IGNORE_CASE),
            Regex("""(?:https?://)?(?:m\.)?youtube\.com/watch\?v=[\w-]+""", RegexOption.IGNORE_CASE),
        ),
        Platform.TWITTER to listOf(
            Regex("""(?:https?://)?(?:www\.)?(?:twitter|x)\.com/\w+/status/\d+""", RegexOption.IGNORE_CASE),
            Regex("""(?:https?://)?(?:mobile\.)?(?:twitter|x)\.com/\w+/status/\d+""", RegexOption.IGNORE_CASE),
            Regex("""(?:https?://)?t\.co/[\w]+""", RegexOption.IGNORE_CASE),
        ),
        Platform.FACEBOOK to listOf(
            Regex("""(?:https?://)?(?:www\.)?facebook\.com/.+/videos/\d+""", RegexOption.IGNORE_CASE),
            Regex("""(?:https?://)?(?:www\.)?facebook\.com/watch/?\?v=\d+""", RegexOption.IGNORE_CASE),
            Regex("""(?:https?://)?(?:www\.)?facebook\.com/reel/\d+""", RegexOption.IGNORE_CASE),
            Regex("""(?:https?://)?fb\.watch/[\w]+""", RegexOption.IGNORE_CASE),
            Regex("""(?:https?://)?(?:www\.)?facebook\.com/share/[\w]+""", RegexOption.IGNORE_CASE),
        ),
    )

    /**
     * Detects the platform from a URL string.
     * Returns null if the URL doesn't match any supported platform.
     */
    fun detectPlatform(url: String): Platform? {
        val trimmed = url.trim()
        for ((platform, patterns) in platformPatterns) {
            for (pattern in patterns) {
                if (pattern.containsMatchIn(trimmed)) {
                    return platform
                }
            }
        }
        return null
    }

    /**
     * Checks if the URL is from a supported platform.
     */
    fun isSupported(url: String): Boolean = detectPlatform(url) != null

    /**
     * Extracts a URL from shared text which may contain extra text around it.
     */
    fun extractUrl(text: String): String? {
        val urlRegex = Regex("""https?://\S+""")
        return urlRegex.find(text.trim())?.value
    }
}
