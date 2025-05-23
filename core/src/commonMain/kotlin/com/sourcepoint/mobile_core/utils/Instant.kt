package com.sourcepoint.mobile_core.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

fun now(): Instant = Clock.System.now()

fun yesterday(): Instant = Clock.System.now().minus(1.days)

fun Instant.inOneYear() = plus(365.days)

fun Instant.instantToString() = this.toString()

fun String.toInstant() = Instant.parse(this)
