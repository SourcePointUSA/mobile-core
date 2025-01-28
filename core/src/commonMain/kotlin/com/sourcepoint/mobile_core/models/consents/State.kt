package com.sourcepoint.mobile_core.models.consents

data class State (
    var gdpr: GDPRConsent?,
    var ccpa: CCPAConsent?,
    var usNat: USNatConsent?,
    var gdprMetaData: GDPRMetaData?,
    var ccpaMetaData: CCPAMetaData?,
    var usNatMetaData: UsNatMetaData?
) {
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
