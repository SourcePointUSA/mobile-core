package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.GlobalCmpConsent
import com.sourcepoint.mobile_core.models.consents.PreferencesConsent
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import kotlinx.serialization.Serializable

@Serializable
data class ConsentStatusResponse (
    val consentStatusData: ConsentStatusData,
    val localState: String
) {
    @Serializable
    data class ConsentStatusData (
        val gdpr: GDPRConsent? = null,
        val usnat: USNatConsent? = null,
        val ccpa: CCPAConsent? = null,
        val preferences: PreferencesConsent? = null,
        val globalcmp: GlobalCmpConsent? = null
    )
}
