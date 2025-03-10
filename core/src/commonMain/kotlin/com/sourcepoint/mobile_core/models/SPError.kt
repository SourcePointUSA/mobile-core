package com.sourcepoint.mobile_core.models

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

open class SPError(
    val code: String = "sp_metric_generic_mobile-core_error",
    val description: String = "Something went wrong in the Mobile Core",
    val causedBy: SPError? = null,
    open val campaignType: SPCampaignType? = null
): Exception(description)

open class SPUnknownNetworkError(path: String): SPError(
    code = "sp_metric_unknown_network_error_${path}",
    description = "Something went wrong while performing a request to $path.",
)

open class SPClientTimeout(
    path: String,
    timeoutInSeconds: Int,
    httpVerb: String,
): SPError(
    code = "sp_metric_network_error_${httpVerb}_${path}_${timeoutInSeconds}",
    description = "The SDK timed out before being able to complete the request in $timeoutInSeconds seconds.",
)

open class SPNetworkError(
    statusCode: Int?,
    httpVerb: String,
    path: String,
    code: String = "sp_metric_network_error_${httpVerb}_${path}_${statusCode}",
    description: String = "The server responded with HTTP $statusCode."
): SPError(code = code, description = description)

open class SPUnableToParseBodyError(
    bodyName: String?,
): SPError(
    code = "sp_metric_invalid_response_${bodyName}",
    description = "The server responded with HTTP 200, but the body doesn't match the expected response type: $bodyName",
)

open class InvalidChoiceAllParamsError : SPError (
    code = "sp_metric_invalid_choice_all_query_params"
)

open class InvalidCustomConsentUUIDError : SPError (
    code = "sp_metric_invalid_consent_UUID"
)

@OptIn(ExperimentalObjCName::class)
@ObjCName(swiftName = "CoreInvalidPropertyNameError")
open class InvalidPropertyNameError(propertyName: String): SPError(
    code = "sp_metric_invalid_property_name",
    description = "PropertyName can only include letters, numbers, '.', ':', '-' and '/'. $propertyName passed is invalid"
)

open class ReportActionException(
    actionType: SPActionType,
    campaignType: SPCampaignType?,
    causedBy: SPError
): SPError(
    code = "sp_metric_report_action_exception_${campaignType?.name}_${actionType.name}",
    description = "Unable to report ${actionType.name} action for campaign ${campaignType?.name} due to ${causedBy.description}",
    causedBy = causedBy
)

open class LoadMessagesException(causedBy: SPError): SPError(
    code = "sp_metric_load_messages",
    description = "Unable to loadMessages due to ${causedBy.description}",
    causedBy = causedBy
)

open class PostCustomConsentGDPRException(causedBy: SPError): SPError(
    code = "sp_metric_post_custom_consent_gdpr",
    description = "Unable to post CustomConsentGDPR due to ${causedBy.description}",
    causedBy = causedBy
)

open class DeleteCustomConsentGDPRException(causedBy: SPError): SPError(
    code = "sp_metric_delete_custom_consent_gdpr",
    description = "Unable to delete CustomConsentGDPR due to ${causedBy.description}",
    causedBy = causedBy
)
