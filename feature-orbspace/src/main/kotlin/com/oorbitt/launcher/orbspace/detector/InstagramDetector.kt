package com.oorbitt.launcher.orbspace.detector

/**
 * Detects Instagram Reels vs. regular Instagram browsing.
 *
 * ## How Instagram's navigation works internally:
 *
 * Instagram is a single-Activity app — almost everything runs inside
 * `com.instagram.android.activity.MainTabActivity`. Fragment transitions
 * fire TYPE_WINDOW_STATE_CHANGED with the Fragment class name.
 *
 * Key fragments / windows:
 * - **Reels tab**: `ReelsFragment`, `ClipsViewerFragment`, `ReelViewerFragment`,
 *   `IgReelsFragment`, or classes containing "reel" / "clips".
 * - **Home feed**: `FeedFragment`, `MainFeedFragment`, `TimelineFeedFragment`.
 * - **Explore/Search**: `ExploreFragment`, `SearchFragment`.
 * - **DMs**: `DirectThreadFragment`, `DirectInboxFragment`.
 * - **Profile**: `ProfileFragment`, `UserDetailFragment`.
 * - **Stories**: `StoryViewerFragment`, `ReelViewerFragment` (yes, Stories
 *   and Reels share some class names — we disambiguate via content description).
 *
 * ## Detection strategy:
 *
 * 1. Fragment/class name containing "reel" or "clips" → SHORT_FORM
 *    (with exclusion for "storyviewer" which is ephemeral stories, not Reels)
 * 2. Fragment name for feed/explore/DMs/profile → NOT_SHORT_FORM
 * 3. Window title containing "Reels" → SHORT_FORM
 * 4. View hierarchy: look for `clips_viewer_view_pager` or `reels_viewer_pager`
 */
class InstagramDetector : PlatformDetector {

    override val packages = setOf(
        "com.instagram.android"
    )

    override fun onWindowChanged(className: String?, windowTitle: CharSequence?): DetectionResult {
        val cls = className?.lowercase() ?: ""
        val title = windowTitle?.toString()?.lowercase() ?: ""

        // ── High-confidence: Reels-specific fragments ────────────────────
        // Instagram's Reels viewer uses fragments with "reel" or "clips" in the name.
        // We exclude "story" to avoid confusing Stories with Reels.
        if ((cls.contains("reel") || cls.contains("clips")) &&
            !cls.contains("story")
        ) {
            return DetectionResult.SHORT_FORM
        }

        // ── High-confidence: NOT short-form fragments ────────────────────
        if (cls.contains("feed") ||
            cls.contains("timeline") ||
            cls.contains("explore") ||
            cls.contains("search") ||
            cls.contains("direct") ||    // DMs
            cls.contains("inbox") ||     // DMs inbox
            cls.contains("profile") ||
            cls.contains("userdetail") ||
            cls.contains("settings") ||
            cls.contains("edit") ||
            cls.contains("camera") ||
            cls.contains("story")        // Stories ≠ Reels
        ) {
            return DetectionResult.NOT_SHORT_FORM
        }

        // ── Medium-confidence: Window title ──────────────────────────────
        if (title.contains("reel")) return DetectionResult.SHORT_FORM

        if (title.contains("home") ||
            title.contains("explore") ||
            title.contains("message") ||
            title.contains("profile") ||
            title.contains("search")
        ) {
            return DetectionResult.NOT_SHORT_FORM
        }

        return DetectionResult.AMBIGUOUS
    }

    override fun onViewHierarchyAvailable(nodeInfo: NodeSnapshot?): DetectionResult {
        if (nodeInfo == null) return DetectionResult.AMBIGUOUS

        // Look for Instagram's Reels-specific view IDs.
        val hasReelsView = nodeInfo.any { node ->
            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            viewId.contains("clips_viewer") ||
                viewId.contains("reels_viewer") ||
                viewId.contains("reel_pager") ||
                viewId.contains("clips_pager")
        }

        // Also check content descriptions for Reels indicators.
        val hasReelsContent = nodeInfo.any { node ->
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            desc.contains("reel") && !desc.contains("story")
        }

        return when {
            hasReelsView || hasReelsContent -> DetectionResult.SHORT_FORM
            else -> DetectionResult.AMBIGUOUS
        }
    }
}
