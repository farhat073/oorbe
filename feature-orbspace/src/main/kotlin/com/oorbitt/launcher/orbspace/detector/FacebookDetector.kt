package com.oorbitt.launcher.orbspace.detector

/**
 * Detects Facebook Reels vs. regular Facebook browsing.
 *
 * ## Facebook's architecture:
 *
 * Facebook (`com.facebook.katana`) is a single-Activity app with a massive
 * fragment-based navigation system. The main container is typically
 * `FbMainTabActivity` or `FacebookMainActivity`.
 *
 * Key navigation surfaces:
 * - **Reels tab**: Accessible from the bottom nav or Video tab. Uses fragments
 *   with "reel" in the name (`ReelsFragment`, `FBReelsViewerFragment`,
 *   `ReelPlayerFragment`).
 * - **News Feed**: `NewsFeedFragment`, `FeedFragment`.
 * - **Video tab**: `VideoTabFragment` — this shows a mix of regular videos AND
 *   Reels. We only count it as short-form if the user is in the Reels sub-section.
 * - **Marketplace**: `MarketplaceFragment`.
 * - **Groups**: `GroupsFragment`.
 * - **Messenger (embedded)**: `MessengerFragment`, chat-related fragments.
 * - **Profile**: `ProfileFragment`, `TimelineFragment`.
 *
 * ## Detection strategy:
 *
 * Fragment/class name containing "reel" → SHORT_FORM.
 * All other known screens → NOT_SHORT_FORM.
 */
class FacebookDetector : PlatformDetector {

    override val packages = setOf(
        "com.facebook.katana"
    )

    override fun onWindowChanged(className: String?, windowTitle: CharSequence?): DetectionResult {
        val cls = className?.lowercase() ?: ""
        val title = windowTitle?.toString()?.lowercase() ?: ""

        // ── Reels-specific fragments ─────────────────────────────────────
        if (cls.contains("reel") || cls.contains("fbreels")) {
            return DetectionResult.SHORT_FORM
        }

        // ── NOT short-form screens ───────────────────────────────────────
        if (cls.contains("feed") ||
            cls.contains("newsfeed") ||
            cls.contains("marketplace") ||
            cls.contains("groups") ||
            cls.contains("messenger") ||
            cls.contains("chat") ||
            cls.contains("profile") ||
            cls.contains("timeline") ||
            cls.contains("notification") ||
            cls.contains("settings") ||
            cls.contains("search") ||
            cls.contains("comment") ||
            cls.contains("photo") ||
            cls.contains("story")     // FB Stories ≠ Reels
        ) {
            return DetectionResult.NOT_SHORT_FORM
        }

        // ── Window title checks ──────────────────────────────────────────
        if (title.contains("reel")) return DetectionResult.SHORT_FORM

        if (title.contains("news feed") ||
            title.contains("marketplace") ||
            title.contains("groups") ||
            title.contains("profile") ||
            title.contains("menu") ||
            title.contains("search") ||
            title.contains("notification") ||
            title.contains("watch")   // "Watch" tab is mixed content, not pure Reels
        ) {
            return DetectionResult.NOT_SHORT_FORM
        }

        return DetectionResult.AMBIGUOUS
    }

    override fun onViewHierarchyAvailable(nodeInfo: NodeSnapshot?): DetectionResult {
        if (nodeInfo == null) return DetectionResult.AMBIGUOUS

        val hasReelsView = nodeInfo.any { node ->
            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            viewId.contains("reel") || desc.contains("reel")
        }

        return if (hasReelsView) DetectionResult.SHORT_FORM else DetectionResult.AMBIGUOUS
    }
}
