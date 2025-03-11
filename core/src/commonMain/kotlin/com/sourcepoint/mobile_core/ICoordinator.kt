package com.sourcepoint.mobile_core

import com.sourcepoint.mobile_core.models.DeleteCustomConsentGDPRException
import com.sourcepoint.mobile_core.models.InvalidCustomConsentUUIDError
import com.sourcepoint.mobile_core.models.LoadMessagesException
import com.sourcepoint.mobile_core.models.MessageToDisplay
import com.sourcepoint.mobile_core.models.PostCustomConsentGDPRException
import com.sourcepoint.mobile_core.models.ReportActionException
import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPError
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.models.SPNetworkError
import com.sourcepoint.mobile_core.models.SPUnknownNetworkError
import com.sourcepoint.mobile_core.models.consents.SPUserData
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.cancellation.CancellationException

interface ICoordinator {
    val userData: SPUserData

    @Throws(ReportActionException::class, CancellationException::class)
    suspend fun reportAction(action: SPAction): SPUserData

    @Throws(LoadMessagesException::class, CancellationException ::class)
    suspend fun loadMessages(
        authId: String?,
        pubData: JsonObject?,
        language: SPMessageLanguage
    ): List<MessageToDisplay>

    @Throws(
        InvalidCustomConsentUUIDError::class,
        PostCustomConsentGDPRException::class,
        CancellationException::class
    )
    suspend fun customConsentGDPR(
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    )

    @Throws(
        InvalidCustomConsentUUIDError::class,
        DeleteCustomConsentGDPRException::class,
        CancellationException::class
    )
    suspend fun deleteCustomConsentGDPR(
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    )

    @Throws(SPNetworkError::class, SPUnknownNetworkError::class, CancellationException::class)
    suspend fun logError(error: SPError)

    fun clearLocalData()

    fun setTranslateMessage(value: Boolean)
}
