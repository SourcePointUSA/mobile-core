package com.sourcepoint.mobile_core.models.consents

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertTrue

class PreferencesConsentTest {
    @Test
    fun targetingParamsWhenConsentNotPresent() {
        val state = State(propertyId = 1, accountId = 1)
        val targetingParams = state.preferences.consents.collectTargetingParams(state.preferences.metaData)
        assertTrue(targetingParams.containsKey("_sp_lt_AI-POLICY_na"))
        assertTrue(targetingParams.containsKey("_sp_lt_TERMS-AND-CONDITIONS_na"))
        assertTrue(targetingParams.containsKey("_sp_lt_PRIVACY-POLICY_na"))
        assertTrue(targetingParams.containsKey("_sp_lt_LEGAL-POLICY_na"))
        assertTrue(targetingParams.containsKey("_sp_lt_TERMS-OF-SALE_na"))
    }

    @Test
    fun targetingParamsForEachScenario() {
        val state = State(propertyId = 1, accountId = 1, preferences = State.PreferencesState(
            metaData = State.PreferencesState.PreferencesMetaData(legalDocLiveDate = mapOf(
                Pair(PreferencesConsent.PreferencesSubType.AIPolicy, Instant.DISTANT_PAST),
                Pair(PreferencesConsent.PreferencesSubType.TermsOfSale, Instant.DISTANT_FUTURE),
                Pair(PreferencesConsent.PreferencesSubType.LegalPolicy, Instant.DISTANT_PAST)
            )),
            consents = PreferencesConsent(
                status = listOf(
                    PreferencesConsent.PreferencesStatus(
                        categoryId = 0,
                        channels = emptyList(),
                        changed = true,
                        dateConsented = Clock.System.now(),
                        subType = PreferencesConsent.PreferencesSubType.AIPolicy
                    ),
                    PreferencesConsent.PreferencesStatus(
                        categoryId = 0,
                        channels = emptyList(),
                        changed = true,
                        dateConsented = Clock.System.now(),
                        subType = PreferencesConsent.PreferencesSubType.TermsOfSale
                    )),
                rejectedStatus = listOf(
                    PreferencesConsent.PreferencesStatus(
                        categoryId = 0,
                        channels = emptyList(),
                        changed = true,
                        dateConsented = Clock.System.now(),
                        subType = PreferencesConsent.PreferencesSubType.LegalPolicy
                    )
                )
            )
        ))
        val targetingParams = state.preferences.consents.collectTargetingParams(state.preferences.metaData)
        assertTrue(targetingParams.containsKey("_sp_lt_AI-POLICY_a"))
        assertTrue(targetingParams.containsKey("_sp_lt_TERMS-OF-SALE_od"))
        assertTrue(targetingParams.containsKey("_sp_lt_LEGAL-POLICY_r"))
        assertTrue(targetingParams.containsKey("_sp_lt_PRIVACY-POLICY_na"))
    }
}
