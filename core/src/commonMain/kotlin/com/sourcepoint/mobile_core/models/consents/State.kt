package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.models.SPJson
import kotlinx.serialization.Serializable

@Serializable
data class State (
    var gdpr: GDPRState = GDPRState(),
    var ccpa: CCPAState = CCPAState(),
    var usNat: USNatState = USNatState(),
    var ios14: AttCampaign = AttCampaign(),
    var authId: String? = null,
    var localVersion: Int = VERSION,
    var localState: SPJson? = null,
    var nonKeyedLocalState: SPJson? = null,
    var storedAuthId: String? = null,
) {
    companion object {
        const val VERSION = 1
    }

    val hasGDPRLocalData: Boolean get () = gdpr.consents.uuid != null
    val hasCCPALocalData: Boolean get () = ccpa.consents.uuid != null
    val hasUSNatLocalData: Boolean get () = usNat.consents.uuid != null

    interface SPSampleable {
        var sampleRate: Float
        var wasSampled: Boolean?
        var wasSampledAt: Float?

        fun updateSampleFields(newSampleRate: Float) {
            sampleRate = newSampleRate
            if (sampleRate != wasSampledAt) {
                wasSampledAt = sampleRate
                wasSampled = null
            }
        }
    }

    init {
        expireStateBasedOnExpiryDates()
    }

    @Serializable
    data class GDPRState(
        val metaData: GDPRMetaData = GDPRMetaData(),
        val consents: GDPRConsent = GDPRConsent(),
        val childPmId: String? = null
    ) {
        @Serializable
        data class GDPRMetaData (
            val additionsChangeDate: String = SPDate.now().toString(),
            val legalBasisChangeDate: String? = null,
            override var sampleRate: Float = 1f,
            override var wasSampled: Boolean? = null,
            override var wasSampledAt: Float? = null,
            val vendorListId: String? = null
        ): SPSampleable

        fun resetStateIfVendorListChanges(newVendorListId: String): GDPRState =
            if (metaData.vendorListId != null && metaData.vendorListId != newVendorListId) {
                copy(consents = GDPRConsent())
            } else {
                this
            }
    }

    @Serializable
    data class CCPAState(
        val metaData: CCPAMetaData = CCPAMetaData(),
        val consents: CCPAConsent = CCPAConsent(),
        val childPmId: String? = null
    ) {
        @Serializable
        data class CCPAMetaData (
            override var sampleRate: Float = 1f,
            override var wasSampled: Boolean? = null,
            override var wasSampledAt: Float? = null
        ): SPSampleable
    }

    @Serializable
    data class USNatState(
        val metaData: UsNatMetaData = UsNatMetaData(),
        val consents: USNatConsent = USNatConsent(),
        val childPmId: String? = null
    ) {
        @Serializable
        data class UsNatMetaData (
            val additionsChangeDate: String = SPDate.now().toString(),
            override var sampleRate:Float = 1f,
            override var wasSampled: Boolean? = null,
            override var wasSampledAt: Float? = null,
            val vendorListId: String? = null,
            val applicableSections: List<Int> = emptyList()
        ): SPSampleable

        fun resetStateIfVendorListChanges(newVendorListId: String): USNatState =
            if (metaData.vendorListId != null && metaData.vendorListId != newVendorListId) {
                copy(consents = USNatConsent())
            } else {
                this
            }
    }

    private fun expireStateBasedOnExpiryDates() {
        val now = SPDate.now().date
        if(gdpr.consents.expirationDate != null && SPDate(gdpr.consents.expirationDate).date < now) {
            gdpr = GDPRState()
        }
        if(ccpa.consents.expirationDate != null && SPDate(ccpa.consents.expirationDate).date < now) {
            ccpa = CCPAState()
        }
        if(usNat.consents.expirationDate != null && SPDate(usNat.consents.expirationDate).date < now) {
            usNat = USNatState()
        }
    }

    fun updateGDPRStatusForVendorListChanges() {
        var newConsentStatus = gdpr.consents.consentStatus.copy()
        if (SPDate(gdpr.consents.dateCreated).date < SPDate(gdpr.metaData.additionsChangeDate).date) {
            newConsentStatus = newConsentStatus.copy(vendorListAdditions = true)
        }
        if (SPDate(gdpr.consents.dateCreated).date < SPDate(gdpr.metaData.legalBasisChangeDate).date) {
            newConsentStatus = newConsentStatus.copy(legalBasisChanges = true)
        }
        if (newConsentStatus.consentedAll == true &&
            (newConsentStatus.vendorListAdditions == true || newConsentStatus.legalBasisChanges == true)
            ) {
            newConsentStatus = newConsentStatus.copy(
                consentedAll = false,
                granularStatus = newConsentStatus.granularStatus?.copy(previousOptInAll = true)
            )
        }

        gdpr = gdpr.copy(consents = gdpr.consents.copy(consentStatus = newConsentStatus))
    }

    fun updateUSNatStatusForVendorListChanges() {
        var newConsentStatus = usNat.consents.consentStatus
        if (SPDate(usNat.consents.dateCreated).date < SPDate(usNat.metaData.additionsChangeDate).date) {
            newConsentStatus = newConsentStatus.copy(vendorListAdditions = true)
            if (usNat.consents.consentStatus.consentedAll == true) {
                newConsentStatus = newConsentStatus.copy(
                    consentedAll = false,
                    granularStatus = newConsentStatus.granularStatus?.copy(previousOptInAll = true)
                )
            }
        }
        usNat.consents.consentStatus = newConsentStatus
    }
}
