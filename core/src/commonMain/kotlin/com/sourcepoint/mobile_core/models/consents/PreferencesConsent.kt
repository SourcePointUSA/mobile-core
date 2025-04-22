package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.utils.now
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PreferencesConsent(
    val dateCreated: Instant? = null,
    val messageId: Int? = null,
    val status: List<PreferencesStatus>? = null,
    val rejectedStatus: List<PreferencesStatus>? = null,
    val uuid: String? = null
) {
    @Serializable
    data class PreferencesStatus(
        val categoryId: Int,
        val channels: List<PreferencesChannels>?,
        val changed: Boolean?,
        val dateConsented: Instant?,
        val subType: PreferencesSubType? = PreferencesSubType.Unknown
    ) {
        @Serializable
        data class PreferencesChannels(
            val channelId: Int,
            val status: Boolean
        )
    }

    @Serializable
    enum class PreferencesSubType(val value: String) {
        Unknown("Unknown"),
        @SerialName("AI-POLICY") AIPolicy("AI-POLICY"),
        @SerialName("TERMS-AND-CONDITIONS") TermsAndConditions("TERMS-AND-CONDITIONS"),
        @SerialName("PRIVACY-POLICY") PrivacyPolicy("PRIVACY-POLICY"),
        @SerialName("LEGAL-POLICY") LegalPolicy("LEGAL-POLICY"),
        @SerialName("TERMS-OF-SALE") TermsOfSale("TERMS-OF-SALE")
    }



    fun collectTargetingParams(metaData: State.PreferencesState.PreferencesMetaData): Map<String, String> {
        var targetingParams = mapOf<String, String>()
        val notUsedParams: MutableList<PreferencesSubType> = enumValues<PreferencesSubType>().toMutableList()
        notUsedParams.removeAt(notUsedParams.indexOfFirst { it == PreferencesSubType.Unknown })
        val prefix = "_sp_lt_"
        status?.map { param ->
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
        rejectedStatus?.map { param ->
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
}
private fun compareConsentDate(dateConsented: Instant?, legalDocLiveDate: Instant?, targetEnd: String): String {
    if ((dateConsented ?: now()) < (legalDocLiveDate ?: Instant.DISTANT_PAST)) {
        return "_od"
    } else {
        return targetEnd
    }
}

