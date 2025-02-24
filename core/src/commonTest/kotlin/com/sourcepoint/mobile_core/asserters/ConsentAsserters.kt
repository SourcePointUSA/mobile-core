package com.sourcepoint.mobile_core.asserters

import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import kotlin.test.assertEquals
import kotlin.test.fail

fun assertAllAccepted(gdpr: GDPRConsent?, checkIABData: Boolean = true) {
    val prefix = "Expected gdpr consent to be all accepted, but "
    gdpr ?: { fail("$prefix it was null") }

    assertNotEmpty(gdpr?.euconsent, "$prefix euconsent is empty or null")
    assertNotEmpty(gdpr?.categories, "$prefix accepted categories is empty or null")
    assertTrue(
        gdpr?.grants?.all { (_, grant) -> grant.vendorGrant },
        "$prefix some vendors were not granted (${gdpr?.grants})"
    )
    assertEquals(
        gdpr?.grants?.keys,
        gdpr?.vendors?.toSet(),
        "$prefix not all vendors in vendor grants are in the list of accepted vendors"
    )
    if (checkIABData) {
        assertNotEmpty(gdpr?.tcData, "$prefix tcData is empty or null")
    }
}

fun assertAllRejected(gdpr: GDPRConsent?, checkIABData: Boolean = true) {
    val prefix = "Expected gdpr consent to be all rejected, but "
    gdpr ?: { fail("$prefix but it was null") }
    assertTrue(
        gdpr?.grants?.all { (_, grant) -> !grant.vendorGrant },
        "$prefix vendorGrants (${gdpr?.grants}) is null or some of its vendors are granted"
    )
    assertNotEmpty(gdpr?.euconsent, "$prefix euconsent is empty or null")
    assertIsEmpty(gdpr?.categories, "$prefix accepted categories is either null or has values")
    assertIsEmpty(gdpr?.vendors, "$prefix accepted vendors is either null or has values")
    if (checkIABData) assertNotEmpty(gdpr?.tcData, "$prefix tcData is empty or null")
}

fun assertDefaultConsents(gdpr: GDPRConsent?, checkIABData: Boolean = true) {
    val prefix = "Expected gdpr consent to have default values, but "
    gdpr ?: { fail("$prefix but it was null") }
    assertNotEmpty(gdpr?.grants?.keys, "$prefix vendor grants is empty or null")
    assertNotEmpty(gdpr?.euconsent, "$prefix euconsent is empty or null")
    if (checkIABData) assertNotEmpty(gdpr?.tcData, "$prefix tcData is empty or null")
}

fun assertAllAccepted(ccpa: CCPAConsent?, checkIABData: Boolean = true) {
    val prefix = "Expected ccpa consent to be all accepted, but "
    ccpa ?: { fail("$prefix but it was null") }
    assertIsEmpty(ccpa?.rejectedCategories, "$prefix rejectedCategories is either null or has values")
    assertIsEmpty(ccpa?.rejectedVendors, "$prefix rejectedVendors is either null or has values")

    assertFalse(ccpa?.rejectedAll, "$prefix rejectedAll is null or true")
    assertTrue(ccpa?.consentedAll, "$prefix consentedAll is null or false")
    assertEquals(
        CCPAConsent.CCPAConsentStatus.ConsentedAll,
        ccpa?.status,
        "$prefix its status (${ccpa?.status}) is not ConsentedAll"
    )
    if (checkIABData) assertNotEmpty(ccpa?.gppData, "$prefix gppData is empty or null")
}

fun assertDefaultConsents(ccpa: CCPAConsent?, checkIABData: Boolean = true) {
    val prefix = "Expected ccpa consent to have default values, but "
    ccpa ?: { fail("$prefix but it was null") }
    assertFalse(ccpa?.rejectedAll, "$prefix rejectedAll is null or true")
    assertFalse(ccpa?.consentedAll, "$prefix consentedAll is null or true")
    assertEquals(
        CCPAConsent.CCPAConsentStatus.RejectedNone,
        ccpa?.status,
        "$prefix its status (${ccpa?.status}) is not RejectedNone"
    )
    if (checkIABData) assertNotEmpty(ccpa?.gppData, "$prefix gppData is empty or null")
}

fun assertAllRejected(ccpa: CCPAConsent?, checkIABData: Boolean = true) {
    val prefix = "Expected ccpa consent to be all rejected, but "
    ccpa ?: { fail("$prefix but it was null") }
    assertNotEmpty(ccpa?.rejectedCategories, "$prefix rejectedCategories is null or empty")
    assertNotEmpty(ccpa?.rejectedVendors, "$prefix rejectedVendors is null or empty")
    assertTrue(ccpa?.rejectedAll, "$prefix rejectedAll is null or false")
    assertFalse(ccpa?.consentedAll, "$prefix consentedAll is null or true")
    assertEquals(
        CCPAConsent.CCPAConsentStatus.RejectedAll,
        ccpa?.status,
        "$prefix its status (${ccpa?.status}) is not RejectedAll"
    )
    if (checkIABData) assertNotEmpty(ccpa?.gppData, "$prefix gppData is empty or null")
}

fun assertAllAccepted(usnat: USNatConsent?, checkIABData: Boolean = true) {
    val prefix = "Expected usnat consent to be all accepted, but "
    usnat ?: { fail("$prefix but it was null") }
    assertNotEmpty(usnat?.consentStrings, "$prefix consentStrings is empty or null")
    assertTrue(
        usnat?.userConsents?.categories?.all { it.consented },
        "$prefix userConsents.categories is either null or some of it are not consented"
    )
    assertTrue(
        usnat?.userConsents?.vendors?.all { it.consented },
        "$prefix userConsents.vendors is either null or some of it are not consented"
    )
    if (checkIABData) assertNotEmpty(usnat?.gppData, "$prefix gppData is empty or null")
}

fun assertAllRejected(usnat: USNatConsent?, checkIABData: Boolean = true) {
    val prefix = "Expected usnat consent to be all rejected, but "
    usnat ?: { fail("$prefix but it was null") }
    assertNotEmpty(usnat?.consentStrings, "$prefix consentStrings is empty or null")
    assertTrue(
        usnat?.userConsents?.categories?.all { !it.consented },
        "$prefix userConsents.categories is either null or some of it are consented"
    )
    assertTrue(
        usnat?.userConsents?.vendors?.all { !it.consented },
        "$prefix userConsents.vendors is either null or some of it are consented"
    )
    if (checkIABData) assertNotEmpty(usnat?.gppData, "$prefix gppData is empty or null")
}

fun assertDefaultConsents(usnat: USNatConsent?, checkIABData: Boolean = true) {
    val prefix = "Expected usnat consent to default values, but "
    usnat ?: { fail("$prefix but it was null") }
    assertNotEmpty(usnat?.consentStrings, "$prefix consentStrings is empty or null")
    if (checkIABData) assertNotEmpty(usnat?.gppData, "$prefix gppData is empty or null")
}
