package com.oorbitt.launcher.orbspace.detector

import org.junit.Assert.assertEquals
import org.junit.Test

class TikTokDetectorTest {

    private val detector = TikTokDetector()

    @Test
    fun `test window changed detects not shorts for specific screens`() {
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged("com.ss.android.ugc.aweme.discover.DiscoverActivity", null))
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged("com.ss.android.ugc.aweme.message.MessageFragment", null))
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged("com.ss.android.ugc.aweme.profile.ProfileFragment", null))
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged("com.ss.android.ugc.aweme.live.LiveStreamActivity", null))
        assertEquals(DetectionResult.NOT_SHORT_FORM, detector.onWindowChanged(null, "Inbox"))
    }

    @Test
    fun `test window changed defaults to shorts for main feed`() {
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged("com.ss.android.ugc.aweme.main.MainActivity", null))
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged(null, "For You"))
    }

    @Test
    fun `test window changed defaults to shorts for unknown screens`() {
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged("com.unknown.Activity", "Unknown Title"))
        assertEquals(DetectionResult.SHORT_FORM, detector.onWindowChanged(null, null))
    }
}
