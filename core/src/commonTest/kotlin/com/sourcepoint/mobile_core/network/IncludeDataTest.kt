package com.sourcepoint.mobile_core.network

import com.sourcepoint.mobile_core.network.requests.IncludeData
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class IncludeDataTest {
    @Test
    fun defaultValues() {
        val includeData = IncludeData().toString()
        assertEquals(
            "{\"TCData\":{\"type\":\"string\"},\"webConsentPayload\":{\"type\":\"string\"},\"localState\":{\"type\":\"string\"},\"categories\":true,\"GPPData\":{\"uspString\":false}}",
            includeData
        )
    }

    @Test
    fun withTranslatedMessages() {
        val includeData = IncludeData(translateMessage = true).toString()
        assertContains(includeData, "\"translateMessage\":true")
    }
}
