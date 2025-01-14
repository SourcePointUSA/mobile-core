package com.sourcepoint.mobile_core.models.consents

data class State (
    var gdpr: GDPRConsent?,
    var ccpa: CCPAConsent?,
    var usNat: USNatConsent?,
    val gdprMetaData: GDPRMetaData?,
    val ccpaMetaData: CCPAMetaData?,
    val usNatMetaData: UsNatMetaData?
) {
    data class GDPRMetaData (
        val additionsChangeDate: String,
        val legalBasisChangeDate: String?,
        val sampleRate: Float = 1f,
        val wasSampled: Boolean?,
        val wasSampledAt: Float?,
        val vendorListId: String?
    )

    data class CCPAMetaData (
        val sampleRate: Float = 1f,
        val wasSampled: Boolean?,
        val wasSampledAt: Float?
    )

    data class UsNatMetaData (
        val additionsChangeDate: String,
        val sampleRate:Float = 1f,
        val wasSampled: Boolean?,
        val wasSampledAt: Float?,
        val vendorListId: String?,
        val applicableSections: List<Int> = emptyList()
    )
}
