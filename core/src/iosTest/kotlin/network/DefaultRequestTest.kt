package network

import com.sourcepoint.core.BuildConfig
import com.sourcepoint.mobile_core.network.requests.DefaultRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultRequestTest {
    @Test
    fun containsTheRightAttributes() = runTest {
        val request = DefaultRequest()
        assertEquals("mobile-core-iOS", request.scriptType)
        assertEquals(BuildConfig.Version, request.scriptVersion)
        assertEquals("prod", request.env)
    }
}
