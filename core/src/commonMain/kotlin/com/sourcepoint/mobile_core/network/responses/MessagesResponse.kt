package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.models.consents.AttCampaign
import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.ConsentStrings
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.GlobalCmpConsent
import com.sourcepoint.mobile_core.models.consents.IABData
import com.sourcepoint.mobile_core.models.consents.PreferencesConsent
import com.sourcepoint.mobile_core.models.consents.SPGDPRVendorGrants
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import com.sourcepoint.mobile_core.network.responses.MessagesResponse.MessageMetaData.MessageCategory
import com.sourcepoint.mobile_core.network.responses.MessagesResponse.MessageMetaData.MessageSubCategory
import com.sourcepoint.mobile_core.utils.IntEnum
import com.sourcepoint.mobile_core.utils.IntEnumSerializer
import com.sourcepoint.mobile_core.utils.StringEnumWithDefaultSerializer
import com.sourcepoint.mobile_core.utils.inOneYear
import com.sourcepoint.mobile_core.utils.now
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json.Default.encodeToString
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

        abstract fun toConsent(default: ConsentClass?): ConsentClass?
    }

    @Serializable
    data class Message(
        val categories: List<GDPRCategory>?,
        val language: SPMessageLanguage?,
        @SerialName("message_json") val messageJson: JsonObject,
        @SerialName("message_choice") val messageChoices: List<JsonObject>,
        @SerialName("site_id") val propertyId: Int
    ) {
        fun encodeToJson(categoryId: MessageCategory, subCategoryId: MessageSubCategory) = encodeToString(
            serializer = MessageWithCategorySubCategory.serializer(),
            value = MessageWithCategorySubCategory(
                categoryId = categoryId,
                subCategoryId = subCategoryId,
                categories = categories,
                language = language,
                messageJson = messageJson,
                messageChoices = messageChoices,
                propertyId = propertyId
            )
        )

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
                        entries, Unknown
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
        private val dateCreated: Instant?,
        private val expirationDate: Instant?,
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
            dateCreated = dateCreated ?: now(),
            expirationDate = expirationDate ?: dateCreated?.inOneYear() ?: now().inOneYear(),
            tcData = tcData ?: emptyMap()
        ) else null
    ): Campaign<GDPRConsent?>() {
        override fun toConsent(default: GDPRConsent?): GDPRConsent? =
            if (derivedConsents != null) {
                default?.copy(
                    grants = derivedConsents.grants,
                    euconsent = derivedConsents.euconsent,
                    tcData = derivedConsents.tcData,
                    dateCreated = derivedConsents.dateCreated,
                    expirationDate = derivedConsents.expirationDate,
                    consentStatus = derivedConsents.consentStatus,
                    webConsentPayload = derivedConsents.webConsentPayload,
                    legIntCategories = derivedConsents.legIntCategories,
                    legIntVendors = derivedConsents.legIntVendors,
                    vendors = derivedConsents.vendors,
                    categories = derivedConsents.categories
                )
            } else {
                default?.copy()
            }
    }

    @Serializable
    @SerialName("globalcmp")
    data class GlobalCmp(
        override val type: SPCampaignType = SPCampaignType.GlobalCmp,
        private val consentStatus: ConsentStatus?,
        private val userConsents: USNatConsent.USNatUserConsents?,
        private val dateCreated: Instant?,
        private val expirationDate: Instant?,
        override val derivedConsents: GlobalCmpConsent? = if (
            userConsents != null &&
            consentStatus != null
        ) GlobalCmpConsent(
            dateCreated = dateCreated ?: now(),
            expirationDate = expirationDate ?: dateCreated?.inOneYear() ?: now().inOneYear(),
            consentStatus = consentStatus,
            userConsents = userConsents
        ) else null
    ): Campaign<GlobalCmpConsent?>() {
        override fun toConsent(default: GlobalCmpConsent?): GlobalCmpConsent? =
            if (derivedConsents != null){
                default?.copy(
                    dateCreated = derivedConsents.dateCreated,
                    expirationDate = derivedConsents.expirationDate,
                    userConsents = default.userConsents.copy(
                        categories = derivedConsents.userConsents.categories,
                        vendors = derivedConsents.userConsents.vendors
                    ),
                    consentStatus = derivedConsents.consentStatus,
                )
            } else {
                default?.copy()
            }
    }

    @Serializable
    @SerialName("usnat")
    data class USNat(
        override val type: SPCampaignType = SPCampaignType.UsNat,
        private val consentStatus: ConsentStatus?,
        private val consentStrings: ConsentStrings?,
        private val userConsents: USNatConsent.USNatUserConsents?,
        private val dateCreated: Instant?,
        private val expirationDate: Instant?,
        private val webConsentPayload: String?,
        @SerialName("GPPData") val gppData: IABData? = emptyMap(),
        override val derivedConsents: USNatConsent? = if (
            consentStrings != null &&
            userConsents != null &&
            consentStatus != null
        ) USNatConsent(
            dateCreated = dateCreated ?: now(),
            expirationDate = expirationDate ?: dateCreated?.inOneYear() ?: now().inOneYear(),
            consentStatus = consentStatus,
            consentStrings = consentStrings,
            userConsents = userConsents,
            webConsentPayload = webConsentPayload,
            gppData = gppData ?: emptyMap()
        ) else null
    ): Campaign<USNatConsent?>() {
        override fun toConsent(default: USNatConsent?): USNatConsent? =
            if (derivedConsents != null){
                default?.copy(
                    dateCreated = derivedConsents.dateCreated,
                    expirationDate = derivedConsents.expirationDate,
                    consentStrings = derivedConsents.consentStrings,
                    webConsentPayload = derivedConsents.webConsentPayload,
                    userConsents = default.userConsents.copy(
                        categories = derivedConsents.userConsents.categories,
                        vendors = derivedConsents.userConsents.vendors
                    ),
                    consentStatus = derivedConsents.consentStatus,
                    gppData = derivedConsents.gppData
                )
            } else {
                default?.copy()
            }
    }

    @Serializable
    @SerialName("CCPA")
    data class CCPA(
        override val type: SPCampaignType = SPCampaignType.Ccpa,
        val status: CCPAConsent.CCPAConsentStatus?,
        val signedLspa: Boolean?,
        val rejectedVendors: List<String>? = emptyList(),
        val rejectedCategories: List<String>? = emptyList(),
        val dateCreated: Instant?,
        val expirationDate: Instant?,
        val webConsentPayload: String?,
        @SerialName("GPPData") val gppData: IABData? = emptyMap(),
        override val derivedConsents: CCPAConsent? = if (
            rejectedVendors != null &&
            rejectedCategories != null &&
            signedLspa != null
        ) CCPAConsent(
            dateCreated = dateCreated ?: now(),
            expirationDate = expirationDate ?: dateCreated?.inOneYear() ?: now().inOneYear(),
            status = status,
            signedLspa = signedLspa,
            rejectedCategories = rejectedCategories,
            rejectedVendors = rejectedVendors,
            webConsentPayload = webConsentPayload,
            gppData = gppData ?: emptyMap()
        ) else null
    ): Campaign<CCPAConsent?>() {
        override fun toConsent(default: CCPAConsent?): CCPAConsent? =
            if (derivedConsents != null){
                default?.copy(
                    status = derivedConsents.status,
                    rejectedVendors = derivedConsents.rejectedVendors,
                    rejectedCategories = derivedConsents.rejectedCategories,
                    signedLspa = derivedConsents.signedLspa,
                    dateCreated = derivedConsents.dateCreated,
                    expirationDate = derivedConsents.expirationDate,
                    rejectedAll = derivedConsents.rejectedAll,
                    consentedAll = derivedConsents.consentedAll,
                    webConsentPayload = derivedConsents.webConsentPayload,
                    gppData = derivedConsents.gppData
                )
            } else {
                default?.copy()
            }
    }

    @Serializable
    @SerialName("ios14")
    data class Ios14(
        override val type: SPCampaignType = SPCampaignType.IOS14,
        override val derivedConsents: Nothing? = null
    ): Campaign<AttCampaign>() {
        override fun toConsent(default: AttCampaign?): AttCampaign? = null
    }

    @Serializable
    @SerialName("preferences")
    data class Preferences(
        override val type: SPCampaignType = SPCampaignType.Preferences,
        override val derivedConsents: Nothing? = null
    ): Campaign<PreferencesConsent>() {
        override fun toConsent(default: PreferencesConsent?): PreferencesConsent? = null
    }

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
            Preferences(7),
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

@Serializable
data class MessageWithCategorySubCategory(
    val categoryId: MessageCategory,
    val subCategoryId: MessageSubCategory,
    val categories: List<MessagesResponse.Message.GDPRCategory>?,
    val language: SPMessageLanguage?,
    @SerialName("message_json") val messageJson: JsonObject,
    @SerialName("message_choice") val messageChoices: List<JsonObject>,
    @SerialName("site_id") val propertyId: Int
)
