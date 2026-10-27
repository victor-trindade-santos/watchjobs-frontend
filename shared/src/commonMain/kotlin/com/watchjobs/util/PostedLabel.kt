package com.watchjobs.util

import com.watchjobs.model.JobPosting
import kotlin.time.Clock
import kotlin.time.Instant

// "Publicado há 2 dias". Sem a data da fonte, usa a da coleta e diz isso.
fun postedLabel(job: JobPosting): String? {
    val fromSource = job.postedAt != null
    val iso = job.postedAt ?: job.discoveredAt ?: return null
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return null
    val minutes = (Clock.System.now() - instant).inWholeMinutes.coerceAtLeast(0)
    val hours = minutes / 60
    val days = hours / 24
    val age = when {
        minutes < 60 -> "há poucos minutos"
        hours < 24 -> if (hours == 1L) "há 1 hora" else "há $hours horas"
        days == 1L -> "há 1 dia"
        else -> "há $days dias"
    }
    return (if (fromSource) "Publicado " else "Encontrada ") + age
}
