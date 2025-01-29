package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.models.SPJson

data class State (
    var gdpr: GDPRConsent?,
    var ccpa: CCPAConsent?,
    var usNat: USNatConsent?,
    var ios14: AttCampaign?,
    var gdprMetaData: GDPRMetaData?,
    var ccpaMetaData: CCPAMetaData?,
    var usNatMetaData: UsNatMetaData?,
    var localState: SPJson?,
    var nonKeyedLocalState: SPJson?
) {
    val hasGDPRLocalData: Boolean get() = gdpr?.uuid != null
    val hasCCPALocalData: Boolean get() = ccpa?.uuid != null
    val hasUSNatLocalData: Boolean get() = usNat?.uuid != null

    data class GDPRMetaData (
        val additionsChangeDate: String,
        val legalBasisChangeDate: String?,
        override val sampleRate: Float = 1f,
        override val wasSampled: Boolean?,
        override val wasSampledAt: Float?,
        val vendorListId: String?
    ): SPSampleable

    data class CCPAMetaData (
        override val sampleRate: Float = 1f,
        override val wasSampled: Boolean?,
        override val wasSampledAt: Float?
    ): SPSampleable

    data class UsNatMetaData (
        val additionsChangeDate: String,
        override val sampleRate:Float = 1f,
        override val wasSampled: Boolean?,
        override val wasSampledAt: Float?,
        val vendorListId: String?,
        val applicableSections: List<Int> = emptyList()
    ): SPSampleable

    interface SPSampleable {
        val sampleRate: Float
        val wasSampled: Boolean?
        val wasSampledAt: Float?
    }
}
