package com.sourcepoint.mobile_core.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class IncludeDataTest {
    @Test
    fun defaultValues() = runTest {
        val includeData = IncludeData().toString()
        assertEquals(
            "{\"TCData\":{\"type\":\"string\"},\"webConsentPayload\":{\"type\":\"string\"},\"localState\":{\"type\":\"string\"},\"categories\":true,\"GPPData\":{\"uspString\":true}}",
            includeData
        )
    }

    @Test
    fun withTranslatedMessages() = runTest {
        val includeData = IncludeData(translateMessage = true).toString()
        assertContains(includeData, "\"translateMessage\":true")
    }
}
