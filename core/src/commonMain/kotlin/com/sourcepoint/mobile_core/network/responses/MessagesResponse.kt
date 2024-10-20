package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.ConsentStrings
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.IABData
import com.sourcepoint.mobile_core.models.consents.SPGDPRVendorGrants
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import com.sourcepoint.mobile_core.utils.IntEnum
import com.sourcepoint.mobile_core.utils.IntEnumSerializer
import com.sourcepoint.mobile_core.utils.StringEnumWithDefaultSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class MessagesResponse(
    val campaigns: List<Campaign<@Contextual Any>>,
    val localState: String,
    val nonKeyedLocalState: String
) {
    @Serializable
    sealed class Campaign<ConsentClass> {
        abstract val type: SPCampaignType
        abstract val derivedConsents: ConsentClass?
        val url: String? = null
        val message: Message? = null
        val messageMetaData: MessageMetaData? = null
        val childPmId: String? = null
    }

    @Serializable
    data class Message(
        val categories: List<GDPRCategory>?,
        val language: SPMessageLanguage?,
        @SerialName("message_json") val messageJson: JsonObject,
        @SerialName("message_choice") val messageChoices: List<JsonObject>,
        @SerialName("site_id") val propertyId: Int
    ) {
        @Serializable
        data class GDPRCategory(
            val iabId: Int?,
            @SerialName("_id") val id: String,
            val name: String,
            val description: String,
            val friendlyDescription: String?,
            val type: CategoryType?,
            val disclosureOnly: Boolean?,
            val requireConsent: Boolean?,
            val legIntVendors: List<Vendor>?,
            val requiringConsentVendors: List<Vendor>?,
            val disclosureOnlyVendors: List<Vendor>?,
            val vendors: List<Vendor>?
        ) {
            @Serializable(with = CategoryType.Serializer::class)
            enum class CategoryType {
                IAB_PURPOSE,
                IAB,
                Unknown,
                CUSTOM;

                object Serializer: StringEnumWithDefaultSerializer<CategoryType>(entries, Unknown)
            }

            @Serializable
            data class Vendor(
                val name: String,
                val vendorId: String?,
                val policyUrl: String?,
                val vendorType: VendorType?
            ) {
                @Serializable(with = VendorType.Serializer::class)
                enum class VendorType {
                    IAB,
                    CUSTOM,
                    Unknown;

                    object Serializer: StringEnumWithDefaultSerializer<VendorType>(
                        VendorType.entries, Unknown
                    )
                }
            }
        }
    }

    @Serializable
    @SerialName("GDPR")
    data class GDPR(
        override val type: SPCampaignType = SPCampaignType.Gdpr,
        private val euconsent: String?,
        private val grants: SPGDPRVendorGrants?,
        private val consentStatus: ConsentStatus?,
        private val dateCreated: String?,
        private val expirationDate: String?,
        private val webConsentPayload: String?,
        @SerialName("TCData") val tcData: IABData? = emptyMap(),
        override val derivedConsents: GDPRConsent? = if (
            euconsent != null &&
            grants != null &&
            consentStatus != null
        ) GDPRConsent(
            euconsent = euconsent,
            grants = grants,
            consentStatus = consentStatus,
            webConsentPayload = webConsentPayload,
            expirationDate = expirationDate,
            dateCreated = dateCreated,
            tcData = tcData ?: emptyMap()
        ) else null
    ): Campaign<GDPRConsent>()

    @Serializable
    @SerialName("usnat")
    data class USNat(
        override val type: SPCampaignType = SPCampaignType.UsNat,
        private val consentStatus: ConsentStatus?,
        private val consentStrings: ConsentStrings?,
        private val userConsents: USNatConsent.USNatUserConsents?,
        private val dateCreated: String?,
        private val expirationDate: String?,
        private val webConsentPayload: String?,
        @SerialName("GPPData") val gppData: IABData? = emptyMap(),
        override val derivedConsents: USNatConsent? = if (
            consentStrings != null &&
            userConsents != null &&
            consentStatus != null
        ) USNatConsent(
            dateCreated = dateCreated,
            expirationDate = expirationDate,
            consentStatus = consentStatus,
            consentStrings = consentStrings,
            userConsents = userConsents,
            webConsentPayload = webConsentPayload,
            gppData = gppData ?: emptyMap()
        ) else null
    ): Campaign<USNatConsent>()

    @Serializable
    @SerialName("CCPA")
    data class CCPA(
        override val type: SPCampaignType = SPCampaignType.Ccpa,
        val status: CCPAConsent.CCPAConsentStatus,
        val signedLspa: Boolean?,
        val rejectedVendors: List<String>? = emptyList(),
        val rejectedCategories: List<String>? = emptyList(),
        val dateCreated: String?,
        val expirationDate: String?,
        val webConsentPayload: String?,
        @SerialName("GPPData") val gppData: IABData? = emptyMap(),
        override val derivedConsents: CCPAConsent? = if (
            rejectedVendors != null &&
            rejectedCategories != null &&
            signedLspa != null
        ) CCPAConsent(
            dateCreated = dateCreated,
            expirationDate = expirationDate,
            signedLspa = signedLspa,
            rejectedCategories = rejectedCategories,
            rejectedVendors = rejectedVendors,
            webConsentPayload = webConsentPayload,
            gppData = gppData ?: emptyMap()
        ) else null
    ): Campaign<CCPAConsent>()

    @Serializable
    data class MessageMetaData(
        val categoryId: MessageCategory,
        val subCategoryId: MessageSubCategory,
        val messageId: Int,
        val messagePartitionUUID: String?
    ) {
        @Serializable(with = MessageCategory.Serializer::class)
        enum class MessageCategory(override val rawValue: Int): IntEnum {
            Gdpr(1),
            Ccpa(2),
            AdBlock(3),
            IOS14(4),
            Custom(rawValue = 5),
            UsNat(6),
            Unknown(0);

            object Serializer : IntEnumSerializer<MessageCategory>(entries, default = Unknown)
        }

        @Serializable(with = MessageSubCategory.Serializer::class)
        enum class MessageSubCategory(override val rawValue: Int) : IntEnum {
            Notice(1),
            PrivacyManager(2),
            SubjectAccessRequest(3),
            DSAR(4),
            NoticeTCFV2(5),
            NoticeNative(6),
            PrivacyManagerOTT(7),
            NoticeNonIAB(8),
            PrivacyManagerNonIAB(9),
            IOS(10),
            CCPAOTT(11),
            PrivacyManagerCCPA(12),
            Custom(13),
            NativeOTT(14),
            Unknown(0);

            object Serializer : IntEnumSerializer<MessageSubCategory>(entries, Unknown)
        }
    }
}
