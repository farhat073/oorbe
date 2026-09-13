package com.oorbitt.launcher.orbspace.detector

/**
 * Contract for platform-specific short-form video detection.
 *
 * Each implementation knows how to interpret accessibility events from one
 * or more app packages and determine whether the user is viewing short-form
 * content (Shorts, Reels, Stories, vertical feed videos).
 *
 * Implementations must be:
 * - **Stateless-safe**: All mutable state lives in the service, not here.
 * - **Fast**: Called on the main thread from an AccessibilityService.
 *   No I/O, no allocations beyond small strings.
 * - **Testable**: Only primitive data passes through the interface —
 *   no Android framework types in the signatures.
 */
interface PlatformDetector {

    /** Package names this detector handles. */
    val packages: Set<String>

    /**
     * Called when TYPE_WINDOW_STATE_CHANGED fires for a tracked package.
     *
     * @param className  The fully-qualified class name of the new window
     *                   (Activity or Fragment), e.g. "com.google.android.youtube.shorts.ShortsActivity".
     *                   May be null if the system didn't supply it.
     * @param windowTitle The window title / content description text.
     *                    May be null.
     * @return [DetectionResult] indicating whether the user is now in short-form mode.
     */
    fun onWindowChanged(className: String?, windowTitle: CharSequence?): DetectionResult

    /**
     * Optional secondary check using a snapshot of the view hierarchy.
     * Called only when [onWindowChanged] returned [DetectionResult.AMBIGUOUS]
     * and the service's rate-limiter allows a hierarchy inspection.
     *
     * @param nodeInfo A lightweight, recycled-safe snapshot of the root node tree.
     *                 May be null if the system couldn't provide one.
     * @return [DetectionResult] — should try to resolve the ambiguity.
     */
    fun onViewHierarchyAvailable(nodeInfo: NodeSnapshot?): DetectionResult {
        // Default: can't resolve ambiguity from hierarchy alone.
        return DetectionResult.AMBIGUOUS
    }

    /** Reset internal state (called when the app is backgrounded or a new session starts). */
    fun reset() { /* Default no-op for stateless detectors */ }
}

/**
 * Tri-state result of a short-form detection check.
 */
enum class DetectionResult {
    /** User is definitely viewing short-form content (Shorts, Reels, Stories, TikTok feed). */
    SHORT_FORM,

    /** User is definitely NOT viewing short-form content (home feed, DMs, search, profile). */
    NOT_SHORT_FORM,

    /**
     * Can't determine from the available signal alone.
     * The service should either:
     * - Keep the previous state (if one existed), or
     * - Request a view hierarchy inspection via [PlatformDetector.onViewHierarchyAvailable].
     */
    AMBIGUOUS
}

/**
 * Lightweight, immutable snapshot of an [android.view.accessibility.AccessibilityNodeInfo] subtree.
 *
 * We snapshot instead of passing raw NodeInfo objects because:
 * 1. NodeInfo must be recycled — holding references leaks them.
 * 2. We want detectors to be unit-testable without Android framework mocks.
 * 3. We only need a tiny fraction of the tree (class name, content description, view ID).
 *
 * The snapshot is built once per hierarchy inspection and discarded immediately after.
 */
data class NodeSnapshot(
    val className: String?,
    val contentDescription: String?,
    val viewIdResourceName: String?,
    val children: List<NodeSnapshot> = emptyList()
) {
    /**
     * Depth-first search for a node matching the predicate.
     * Returns the first match or null.
     */
    fun findFirst(predicate: (NodeSnapshot) -> Boolean): NodeSnapshot? {
        if (predicate(this)) return this
        for (child in children) {
            val found = child.findFirst(predicate)
            if (found != null) return found
        }
        return null
    }

    /**
     * Check if any node in the subtree matches the predicate.
     */
    fun any(predicate: (NodeSnapshot) -> Boolean): Boolean = findFirst(predicate) != null
}
