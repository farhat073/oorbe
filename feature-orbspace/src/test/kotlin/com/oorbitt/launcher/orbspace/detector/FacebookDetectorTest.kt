package com.oorbitt.launcher.orbspace.detector

import org.junit.Assert.assertEquals
import org.junit.Test

class FacebookDetectorTest {

    private val detector = FacebookDetector()

    @Test
    fun `test window changed detects shorts`() {
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged("com.facebook.reels.ReelsFragment", null))
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged(null, "Reels"))
    }

    @Test
    fun `test window changed detects not shorts`() {
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged("com.facebook.feed.NewsFeedFragment", null))
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged("com.facebook.messenger.ChatFragment", null))
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged(null, "News Feed"))
    }

    @Test
    fun `test window changed ambiguous`() {
        assertEquals(DetectionResult.AMBIGUOUS, detector.onWindowChanged("com.facebook.unknown.Fragment", "Unknown Title"))
        assertEquals(DetectionResult.AMBIGUOUS, detector.onWindowChanged(null, null))
    }

    @Test
    fun `test view hierarchy detects shorts via view id`() {
        val nodeInfo = NodeSnapshot(
            className = "android.view.View",
            contentDescription = null,
            viewIdResourceName = "reel_viewer_pager"
        )
        assertEquals(DetectionResult.SHORT_FORM, detector.onViewHierarchyAvailable(nodeInfo))
    }

    @Test
    fun `test view hierarchy ambiguous`() {
        val nodeInfo = NodeSnapshot(
            className = "android.view.View",
            contentDescription = "Unknown",
            viewIdResourceName = "unknown"
        )
        assertEquals(DetectionResult.AMBIGUOUS, detector.onViewHierarchyAvailable(nodeInfo))
    }
}
