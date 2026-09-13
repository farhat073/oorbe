package com.oorbitt.launcher.orbspace.detector

import org.junit.Assert.assertEquals
import org.junit.Test

class InstagramDetectorTest {

    private val detector = InstagramDetector()

    @Test
    fun `test window changed detects shorts`() {
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged("com.instagram.reels.fragment.ReelsViewerFragment", null))
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged("com.instagram.clips.viewer.ClipsViewerFragment", null))
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged(null, "Reels"))
    }

    @Test
    fun `test window changed detects not shorts`() {
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged("com.instagram.reels.fragment.StoryViewerFragment", null))
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged("com.instagram.feed.MainFeedFragment", null))
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged("com.instagram.direct.DirectThreadFragment", null))
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged(null, "Home"))
    }

    @Test
    fun `test window changed ambiguous`() {
        assertEquals(DetectionResult.AMBIGUOUS, detector.onWindowChanged("com.instagram.unknown.Fragment", "Unknown Title"))
        assertEquals(DetectionResult.AMBIGUOUS, detector.onWindowChanged(null, null))
    }

    @Test
    fun `test view hierarchy detects shorts via view id`() {
        val nodeInfo = NodeSnapshot(
            className = "android.view.View",
            contentDescription = null,
            viewIdResourceName = "clips_viewer_view_pager"
        )
        assertEquals(DetectionResult.SHORT_FORM, detector.onViewHierarchyAvailable(nodeInfo))
    }
    
    @Test
    fun `test view hierarchy detects shorts via content description`() {
        val nodeInfo = NodeSnapshot(
            className = "android.view.View",
            contentDescription = "Reel by somebody",
            viewIdResourceName = null
        )
        assertEquals(DetectionResult.SHORT_FORM, detector.onViewHierarchyAvailable(nodeInfo))
    }

    @Test
    fun `test view hierarchy ambiguous`() {
        val nodeInfo = NodeSnapshot(
            className = "android.view.View",
            contentDescription = "Story by somebody",
            viewIdResourceName = "story_viewer_pager"
        )
        assertEquals(DetectionResult.AMBIGUOUS, detector.onViewHierarchyAvailable(nodeInfo))
    }
}
