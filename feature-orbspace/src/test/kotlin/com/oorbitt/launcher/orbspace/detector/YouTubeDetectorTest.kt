package com.oorbitt.launcher.orbspace.detector

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeDetectorTest {

    private val detector = YouTubeDetector()

    @Test
    fun `test window changed detects shorts`() {
        // High confidence class name
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged("com.google.android.youtube.shorts.ui.ShortsActivity", null))
        
        // Medium confidence title
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged(null, "Shorts"))
    }

    @Test
    fun `test window changed detects not shorts`() {
        // Full video player
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged("com.google.android.youtube.WatchWhileActivity", null))
        
        // Search
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged("com.google.android.youtube.SearchActivity", null))

        // Titles
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged(null, "Home"))
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged(null, "Subscriptions"))
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged(null, "Library"))
    }

    @Test
    fun `test window changed ambiguous`() {
        assertEquals(DetectionResult.AMBIGUOUS, detector.onWindowChanged(null, null))
        assertEquals(DetectionResult.AMBIGUOUS, detector.onWindowChanged("com.google.android.youtube.UnknownActivity", "Unknown Title"))
    }

    @Test
    fun `test view hierarchy detects shorts`() {
        val nodeInfo = NodeSnapshot(
            className = "android.widget.FrameLayout",
            contentDescription = null,
            viewIdResourceName = "reel_recycler_view"
        )
        assertEquals(DetectionResult.SHORT_FORM, detector.onViewHierarchyAvailable(nodeInfo))
        
        val nodeInfo2 = NodeSnapshot(
            className = "android.widget.FrameLayout",
            contentDescription = null,
            viewIdResourceName = "shorts_video_player"
        )
        assertEquals(DetectionResult.SHORT_FORM, detector.onViewHierarchyAvailable(nodeInfo2))
    }
    
    @Test
    fun `test view hierarchy ambiguous`() {
        val nodeInfo = NodeSnapshot(
            className = "android.widget.FrameLayout",
            contentDescription = null,
            viewIdResourceName = "some_other_view"
        )
        assertEquals(DetectionResult.AMBIGUOUS, detector.onViewHierarchyAvailable(nodeInfo))
    }
}
