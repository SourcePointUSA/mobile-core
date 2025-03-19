package com.sourcepoint.mobile_core.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.math.pow
import kotlin.test.fail

fun runTestWithRetries(
    upToTimes: Int = 5,
    initialDelayMillis: Long = 300,
    backoffFactor: Double = 2.0,
    useExponentialBackoff: Boolean = true,
    block: suspend () -> Unit
) {
    runBlocking {
        var lastError: Throwable? = null

        repeat(upToTimes) { attempt ->
            try {
                runTest {
                    block()
                }
                return@runBlocking
            } catch (e: Throwable) {
                lastError = e
                println("Test failed on attempt ${attempt + 1}/$upToTimes: ${e.message}")

                if (attempt <= upToTimes) {
                    val delayTime = if (useExponentialBackoff) {
                        (initialDelayMillis * backoffFactor.pow(attempt + 1)).toLong()
                    } else {
                        initialDelayMillis
                    }
                    println("Delaying for ${delayTime}ms before retrying...")
                    delay(delayTime)
                }
            }
        }

        fail("Test failed after $upToTimes attempts. Last error: ${lastError?.message}")
    }
}
