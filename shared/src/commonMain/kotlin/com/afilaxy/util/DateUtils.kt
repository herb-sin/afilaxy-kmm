package com.afilaxy.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Returns the last N date keys in "yyyy-MM-dd" UTC format, matching the Cloud Function's
 * dailyCount field: `dateKey = new Date(ts).toISOString().slice(0, 10)`.
 * Index 0 = today, index N-1 = N-1 days ago.
 */
fun last7DayUtcKeys(n: Int = 7): List<String> {
    val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
    return (0 until n).map { daysAgo -> today.minus(daysAgo, DateTimeUnit.DAY).toString() }
}

/**
 * Sums the rolling-N-day count from a dailyCount map.
 * Returns null when dailyCount map is absent (caller should fall back to weeklyCount).
 */
fun sumRollingDays(dailyMap: Map<String, Any?>?, keys: List<String>): Int? {
    if (dailyMap == null) return null
    return keys.sumOf { dateKey ->
        when (val v = dailyMap[dateKey]) {
            is Long   -> v.toInt()
            is Double -> v.toInt()
            is Number -> v.toInt()
            else      -> 0
        }
    }
}
