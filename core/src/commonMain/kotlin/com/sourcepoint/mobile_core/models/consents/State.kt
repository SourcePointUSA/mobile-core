package com.sourcepoint.mobile_core.models.consents

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class State (
    var gdpr: GDPRState = GDPRState(),
    var ccpa: CCPAState = CCPAState(),
    var usNat: USNatState = USNatState(),
    var ios14: AttCampaign = AttCampaign(),
    var globalcmp: GlobalCmpState = GlobalCmpState(),
    var preferences: PreferencesState = PreferencesState(),
    var authId: String? = null,
    val propertyId: Int,
    val accountId: Int,
    var localVersion: Int = VERSION,
    var localState: String = "",
    var nonKeyedLocalState: String = "",
) {
    companion object {
        const val VERSION = 1
    }

    val hasGDPRLocalData: Boolean get () = gdpr.consents.uuid != null
    val hasCCPALocalData: Boolean get () = ccpa.consents.uuid != null
    val hasUSNatLocalData: Boolean get () = usNat.consents.uuid != null
    val hasPreferencesLocalData: Boolean get () = preferences.consents.uuid != null
    val hasGlobalCmpLocalData: Boolean get () = globalcmp.consents.uuid != null

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
            val additionsChangeDate: Instant = Instant.DISTANT_PAST,
            val legalBasisChangeDate: Instant = Instant.DISTANT_PAST,
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
            val additionsChangeDate: Instant = Instant.DISTANT_PAST,
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

    @Serializable
    data class GlobalCmpState(
        val metaData: GlobalCmpMetaData = GlobalCmpMetaData(),
        val consents: GlobalCmpConsent = GlobalCmpConsent(),
        val childPmId: String? = null
    ) {
        @Serializable
        data class GlobalCmpMetaData (
            val additionsChangeDate: Instant = Instant.DISTANT_PAST,
            override var sampleRate:Float = 1f,
            override var wasSampled: Boolean? = null,
            override var wasSampledAt: Float? = null,
            val vendorListId: String? = null,
            val applicableSections: List<Int> = emptyList()
        ): SPSampleable

        fun resetStateIfVendorListChanges(newVendorListId: String): GlobalCmpState =
            if (metaData.vendorListId != null && metaData.vendorListId != newVendorListId) {
                copy(consents = GlobalCmpConsent())
            } else {
                this
            }
    }

    @Serializable
    data class PreferencesState(
        val metaData: PreferencesMetaData = PreferencesMetaData(),
        val consents: PreferencesConsent = PreferencesConsent(),
    ) {
        @Serializable
        data class PreferencesMetaData(
            val configurationId: String = "",
            val additionsChangeDate: Instant = Instant.DISTANT_PAST,
            val legalDocLiveDate: Map<PreferencesConsent.PreferencesSubType, Instant>? = null
        )
    }

    private fun expireStateBasedOnExpiryDates() {
        val now = Clock.System.now()
        gdpr.consents.expirationDate.let { if(it < now) gdpr = GDPRState() }
        ccpa.consents.expirationDate.let { if(it < now) ccpa = CCPAState() }
        usNat.consents.expirationDate.let { if(it < now) usNat = USNatState() }
        globalcmp.consents.expirationDate.let { if(it < now) globalcmp = GlobalCmpState() }
    }

    fun updateGDPRStatusForVendorListChanges() {
        var newConsentStatus = gdpr.consents.consentStatus.copy()
        if (gdpr.consents.dateCreated < gdpr.metaData.additionsChangeDate) {
            newConsentStatus = newConsentStatus.copy(vendorListAdditions = true)
        }
        if (gdpr.consents.dateCreated < gdpr.metaData.legalBasisChangeDate) {
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
        if (usNat.consents.dateCreated < usNat.metaData.additionsChangeDate) {
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

    fun updateGlobaCMPStatusForVendorListChanges() {
        var newConsentStatus = globalcmp.consents.consentStatus
        if (globalcmp.consents.dateCreated < globalcmp.metaData.additionsChangeDate) {
            newConsentStatus = newConsentStatus.copy(vendorListAdditions = true)
            if (globalcmp.consents.consentStatus.consentedAll == true) {
                newConsentStatus = newConsentStatus.copy(
                    consentedAll = false,
                    granularStatus = newConsentStatus.granularStatus?.copy(previousOptInAll = true)
                )
            }
        }
        globalcmp.consents.consentStatus = newConsentStatus
    }
}
