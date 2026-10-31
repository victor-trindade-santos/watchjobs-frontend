package com.watchjobs

import com.watchjobs.ui.analytics.barFraction
import com.watchjobs.ui.analytics.dayMonthLabel
import com.watchjobs.ui.analytics.labelStep
import com.watchjobs.ui.analytics.monthLabel
import com.watchjobs.ui.analytics.percentLabel
import com.watchjobs.ui.analytics.rateOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// F09: formatação dos gráficos e indicadores.
class AnalyticsFormatTest {
    @Test
    fun percentagesAndRates() {
        assertEquals("67%", percentLabel(0.667))
        assertEquals("0%", percentLabel(0.0))
        assertNull(percentLabel(null))
        assertNull(rateOf(3, 0))
        assertEquals(0.5, rateOf(1, 2))
    }

    @Test
    fun barsAreProportionalAndVisibleWhenNonZero() {
        assertEquals(0f, barFraction(0, 10))
        assertEquals(0f, barFraction(5, 0))
        assertEquals(1f, barFraction(10, 10))
        assertEquals(0.02f, barFraction(1, 1000))
    }

    @Test
    fun axisLabels() {
        assertEquals(1, labelStep(4))
        assertEquals(2, labelStep(12))
        assertEquals(7, labelStep(52))
        assertEquals("05/01", dayMonthLabel("2026-01-05"))
        assertEquals("jan/26", monthLabel("2026-01-01"))
        assertEquals("dez/25", monthLabel("2025-12-01"))
    }
}
