package com.watchjobs.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Tempo desde o aviso, curto como no menu do sininho: "agora", "5min", "3h", "2d", "1sem".
@OptIn(ExperimentalTime::class)
fun elapsedLabel(createdAt: String, now: Instant = Clock.System.now()): String {
    val created = runCatching { Instant.parse(createdAt) }.getOrNull() ?: return createdAt.take(10)
    val minutes = (now - created).inWholeMinutes.coerceAtLeast(0)
    return when {
        minutes < 1 -> "agora"
        minutes < 60 -> "${minutes}min"
        minutes < 60 * 24 -> "${minutes / 60}h"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d"
        else -> "${minutes / (60 * 24 * 7)}sem"
    }
}
