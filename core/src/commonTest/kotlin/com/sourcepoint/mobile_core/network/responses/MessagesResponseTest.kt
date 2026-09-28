package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.network.json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json.Default.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class MessagesResponseTest {
    @Test
    fun decodeMessageMetaDataUsesPrtnUuidWireName() {
        val metadata = json.decodeFromString<MessagesResponse.MessageMetaData>(
            """
            {
              "categoryId": 1,
              "subCategoryId": 5,
              "messageId": 1429939,
              "messagePartitionUUID": null,
              "prtnUUID": "partition-uuid"
            }
            """.trimIndent()
        )

        assertEquals("partition-uuid", metadata.messagePartitionUUID)
    }

    @Test
    fun encodeToJsonTest() {
        val message = MessagesResponse.Message(
            categories = null,
            language = null,
            messageJson = JsonObject(emptyMap()),
            messageChoices = emptyList(),
            propertyId = 0
        )
        val messageWithCategorySubCategory = MessageWithCategorySubCategory(
            categories = null,
            language = null,
            messageJson = JsonObject(emptyMap()),
            messageChoices = emptyList(),
            propertyId = 0,
            categoryId = MessagesResponse.MessageMetaData.MessageCategory.Unknown,
            subCategoryId = MessagesResponse.MessageMetaData.MessageSubCategory.Unknown
        )
        assertEquals(
            message.encodeToJson(
                categoryId = MessagesResponse.MessageMetaData.MessageCategory.Unknown,
                subCategoryId = MessagesResponse.MessageMetaData.MessageSubCategory.Unknown),
            encodeToString(MessageWithCategorySubCategory.serializer(), messageWithCategorySubCategory)
        )
    }
}
