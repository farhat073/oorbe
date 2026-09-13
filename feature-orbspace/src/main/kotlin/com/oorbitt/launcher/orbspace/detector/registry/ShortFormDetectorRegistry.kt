package com.oorbitt.launcher.orbspace.detector.registry

import com.oorbitt.launcher.orbspace.detector.FacebookDetector
import com.oorbitt.launcher.orbspace.detector.GenericShortFormDetector
import com.oorbitt.launcher.orbspace.detector.InstagramDetector
import com.oorbitt.launcher.orbspace.detector.PlatformDetector
import com.oorbitt.launcher.orbspace.detector.SnapchatDetector
import com.oorbitt.launcher.orbspace.detector.TikTokDetector
import com.oorbitt.launcher.orbspace.detector.YouTubeDetector

/**
 * Singleton registry that holds all [PlatformDetector] implementations
 * and maps package names to their respective detector.
 *
 * This allows the AccessibilityService to route events to the correct
 * detector in O(1) time without keeping instances inside the service.
 */
object ShortFormDetectorRegistry {

    private val detectors = listOf(
        YouTubeDetector(),
        InstagramDetector(),
        TikTokDetector(),
        FacebookDetector(),
        SnapchatDetector(),
        GenericShortFormDetector()
    )

    private val packageMap: Map<String, PlatformDetector> = buildMap {
        for (detector in detectors) {
            for (pkg in detector.packages) {
                put(pkg, detector)
            }
        }
    }

    /**
     * Returns the [PlatformDetector] responsible for the given package,
     * or null if this package doesn't have short-form detection logic.
     */
    fun getDetectorFor(packageName: String): PlatformDetector? {
        return packageMap[packageName]
    }

    /**
     * Checks if a package has short-form detection support.
     */
    fun supportsPackage(packageName: String): Boolean {
        return packageMap.containsKey(packageName)
    }

    /**
     * Resets all stateful detectors (if any exist).
     */
    fun resetAll() {
        detectors.forEach { it.reset() }
    }
}
