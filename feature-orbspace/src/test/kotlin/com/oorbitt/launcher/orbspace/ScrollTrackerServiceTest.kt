package com.oorbitt.launcher.orbspace

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ScrollTrackerServiceTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var service: ScrollTrackerService
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private val prefsMap = mutableMapOf<String, Any>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock Settings.Secure
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(any(), any()) } returns "com.google.android.inputmethod.latin/.LatinIME"

        // Mock System.currentTimeMillis
        mockkStatic(System::class)
        every { System.currentTimeMillis() } returns 1774834800000L // 2026-04-01 fixed timestamp

        // Set up service spy
        service = spyk(ScrollTrackerService())

        // Set up in-memory mock prefs
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        prefsMap.clear()

        every { mockPrefs.getInt(any(), any()) } answers {
            val key = firstArg<String>()
            val default = secondArg<Int>()
            (prefsMap[key] as? Int) ?: default
        }

        every { mockEditor.putInt(any(), any()) } answers {
            val key = firstArg<String>()
            val valInt = secondArg<Int>()
            prefsMap[key] = valInt
            mockEditor
        }

        every { mockEditor.commit() } returns true
        every { mockEditor.apply() } just Runs
        every { mockPrefs.edit() } returns mockEditor
        
        every { service.getSharedPreferences(any(), any()) } returns mockPrefs
        
        // Stub Android Service registration calls
        every { service.registerReceiver(any(), any()) } returns null
        every { service.unregisterReceiver(any()) } just Runs

        val mockContentResolver = mockk<ContentResolver>(relaxed = true)
        every { service.contentResolver } returns mockContentResolver
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun invokeOnServiceConnected() {
        val method = ScrollTrackerService::class.java.getDeclaredMethod("onServiceConnected")
        method.isAccessible = true
        method.invoke(service)
    }

    private fun createEvent(eventType: Int, packageName: String): AccessibilityEvent {
        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns eventType
        every { event.packageName } returns packageName
        every { event.scrollDeltaY } returns 100
        every { event.scrollDeltaX } returns 0
        every { event.eventTime } answers { System.currentTimeMillis() }
        return event
    }

    @Test
    fun testOnServiceConnected_restoresTodayStats() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(1774834800000L))
        prefsMap["${today}_com.instagram.android"] = 42
        prefsMap["${today}_com.instagram.android_sf"] = 15

        invokeOnServiceConnected()

        // Verify loaded stats
        val stats = ScrollTrackerService.todayStats.value
        val sfStats = ScrollTrackerService.todayShortFormStats.value
        assertThat(stats["com.instagram.android"]).isEqualTo(42)
        assertThat(sfStats["com.instagram.android"]).isEqualTo(15)
    }

    @Test
    fun testSingleInertialFling_countsAsOne() {
        invokeOnServiceConnected()
        val pkg = "com.instagram.android"

        // First scroll event
        every { System.currentTimeMillis() } returns 1000L
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, pkg))

        // Subsequent flings within GAP_MS (600ms)
        every { System.currentTimeMillis() } returns 1100L
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, pkg))
        
        every { System.currentTimeMillis() } returns 1300L
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, pkg))

        assertThat(ScrollTrackerService.todayStats.value[pkg]).isEqualTo(1)
    }

    @Test
    fun testTwoDeliberateSwipes_countsAsTwo() {
        invokeOnServiceConnected()
        val pkg = "com.instagram.android"

        // First swipe
        every { System.currentTimeMillis() } returns 1000L
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, pkg))

        // Second swipe after >800ms gap
        every { System.currentTimeMillis() } returns 2000L
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, pkg))

        assertThat(ScrollTrackerService.todayStats.value[pkg]).isEqualTo(2)
    }

    @Test
    fun testShortFormScrollDetection_incrementsBoth() {
        invokeOnServiceConnected()
        val pkg = "com.google.android.youtube" // Will use YouTubeDetector

        // Simulate entering shorts
        val windowEvent = mockk<AccessibilityEvent>(relaxed = true)
        every { windowEvent.eventType } returns AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        every { windowEvent.packageName } returns pkg
        every { windowEvent.className } returns "com.google.android.youtube.shorts.ui.ShortsActivity"
        
        service.onAccessibilityEvent(windowEvent)

        // Simulate scroll
        every { System.currentTimeMillis() } returns 1000L
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, pkg))

        assertThat(ScrollTrackerService.todayStats.value[pkg]).isEqualTo(1)
        assertThat(ScrollTrackerService.todayShortFormStats.value[pkg]).isEqualTo(1)
    }

    @Test
    fun testAppSwitchFinalizesSession_withGracePeriod() = runTest(testDispatcher) {
        invokeOnServiceConnected()
        val pkg1 = "com.instagram.android"
        val pkg2 = "com.android.launcher" // home screen

        // Start session on Instagram
        every { System.currentTimeMillis() } returns 1000L
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, pkg1))
        
        // Count becomes 1 in memory
        assertThat(ScrollTrackerService.todayStats.value[pkg1]).isEqualTo(1)
        assertThat(prefsMap["2026-04-01_com.instagram.android"]).isNull() // Not persisted yet

        // Switch window to launcher (different package, not transient overlay since we want to finalize tracked apps on home press)
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, pkg2))

        // During grace period, should not be written to prefs yet
        testScheduler.advanceTimeBy(100)
        assertThat(prefsMap["2026-04-01_com.instagram.android"]).isNull()

        // Pass the grace period
        testScheduler.advanceTimeBy(300) // total 400ms > DEBOUNCE_FINALIZE_MS
        
        // Count should be committed
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(1774834800000L))
        assertThat(prefsMap["${today}_$pkg1"]).isEqualTo(1)
    }

    @Test
    fun testAppSwitchReverted_cancelsFinalize() = runTest(testDispatcher) {
        invokeOnServiceConnected()
        val pkg1 = "com.instagram.android"
        val pkg2 = "com.android.launcher"

        // Start session
        every { System.currentTimeMillis() } returns 1000L
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, pkg1))

        // Switch window
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, pkg2))

        // Revert back before grace period finishes
        testScheduler.advanceTimeBy(100)
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, pkg1))

        // Advance past grace period
        testScheduler.advanceTimeBy(300)

        // Count should NOT be finalized (isActive is still true)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(1774834800000L))
        assertThat(prefsMap["${today}_$pkg1"]).isNull()
    }

    @Test
    fun testTransientOverlay_doesNotFinalize() = runTest(testDispatcher) {
        invokeOnServiceConnected()
        val pkg1 = "com.instagram.android"
        val systemUi = "com.android.systemui"

        // Start session
        every { System.currentTimeMillis() } returns 1000L
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, pkg1))

        // Notification shade pulled down
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, systemUi))

        // Pass grace period
        testScheduler.advanceTimeBy(400)

        // Should not be finalized
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(1774834800000L))
        assertThat(prefsMap["${today}_$pkg1"]).isNull()
    }

    @Test
    fun testScreenOff_immediatelyCommits() {
        invokeOnServiceConnected()
        val pkg = "com.instagram.android"

        // Retrieve the BroadcastReceiver from registerReceiver mock
        val receiverSlot = slot<BroadcastReceiver>()
        verify { service.registerReceiver(capture(receiverSlot), any()) }
        val receiver = receiverSlot.captured

        // Start session
        every { System.currentTimeMillis() } returns 1000L
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, pkg))

        // Trigger Screen Off
        val intent = Intent(Intent.ACTION_SCREEN_OFF)
        receiver.onReceive(mockk(), intent)

        // Verify immediate synchronous commit
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(1774834800000L))
        assertThat(prefsMap["${today}_$pkg"]).isEqualTo(1)
        
        // Verify commit() was used (blocking write)
        verify { mockEditor.commit() }
    }

    @Test
    fun testDayRollover_splitsSession() {
        invokeOnServiceConnected()
        val pkg = "com.instagram.android"

        val date1 = "2026-06-28"
        val date2 = "2026-06-29"

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val time1 = sdf.parse(date1)!!.time + 3600000L // June 28, 1:00 AM
        val time2 = sdf.parse(date2)!!.time + 3600000L // June 29, 1:00 AM

        // Event on day 1
        every { System.currentTimeMillis() } returns time1
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, pkg)) // count = 1

        // Another event on day 1
        every { System.currentTimeMillis() } returns time1 + 5000L
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, pkg)) // count = 2

        // Event on day 2 (midnight passed)
        every { System.currentTimeMillis() } returns time2
        service.onAccessibilityEvent(createEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, pkg))

        // Verify day 1 committed with 2 scrolls
        assertThat(prefsMap["${date1}_$pkg"]).isEqualTo(2)

        // Verify day 2 has count 1 (since it reset on rollover)
        val stats = ScrollTrackerService.todayStats.value
        assertThat(stats[pkg]).isEqualTo(1)
    }

    @Test
    fun testWeeklyHistoryBreakdown() {
        // Today is 2026-06-29 in mocked time
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeToday = sdf.parse("2026-06-29")!!.time + 3600000L
        every { System.currentTimeMillis() } returns timeToday
        
        val day0 = "2026-06-29" // Today
        val day1 = "2026-06-28" // Yesterday
        val day2 = "2026-06-27"
        val day3 = "2026-06-26"
        val day4 = "2026-06-25"
        val day5 = "2026-06-24"
        val day6 = "2026-06-23"

        prefsMap["${day0}_com.instagram.android"] = 10
        prefsMap["${day0}_com.instagram.android_sf"] = 6
        prefsMap["${day0}_com.google.android.youtube"] = 5
        prefsMap["${day0}_com.google.android.youtube_sf"] = 0
        prefsMap["${day1}_com.instagram.android"] = 20
        prefsMap["${day2}_com.zhiliaoapp.musically"] = 30
        prefsMap["${day2}_com.zhiliaoapp.musically_sf"] = 25
        
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns mockPrefs

        val breakdown = ScrollTrackerService.getWeeklyBreakdown(context)

        // Verify breakdown size
        assertThat(breakdown).hasSize(7)

        // Verify chronological order (oldest first: day6 to day0)
        assertThat(breakdown[0].date).isEqualTo(day6)
        assertThat(breakdown[6].date).isEqualTo(day0)

        // Verify data correctness
        assertThat(breakdown[6].totalScrolls).isEqualTo(15) // 10 + 5
        assertThat(breakdown[6].totalShortFormScrolls).isEqualTo(6)
        assertThat(breakdown[6].appBreakdown["com.instagram.android"]).isEqualTo(10)
        assertThat(breakdown[6].appShortFormBreakdown["com.instagram.android"]).isEqualTo(6)
        assertThat(breakdown[6].appBreakdown["com.google.android.youtube"]).isEqualTo(5)

        assertThat(breakdown[5].date).isEqualTo(day1)
        assertThat(breakdown[5].totalScrolls).isEqualTo(20)

        assertThat(breakdown[4].date).isEqualTo(day2)
        assertThat(breakdown[4].totalScrolls).isEqualTo(30)
        assertThat(breakdown[4].totalShortFormScrolls).isEqualTo(25)

        assertThat(breakdown[0].totalScrolls).isEqualTo(0) // No data, should be 0
    }
}
