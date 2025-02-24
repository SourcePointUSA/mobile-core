package com.sourcepoint.mobile_core.network.responses

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json.Default.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class MessagesResponseTest {
    @Test
    fun encodeToJsonTest() = runTest {
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
