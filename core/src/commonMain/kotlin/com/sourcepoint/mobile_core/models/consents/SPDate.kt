package com.sourcepoint.mobile_core.models.consents

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant.Companion.DISTANT_FUTURE
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

class SPDate {
    var date: LocalDateTime
    var originalDateString: String?
    private val formatter = LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
        char('T')
        time(LocalTime.Formats.ISO)
        char('Z')
    }

    constructor(date: LocalDateTime) {
        this.date = date
        originalDateString = date.toString()
    }

    constructor(dateString: String?) {
        originalDateString = dateString
        date = try {
            formatter.parse(dateString ?: "")
        } catch (error: IllegalArgumentException) {
            now().date
        }
    }

    companion object {
        fun now(): SPDate {
            val tz = TimeZone.currentSystemDefault()
            val now = Clock.System.now().toLocalDateTime(tz)
            return SPDate(now)
        }

        fun distantFuture(): SPDate = SPDate(DISTANT_FUTURE.toLocalDateTime(TimeZone.UTC))
    }
}
