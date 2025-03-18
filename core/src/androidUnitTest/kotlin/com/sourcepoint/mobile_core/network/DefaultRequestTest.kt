package com.sourcepoint.mobile_core.network

import com.sourcepoint.core.BuildConfig
import com.sourcepoint.mobile_core.network.requests.DefaultRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultRequestTest {
    @Test
    fun containsTheRightAttributes() {
        val request = DefaultRequest()
        assertEquals("mobile-core-Android", request.scriptType)
        assertEquals(BuildConfig.Version, request.scriptVersion)
        assertEquals("prod", request.env)
    }
}
