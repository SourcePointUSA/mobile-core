package com.sourcepoint.mobile_core.models.consents

data class State (
    val gdpr: GDPRConsent?,
    val ccpa: CCPAConsent?,
    val usNat: USNatConsent?,
    var gdprMetaData: GDPRMetaData?,
    var ccpaMetaData: CCPAMetaData?,
    var usNatMetaData: UsNatMetaData?
) {
    data class GDPRMetaData (
        var additionsChangeDate: String,
        var legalBasisChangeDate: String?,
        var sampleRate: Float = 1f,
        var wasSampled: Boolean?,
        var wasSampledAt: Float?,
        var vendorListId: String?
    )

    data class CCPAMetaData (
        var sampleRate: Float = 1f,
        var wasSampled: Boolean?,
        var wasSampledAt: Float?
    )

    data class UsNatMetaData (
        var additionsChangeDate: String,
        var sampleRate:Float = 1f,
        var wasSampled: Boolean?,
        var wasSampledAt: Float?,
        var vendorListId: String?,
        var applicableSections: List<Int> = emptyList()
    )
}
