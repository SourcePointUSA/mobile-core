package com.sourcepoint.mobile_core.models

open class SPError(
    val code: String = "sp_metric_generic_mobile-core_error",
    val description: String = "Something went wrong in the Mobile Core",
    override val cause: Throwable? = null,
    open val campaignType: SPCampaignType? = null
): Throwable(description) {
    companion object {
        fun castToSPError(error: Throwable): SPError =
            error as? SPError ?:
                SPError(cause = error, description = error.message ?: "Something went wrong in the Mobile Core")
    }
}

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

open class InvalidPropertyNameError(propertyName: String): SPError(
    code = "sp_metric_invalid_property_name",
    description = "PropertyName can only include letters, numbers, '.', ':', '-' and '/'. $propertyName passed is invalid"
)

open class ReportActionException(
    actionType: SPActionType,
    campaignType: SPCampaignType?,
    cause: SPError
): SPError(
    code = "sp_metric_report_action_exception_${campaignType?.name}_${actionType.name}",
    description = "Unable to report ${actionType.name} action for campaign ${campaignType?.name} due to ${cause.description}",
    cause = cause
)

open class LoadMessagesException(cause: SPError): SPError(
    code = "sp_metric_load_messages",
    description = "Unable to loadMessages due to ${cause.description}",
    cause = cause
)

open class PostCustomConsentGDPRException(cause: SPError): SPError(
    code = "sp_metric_post_custom_consent_gdpr",
    description = "Unable to post CustomConsentGDPR due to ${cause.description}",
    cause = cause
)

open class DeleteCustomConsentGDPRException(cause: SPError): SPError(
    code = "sp_metric_delete_custom_consent_gdpr",
    description = "Unable to delete CustomConsentGDPR due to ${cause.description}",
    cause = cause
)

open class InvalidRequestAPIError(cause: Throwable, endpoint: InvalidAPICode): SPError (
    code = "sp_metric_invalid_response_api${endpoint.type}",
    description = "The SDK got an unexpected response from ${endpoint.name}",
    cause = castToSPError(cause)
)

enum class InvalidAPICode(val type: String) {
    META_DATA("_meta-data"),
    CONSENT_STATUS("_consent-status"),
    PV_DATA("_pv-data"),
    MESSAGES("_messages"),
    ERROR_METRICS("_error-metrics"),
    CCPA_ACTION("_CCPA-action"),
    GDPR_ACTION("_GDPR-action"),
    USNAT_ACTION("_USNAT-action"),
    GLOBALCMP_ACTION("_GLOBALCMP-action"),
    PREFERENCES_ACTION("_PREFERENCES-action"),
    IDFA_STATUS( "_IDFA-status"),
    CHOICE_ALL("_choice-all"),
    CUSTOM_CONSENT("custom-consent-GDPR"),
    DELETE_CUSTOM_CONSENT("_delete-custom-consent-GDPR"),
    CCPA_PRIVACY_MANAGER("_CCPA-privacy-manager"),
    GDPR_PRIVACY_MANAGER("_GDPR-privacy-manager"),
    CCPA_MESSAGE("_CCPA-message"),
    GDPR_MESSAGE("_GDPR-message"),
    EMPTY("")
}
