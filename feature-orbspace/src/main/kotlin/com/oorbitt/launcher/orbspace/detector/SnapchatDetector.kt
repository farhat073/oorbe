package com.oorbitt.launcher.orbspace.detector

/**
 * Detects Snapchat Spotlight (short-form) and Stories vs. other Snapchat screens.
 *
 * ## Snapchat's navigation:
 *
 * Snapchat's main screens (swipeable):
 * - **Camera** (center) — NOT short-form
 * - **Chat** (left swipe) — NOT short-form
 * - **Stories** (right swipe → Friends/Discover tab) — ephemeral content, counted as short-form
 * - **Spotlight** (dedicated tab) — Snapchat's TikTok competitor, vertical video feed
 * - **Snap Map** — NOT short-form
 * - **Profile** — NOT short-form
 *
 * Key class names:
 * - `SpotlightFeedFragment` / `SpotlightActivity` → SHORT_FORM
 * - `StoryViewerActivity` / `SnapPlaybackFragment` → SHORT_FORM (viewing a story)
 * - `DiscoverFeedFragment` → SHORT_FORM (Discover has vertical video content)
 * - `ChatFragment` / `ChatActivity` → NOT_SHORT_FORM
 * - `CameraFragment` → NOT_SHORT_FORM
 * - `MapFragment` / `SnapMapFragment` → NOT_SHORT_FORM
 * - `ProfileFragment` → NOT_SHORT_FORM
 */
class SnapchatDetector : PlatformDetector {

    override val packages = setOf(
        "com.snapchat.android"
    )

    override fun onWindowChanged(className: String?, windowTitle: CharSequence?): DetectionResult {
        val cls = className?.lowercase() ?: ""
        val title = windowTitle?.toString()?.lowercase() ?: ""

        // ── SHORT_FORM: Spotlight feed (TikTok-style vertical videos) ────
        if (cls.contains("spotlight")) return DetectionResult.SHORT_FORM

        // ── SHORT_FORM: Story viewer (watching someone's story) ──────────
        if (cls.contains("storyviewer") ||
            cls.contains("snapplayback") ||
            cls.contains("storyplayer")
        ) {
            return DetectionResult.SHORT_FORM
        }

        // ── SHORT_FORM: Discover feed (vertical content feed) ────────────
        if (cls.contains("discover") && cls.contains("feed")) {
            return DetectionResult.SHORT_FORM
        }

        // ── NOT short-form screens ───────────────────────────────────────
        if (cls.contains("camera") ||
            cls.contains("chat") ||
            cls.contains("map") ||
            cls.contains("profile") ||
            cls.contains("settings") ||
            cls.contains("search") ||
            cls.contains("memory") ||    // Memories
            cls.contains("bitmoji") ||
            cls.contains("friends")
        ) {
            return DetectionResult.NOT_SHORT_FORM
        }

        // ── Window title fallback ────────────────────────────────────────
        if (title.contains("spotlight")) return DetectionResult.SHORT_FORM
        if (title.contains("stories") || title.contains("story")) return DetectionResult.SHORT_FORM

        if (title.contains("chat") ||
            title.contains("camera") ||
            title.contains("map") ||
            title.contains("profile")
        ) {
            return DetectionResult.NOT_SHORT_FORM
        }

        return DetectionResult.AMBIGUOUS
    }
}
