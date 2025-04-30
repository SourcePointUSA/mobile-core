package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.models.consents.PreferencesConsent.PreferencesSubType
import com.sourcepoint.mobile_core.utils.now
import kotlinx.datetime.Instant

fun preferencesTargetingParams(
    preferencesState: State.PreferencesState
): Map<String, String> {
    val prefix = "_sp_lt_"
    val targetingParams = mutableMapOf<String, String>()

    val accepted = preferencesState.consents.status.orEmpty().associateBy { it.subType }
    val rejected = preferencesState.consents.rejectedStatus.orEmpty().associateBy { it.subType }

    preferencesState.metaData.legalDocLiveDate.orEmpty().forEach { (subType, liveDate) ->
        if (subType == PreferencesSubType.Unknown) return@forEach

        val acceptedSubType = accepted[subType]
        val rejectedSubType = rejected[subType]

        val keyBase = prefix + subType.value

        when {
            acceptedSubType != null -> {
                targetingParams["${keyBase}_a"] = "1"
                if (isOutdated(acceptedSubType.dateConsented, liveDate)) {
                    targetingParams["${keyBase}_od"] = "1"
                }
            }
            rejectedSubType != null -> {
                targetingParams["${keyBase}_r"] = "1"
                if (isOutdated(rejectedSubType.dateConsented, liveDate)) {
                    targetingParams["${keyBase}_od"] = "1"
                }
            }
            else -> {
                targetingParams["${keyBase}_na"] = "1"
            }
        }
    }

    return targetingParams
}

private fun isOutdated(dateConsented: Instant?, legalDocLiveDate: Instant?) =
    (dateConsented ?: now()) < (legalDocLiveDate ?: Instant.DISTANT_PAST)
