package com.sourcepoint.mobile_core.models.consents
import com.sourcepoint.mobile_core.models.consents.PreferencesConsent.PreferencesSubType
import com.sourcepoint.mobile_core.utils.now
import kotlinx.datetime.Instant

fun collectTargetingParams(consent: PreferencesConsent, metaData: State.PreferencesState.PreferencesMetaData): Map<String, String> {
    var targetingParams = mapOf<String, String>()
    val notUsedParams: MutableList<PreferencesSubType> = enumValues<PreferencesSubType>().toMutableList()
    notUsedParams.removeAt(notUsedParams.indexOfFirst { it == PreferencesSubType.Unknown })
    val prefix = "_sp_lt_"
    consent.status?.map { param ->
        if (param.subType != PreferencesSubType.Unknown) {
            targetingParams = targetingParams.plus(
                Pair(
                    prefix + param.subType?.value + compareConsentDate(
                        dateConsented = param.dateConsented,
                        legalDocLiveDate = metaData.legalDocLiveDate?.get(param.subType),
                        targetEnd = "_a"),
                    "true"
                )
            )
            notUsedParams.removeAt(notUsedParams.indexOfFirst { it == param.subType })
        }
    }
    consent.rejectedStatus?.map { param ->
        if (param.subType != PreferencesSubType.Unknown) {
            targetingParams = targetingParams.plus(
                Pair(
                    prefix + param.subType?.value + compareConsentDate(
                        dateConsented = param.dateConsented,
                        legalDocLiveDate = metaData.legalDocLiveDate?.get(param.subType),
                        targetEnd = "_r"
                    ),
                    "true"
                )
            )
            notUsedParams.removeAt(notUsedParams.indexOfFirst { it == param.subType })
        }
    }
    notUsedParams.map {
        targetingParams = targetingParams.plus(
            Pair(
                prefix + it.value + "_na",
                "true"
            )
        )
    }
    return targetingParams
}

private fun compareConsentDate(dateConsented: Instant?, legalDocLiveDate: Instant?, targetEnd: String): String {
    if ((dateConsented ?: now()) < (legalDocLiveDate ?: Instant.DISTANT_PAST)) {
        return "_od"
    } else {
        return targetEnd
    }
}
