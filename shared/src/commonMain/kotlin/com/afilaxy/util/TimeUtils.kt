package com.afilaxy.util

import kotlinx.datetime.Clock

object TimeUtils {
    fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
}