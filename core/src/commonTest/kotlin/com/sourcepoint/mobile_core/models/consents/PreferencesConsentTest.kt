package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.utils.now
import com.sourcepoint.mobile_core.utils.yesterday
import kotlin.test.Test
import kotlin.test.assertEquals

class PreferencesConsentTest {
    private val subType = PreferencesConsent.PreferencesSubType.AIPolicy

    @Test
    fun testUpToDateAcceptedConsent() {
        val date = now()
        val preferencesState = State.PreferencesState(
            consents = PreferencesConsent(
                status = listOf(
                    PreferencesConsent.PreferencesStatus(
                        categoryId = 0,
                        subType = subType,
                        dateConsented = date
                    )
                )
            ),
            metaData = State.PreferencesState.PreferencesMetaData(
                legalDocLiveDate = mapOf(subType to date)
            )
        )
        assertEquals(
            mapOf("_sp_lt_${subType.value}_a" to "1"),
            preferencesTargetingParams(preferencesState)
        )
    }

    @Test
    fun testUpToDateRejectedConsent() {
        val date = now()
        val preferencesState = State.PreferencesState(
            consents = PreferencesConsent(
                rejectedStatus = listOf(
                    PreferencesConsent.PreferencesStatus(
                        categoryId = 0,
                        subType = subType,
                        dateConsented = date
                    )
                )
            ),
            metaData = State.PreferencesState.PreferencesMetaData(
                legalDocLiveDate = mapOf(subType to date)
            )
        )

        assertEquals(
            mapOf("_sp_lt_${subType.value}_r" to "1"),
            preferencesTargetingParams(preferencesState)
        )
    }

    @Test
    fun testNoAction() {
        val preferencesState = State.PreferencesState(
            metaData = State.PreferencesState.PreferencesMetaData(
                legalDocLiveDate = mapOf(subType to now())
            )
        )
        assertEquals(
            mapOf("_sp_lt_${subType.value}_na" to "1"),
            preferencesTargetingParams(preferencesState)
        )
    }

    @Test
    fun testOutdatedAcceptedConsent() {
        val preferencesState = State.PreferencesState(
            consents = PreferencesConsent(
                status = listOf(
                    PreferencesConsent.PreferencesStatus(
                        categoryId = 0,
                        subType = subType,
                        dateConsented = yesterday()
                    )
                )
            ),
            metaData = State.PreferencesState.PreferencesMetaData(
                legalDocLiveDate = mapOf(subType to now())
            )
        )
        assertEquals(
            mapOf(
                "_sp_lt_${subType.value}_a" to "1",
                "_sp_lt_${subType.value}_od" to "1"
            ),
            preferencesTargetingParams(preferencesState)
        )
    }

    @Test
    fun testOutdatedRejectedStatus() {
        val preferencesState = State.PreferencesState(
            consents = PreferencesConsent(
                rejectedStatus = listOf(
                    PreferencesConsent.PreferencesStatus(
                        categoryId = 0,
                        subType = subType,
                        dateConsented = yesterday()
                    )
                )
            ),
            metaData = State.PreferencesState.PreferencesMetaData(
                legalDocLiveDate = mapOf(subType to now())
            )
        )

        assertEquals(
            mapOf(
                "_sp_lt_${subType.value}_r" to "1",
                "_sp_lt_${subType.value}_od" to "1"
            ),
            preferencesTargetingParams(preferencesState)
        )
    }
}
