package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.models.consents.CCPAConsent.CCPAConsentStatus.*
import kotlin.test.Test
import kotlin.test.assertEquals

class CCPAConsentTest {
    @Test
    fun uspstringIs1YYYWhenAppliesIsTrueSignedLspaIsTrueAndStatusIsRejectedAllOrSome() {
        arrayOf(RejectedSome, RejectedAll).forEach { status ->
            val consent = CCPAConsent(applies = true, signedLspa = true, status = status)
            assertEquals("1YYY", consent.uspstring)
        }
    }

    @Test
    fun uspstringIs1YYNWhenAppliesIsTrueSignedLspaIsFalseAndStatusIsRejectedAllOrSome() {
        arrayOf(RejectedSome, RejectedAll).forEach { status ->
            val consent = CCPAConsent(applies = true, signedLspa = false, status = status)
            assertEquals("1YYN", consent.uspstring)
        }
    }

    @Test
    fun uspstringIs1YNYWhenAppliesIsTrueSignedLspaIsTrueAndStatusIsConsentedAll() {
        val consent = CCPAConsent(applies = true, signedLspa = true, status = ConsentedAll)
        assertEquals("1YNY", consent.uspstring)
    }

    @Test
    fun uspstringIs1YNNAppliesIsTrueSignedLspaIsFalseAndStatusIsConsentedAll() {
        val consent = CCPAConsent(applies = true, signedLspa = false, status = ConsentedAll)
        assertEquals("1YNN", consent.uspstring)
    }

    @Test
    fun uspstringIs1___WhenAppliesIsFalse() {
        val consent = CCPAConsent(applies = false)
        assertEquals("1---", consent.uspstring)
    }
}
