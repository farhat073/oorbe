package com.oorbitt.launcher.orbspace.detector

/**
 * Detects YouTube Shorts vs. regular YouTube browsing.
 *
 * ## How YouTube's navigation works internally:
 *
 * YouTube uses a single-Activity architecture for most navigation:
 * - **`com.google.android.youtube.app.honeycomb.Shell$HomeActivity`** — main container
 *   for Home, Subscriptions, Library tabs, and the Shorts tab.
 * - **`com.google.android.youtube.shorts.ui.ShortsActivity`** — dedicated Shorts player
 *   (launched when you tap a Short from search, notifications, or external links).
 *
 * When the user taps the "Shorts" bottom tab from the home screen, YouTube doesn't
 * launch ShortsActivity — it swaps a fragment inside HomeActivity. So we can't rely
 * solely on Activity class names. We use a two-layer approach:
 *
 * 1. **Activity-level** (fast, high confidence):
 *    - `ShortsActivity` / class name containing "shorts" → SHORT_FORM
 *    - `WatchWhileActivity` (full video player) → NOT_SHORT_FORM
 *    - `SearchActivity` → NOT_SHORT_FORM
 *
 * 2. **Window title / content description** (medium confidence):
 *    - Title containing "Shorts" → SHORT_FORM
 *    - Title containing "Home", "Subscriptions", "Library" → NOT_SHORT_FORM
 *
 * 3. **View hierarchy** (slowest, highest accuracy):
 *    - Look for `reel_recycler_view` or `shorts_video_player` view IDs
 *
 * ## Covered packages:
 * - `com.google.android.youtube` (official)
 * - `app.revanced.android.youtube` (ReVanced)
 * - `com.vanced.android.youtube` (legacy Vanced)
 * - `com.google.android.apps.youtube.creator` (YT Studio — has Shorts preview)
 */
class YouTubeDetector : PlatformDetector {

    override val packages = setOf(
        "com.google.android.youtube",
        "app.revanced.android.youtube",
        "com.vanced.android.youtube",
        "com.google.android.apps.youtube.creator"
    )

    override fun onWindowChanged(className: String?, windowTitle: CharSequence?): DetectionResult {
        val cls = className?.lowercase() ?: ""
        val title = windowTitle?.toString()?.lowercase() ?: ""

        // ── High-confidence Activity checks ──────────────────────────────
        // Dedicated Shorts player Activity — 100% certain.
        if (cls.contains("shorts")) return DetectionResult.SHORT_FORM

        // Full video player (normal videos, live streams, premieres).
        if (cls.contains("watchwh")) return DetectionResult.NOT_SHORT_FORM

        // Search screen — the user is searching, not watching Shorts.
        if (cls.contains("search")) return DetectionResult.NOT_SHORT_FORM

        // ── Medium-confidence title checks ───────────────────────────────
        // The Shorts tab inside HomeActivity sets the window title to "Shorts".
        if (title.contains("shorts")) return DetectionResult.SHORT_FORM

        // Other well-known tabs — definitely NOT short-form.
        if (title.contains("home") ||
            title.contains("subscriptions") ||
            title.contains("library") ||
            title.contains("you")) {
            return DetectionResult.NOT_SHORT_FORM
        }

        // ── Can't tell ──────────────────────────────────────────────────
        // This happens for transient overlays (e.g. share sheet inside YT),
        // pip mode, or unknown new activities from app updates.
        return DetectionResult.AMBIGUOUS
    }

    override fun onViewHierarchyAvailable(nodeInfo: NodeSnapshot?): DetectionResult {
        if (nodeInfo == null) return DetectionResult.AMBIGUOUS

        // Look for Shorts-specific view IDs in the hierarchy.
        // YouTube uses RecyclerView with id "reel_recycler_view" for the Shorts feed,
        // and "shorts_video_player" for the player container.
        val hasShortsView = nodeInfo.any { node ->
            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            viewId.contains("reel_recycler") ||
                viewId.contains("shorts_video") ||
                viewId.contains("shorts_player") ||
                viewId.contains("reel_player")
        }

        return if (hasShortsView) DetectionResult.SHORT_FORM else DetectionResult.AMBIGUOUS
    }
}
