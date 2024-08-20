package com.sourcepoint.mobile_core

import com.sourcepoint.mobile_core.network.Client
import com.sourcepoint.mobile_core.network.requests.MetaData
import com.sourcepoint.mobile_core.storage.Repository

class Coordinator(
    private val accountId: Int,
    private val propertyId: Int,
    private val propertyName: String,
    private val repository: Repository,
    private val spClient: Client
) {
    constructor(accountId: Int, propertyId: Int, propertyName: String) : this(
        accountId,
        propertyId,
        propertyName,
        repository = Repository(),
        spClient = Client(accountId, propertyId, propertyName)
    )
    constructor(accountId: Int, propertyId: Int, propertyName: String, repository: Repository) : this(
        accountId,
        propertyId,
        propertyName,
        repository,
        spClient = Client(accountId, propertyId, propertyName)
    )

    suspend fun getMetaData(campaigns: MetaData.Campaigns): String {
        val metaDataResponse = spClient.getMetaData(campaigns)
        val message = """
            The return of /meta-data is:
            $metaDataResponse
            
            The cached version of /meta-data is:
            ${repository.cachedMetaData}
        """
        repository.cachedMetaData = metaDataResponse.toString()
        return message
    }
}
