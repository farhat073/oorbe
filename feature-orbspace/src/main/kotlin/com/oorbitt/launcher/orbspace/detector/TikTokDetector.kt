package com.oorbitt.launcher.orbspace.detector

/**
 * Detects TikTok's short-form video feed vs. non-video screens.
 *
 * ## TikTok's architecture:
 *
 * TikTok is fundamentally a short-form video app — the main "For You" and
 * "Following" feeds are vertical video pagers. So the default state when
 * the user is in TikTok's main activity IS short-form.
 *
 * However, TikTok also has non-video screens:
 * - **Discover/Search**: `com.ss.android.ugc.aweme.discover.DiscoverActivity`
 * - **Inbox/DMs**: fragments containing "im" or "inbox" or "message"
 * - **Profile**: fragments containing "profile" or "me"
 * - **Settings**: fragments containing "setting"
 * - **Live streams**: `LiveStreamActivity` (these are long-form, not shorts)
 * - **LIVE tab**: Watching a live stream is NOT short-form scrolling
 *
 * ## Detection strategy:
 *
 * Unlike YouTube/Instagram, TikTok defaults to SHORT_FORM and we only
 * flip to NOT_SHORT_FORM for known non-video screens.
 *
 * ## Covered packages:
 * - `com.zhiliaoapp.musically` (TikTok global / US)
 * - `com.ss.android.ugc.trill` (TikTok regional variants)
 */
class TikTokDetector : PlatformDetector {

    override val packages = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill"
    )

    override fun onWindowChanged(className: String?, windowTitle: CharSequence?): DetectionResult {
        val cls = className?.lowercase() ?: ""
        val title = windowTitle?.toString()?.lowercase() ?: ""

        // ── NOT short-form: specific non-video screens ───────────────────

        // Search / Discover tab
        if (cls.contains("discover") || cls.contains("search")) {
            return DetectionResult.NOT_SHORT_FORM
        }

        // DMs / Inbox
        if (cls.contains("inbox") ||
            cls.contains("message") ||
            cls.contains("direct") ||
            cls.contains(".im.")     // TikTok's IM module uses "im" package segment
        ) {
            return DetectionResult.NOT_SHORT_FORM
        }

        // Profile / Me tab
        if (cls.contains("profile") || cls.contains("userfragment")) {
            return DetectionResult.NOT_SHORT_FORM
        }

        // Settings
        if (cls.contains("setting")) {
            return DetectionResult.NOT_SHORT_FORM
        }

        // Live streams — long-form content, not short-form scrolling
        if (cls.contains("live") || cls.contains("livestream")) {
            return DetectionResult.NOT_SHORT_FORM
        }

        // ── Window title fallback ────────────────────────────────────────
        if (title.contains("inbox") ||
            title.contains("message") ||
            title.contains("discover") ||
            title.contains("profile") ||
            title.contains("setting") ||
            title.contains("live")
        ) {
            return DetectionResult.NOT_SHORT_FORM
        }

        // ── Default: TikTok's main feed is short-form ────────────────────
        // If we're in the main activity and none of the above screens matched,
        // the user is most likely watching the For You / Following feed.
        if (cls.contains("main") ||
            cls.contains("aweme") ||         // TikTok's internal module name
            title.contains("tiktok") ||
            title.contains("for you") ||
            title.contains("following")
        ) {
            return DetectionResult.SHORT_FORM
        }

        // If we genuinely can't tell (some overlay, unknown new screen),
        // default to SHORT_FORM for TikTok since that's the primary use case.
        return DetectionResult.SHORT_FORM
    }
}
