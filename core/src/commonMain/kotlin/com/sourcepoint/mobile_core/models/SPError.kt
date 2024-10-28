package com.sourcepoint.mobile_core.models

open class SPError(
    val code: String = "sp_metric_generic_mobile-core_error",
    val description: String = "Something went wrong in the Mobile Core",
    open val campaignType: SPCampaignType? = null
): Exception(description)

open class SPUnknownNetworkError(path: String): SPError(
    code = "sp_metric_unknown_network_error_${path}",
    description = "Something went wrong while performing a request to $path.",
)

open class SPClientTimeout(
    path: String,
    timeoutInSeconds: Int,
): SPError(
    code = "sp_metric_network_error_${path}_${timeoutInSeconds}",
    description = "The SDK timedout before being able to complete the request in $timeoutInSeconds seconds.",
)

open class SPNetworkError(
    statusCode: Int,
    path: String,
    override val campaignType: SPCampaignType? = null
): SPError(
    code = "sp_metric_network_error_${path}_${statusCode}",
    description = "The server responded with HTTP $statusCode.",
    campaignType = campaignType
)

open class SPUnableToParseBodyError(
    bodyName: String?,
): SPError(
    code = "sp_metric_invalid_response_${bodyName}",
    description = "The server responded with HTTP 200, but the body doesn't match the expected response type: $bodyName",
)

open class InvalidChoiceAllParamsError(): SPError (
    code = "sp_metric_invalid_choice_all_query_params"
)
