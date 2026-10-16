package com.watchjobs

import com.watchjobs.data.repository.encodeQuery
import com.watchjobs.util.parseBrDate
import com.watchjobs.util.parseBrDateTime
import com.watchjobs.util.parseMoney
import com.watchjobs.util.salaryRange
import com.watchjobs.util.toBr
import com.watchjobs.util.toInput
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormattersTest {
    @Test
    fun parsesBrazilianDates() {
        assertEquals(LocalDate(1995, 5, 10), parseBrDate("10/05/1995"))
        assertEquals(LocalDate(1995, 5, 10), parseBrDate(" 10/5/1995 "))
        assertNull(parseBrDate("31/02/2026"))
        assertNull(parseBrDate("1995-05-10"))
        assertNull(parseBrDate(""))
        assertEquals("10/05/1995", LocalDate(1995, 5, 10).toBr())
    }

    @Test
    fun parsesBrazilianDateTimes() {
        assertEquals(LocalDateTime(2026, 10, 20, 14, 30), parseBrDateTime("20/10/2026 14:30"))
        assertNull(parseBrDateTime("20/10/2026 25:00"))
        assertNull(parseBrDateTime("20/10/2026"))
    }

    @Test
    fun parsesAndFormatsMoney() {
        assertEquals(8000.0, parseMoney("8000"))
        assertEquals(8000.0, parseMoney("8.000"))
        assertEquals(8000.5, parseMoney("8.000,50"))
        assertEquals(8000.5, parseMoney("8000,5"))
        assertEquals(1234.56, parseMoney("1234.56"))
        assertNull(parseMoney("abc"))
        assertNull(parseMoney(""))
        assertEquals("8000,50", 8000.5.toInput())
    @Test
    fun parsesAndFormatsMoney() {
        assertEquals(8000.0, parseMoney("8000"))
        assertEquals(8000.0, parseMoney("8.000"))
        assertEquals(8000.5, parseMoney("8.000,50"))
        assertEquals(8000.5, parseMoney("8000,5"))
        assertEquals(1234.56, parseMoney("1234.56"))
        assertNull(parseMoney("abc"))
        assertNull(parseMoney(""))
        assertEquals("8000,50", 8000.5.toInput())
