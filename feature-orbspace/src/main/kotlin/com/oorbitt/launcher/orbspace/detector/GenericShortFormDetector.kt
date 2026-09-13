package com.oorbitt.launcher.orbspace.detector

/**
 * Fallback detector for apps that are ENTIRELY short-form video.
 *
 * These apps don't have a "home feed vs. Reels" distinction — their
 * primary content IS vertical short-form video. So any scroll in the
 * main activity counts as a short-form video view.
 *
 * ## Covered apps:
 * - **Moj** (`in.mohalla.video`, `com.moj.shortvideoapp`) — Indian short video
 * - **ShareChat** (`in.mohalla.sharechat`) — Indian social + short video
 * - **Roposo** (`com.roposo.android`) — Indian short video
 * - **Bigo Live** (`com.bigo.live`) — has a Reels-like feed
 * - **Triller** (`com.triller.android`) — music short videos
 *
 * ## Detection strategy:
 *
 * Default to SHORT_FORM for any main activity. Only exclude settings,
 * profile, and DM screens.
 */
class GenericShortFormDetector : PlatformDetector {

    override val packages = setOf(
        "in.mohalla.video",
        "com.moj.shortvideoapp",
        "in.mohalla.sharechat",
        "com.roposo.android",
        "com.bigo.live",
        "com.triller.android"
    )

    override fun onWindowChanged(className: String?, windowTitle: CharSequence?): DetectionResult {
        val cls = className?.lowercase() ?: ""
        val title = windowTitle?.toString()?.lowercase() ?: ""

        // ── NOT short-form: universal non-video screens ──────────────────
        if (cls.contains("setting") ||
            cls.contains("profile") ||
            cls.contains("chat") ||
            cls.contains("message") ||
            cls.contains("inbox") ||
            cls.contains("edit") ||
            cls.contains("login") ||
            cls.contains("signup") ||
            cls.contains("webview")
        ) {
            return DetectionResult.NOT_SHORT_FORM
        }

        if (title.contains("setting") ||
            title.contains("profile") ||
            title.contains("chat") ||
            title.contains("message") ||
            title.contains("login")
        ) {
            return DetectionResult.NOT_SHORT_FORM
        }

        // ── Default: these apps are short-form video apps ────────────────
        return DetectionResult.SHORT_FORM
    }
}
