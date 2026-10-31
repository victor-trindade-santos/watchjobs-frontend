package com.watchjobs

import com.watchjobs.model.AppNotification
import com.watchjobs.model.NotificationCategory
import com.watchjobs.util.elapsedLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class NotificationLabelsTest {
    private val now = Instant.parse("2026-10-30T12:00:00Z")

    @Test
    fun elapsedLabelIsShort() {
        assertEquals("agora", elapsedLabel("2026-10-30T11:59:40Z", now))
        assertEquals("5min", elapsedLabel("2026-10-30T11:55:00Z", now))
        assertEquals("3h", elapsedLabel("2026-10-30T06:00:00-03:00", now))
        assertEquals("2d", elapsedLabel("2026-10-28T10:00:00.123456Z", now))
        assertEquals("1sem", elapsedLabel("2026-10-20T12:00:00Z", now))
    }

    @Test
    fun categoriesFollowTheReferenceLabels() {
        assertEquals(NotificationCategory.DEADLINE, NotificationCategory.of("SAVED_JOB_ALERT"))
        assertEquals(NotificationCategory.INTERVIEW, NotificationCategory.of("INTERVIEW_REMINDER"))
        assertEquals("NEW MATCH", NotificationCategory.of("NEW_MATCHES").label)
        assertEquals("JOB ALERT", NotificationCategory.of("SEARCH_COMPLETED").label)
        assertEquals(NotificationCategory.STATUS, NotificationCategory.of("STALE_APPLICATION"))
        assertEquals(NotificationCategory.SYSTEM, NotificationCategory.of("QUALQUER"))
        assertEquals(true, AppNotification(1, "SYSTEM", "x").unread)
    }
}
