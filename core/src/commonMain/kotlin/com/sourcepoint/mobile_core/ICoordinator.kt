package com.sourcepoint.mobile_core

import com.sourcepoint.mobile_core.models.MessageToDisplay
import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.models.consents.SPUserData
import kotlinx.serialization.json.JsonObject

interface ICoordinator {
    val userData: SPUserData

    @Throws(Exception::class) suspend fun reportAction(action: SPAction): SPUserData

    @Throws(Exception::class) suspend fun loadMessages(
        authId: String?,
        pubData: JsonObject?,
        language: SPMessageLanguage
    ): List<MessageToDisplay>

    @Throws(Exception::class) suspend fun customConsentGDPR(
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    )

    @Throws(Exception::class) suspend fun deleteCustomConsentGDPR(
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    )

    fun clearLocalData()
}
