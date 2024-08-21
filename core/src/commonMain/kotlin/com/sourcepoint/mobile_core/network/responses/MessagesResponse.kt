package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.utils.IntEnum
import com.sourcepoint.mobile_core.utils.IntEnumSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class MessagesResponse(
    val campaigns: List<Campaign>,
    val localState: String,
    val nonKeyedLocalState: String
) {
    @Serializable
    sealed class Campaign {
        abstract val type: String
        val url: String? = null
//        abstract val userConsent: Consent
        val message: Message? = null
        val messageMetaData: MessageMetaData? = null
        val dateCreated: String? = null
        val webConsentPayload: String? = null
    }

    @Serializable
    data class Message(
        val categories: List<JsonObject>?,
        val language: SPMessageLanguage?,
        @SerialName("message_json") val messageJson: JsonObject,
        @SerialName("message_choice") val messageChoices: List<JsonObject>,
        @SerialName("site_id") val propertyId: Int
    ) {
        @Serializable
        enum class GDPRCategory {
            @SerialName("IAB_PURPOSE") IABPurpose,
            @SerialName("IAB") IAB,
            @SerialName("CUSTOM") Custom,
        }
    }

    @Serializable
    @SerialName("GDPR")
    data class GDPR(
        override val type: String = "GDPR",
        @SerialName("TCData") val tcData: JsonObject
    ): Campaign()

    @Serializable
    @SerialName("usnat")
    data class USNat(
        override val type: String = "usnat",
        @SerialName("GPPData") val gppData: JsonObject
    ): Campaign()

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
