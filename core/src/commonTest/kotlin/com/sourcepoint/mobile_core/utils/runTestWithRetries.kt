package com.sourcepoint.mobile_core.utils

import kotlinx.coroutines.test.runTest
import kotlin.test.fail

fun runTestWithRetries(upToTimes: Int = 5, block: suspend () -> Unit) {
    var lastError: Throwable? = null

    repeat(upToTimes) { attempt ->
        try {
            runTest {
                block()
            }
            return
        } catch (e: Throwable) {
            lastError = e
            println("Test failed on attempt ${attempt + 1}/$upToTimes: ${e.message}")
        }
    }

    fail("Test failed after $upToTimes attempts. Last error: ${lastError?.message}")
}
