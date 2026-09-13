package com.oorbitt.launcher.orbspace.detector

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapchatDetectorTest {

    private val detector = SnapchatDetector()

    @Test
    fun `test window changed detects shorts`() {
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged("com.snapchat.SpotlightActivity", null))
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged("com.snapchat.StoryViewerActivity", null))
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged("com.snapchat.DiscoverFeedFragment", null))
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged(null, "Spotlight"))
    }

    @Test
    fun `test window changed detects not shorts`() {
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged("com.snapchat.ChatActivity", null))
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged("com.snapchat.CameraFragment", null))
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged(null, "Camera"))
    }

    @Test
    fun `test window changed ambiguous`() {
        assertEquals(DetectionResult.AMBIGUOUS, detector.onWindowChanged("com.snapchat.unknown.Fragment", "Unknown Title"))
        assertEquals(DetectionResult.AMBIGUOUS, detector.onWindowChanged(null, null))
    }
}
