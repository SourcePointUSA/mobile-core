## 0.1.12-beta-2 (Aug, 18, 2025)
* `transitiveExport` set back to `true` for iOS target
* `isStatic` set to default value `false`

## 0.1.12-beta-1 (Aug, 13, 2025)
* `transitiveExport` set to `false` for iOS target

## 0.1.11 (Jul, 20, 2025)
* add `versionId: String?` to `PreferencesConsent.PreferencesStatus`

## 0.1.10 (Jul, 11, 2025)
* Update Gradle version from 8.9 to 8.14.2
* Upload test reports on GitHub Test Action run
* Fixes of `pv-data` and `get-choice` requests & response for globalcmp 
* Add `childPmId` feature support to globalcmp
* Add `prtnUUID` to all campaigns `/choice` requests
* Add `pv-data` request for preferences
* Add `HttpRequestTimeoutException` to all `@Throws` to improve iOS exception handling on timeout

## 0.1.9 (Jun, 14, 2025)
* added support to `globalcmp` campaign type

## 0.1.8 (Jun, 14, 2025)
* migrated to new Maven Central repository

## 0.1.7 (Jun, 13, 2025)
* fixed an issue preventing preferences UUID from being persisted

## 0.1.6 (May, 22, 2025)
* generate `SPMobileCore` XCFramework statically

## 0.1.5 (May, 16, 2025)
* added new campaign type `Preferences`
* expand network requests to support preferences
* implemented preferences gate logic via targeting params. See `preferencesTargetingParams`.

## 0.1.4 (April, 08, 2025)
* expanded list of message languages `SPMessageLanguage`

## 0.1.3 (April, 07, 2025)
* added `requestTimeout` to `Coordinator`'s constructor
* bumped android compileSdk from 34 to 35

## 0.1.2 (April, 04, 2025)
* added `Coordinator` secondary constructor with optional State.

## 0.1.1 (March, 20, 2025)
* [DIA-5293](https://sourcepoint.atlassian.net/browse/DIA-5293) fixed an issue causing the `SourcePointClient` to throw an error when calling `/custom-metrics`
* implement a test for resetting sampling status when sampling rate changes
* applies default request query params to `deleteCustomConsentTo`

## 0.1.0 (March, 19, 2025)
* [DIA-4943](https://sourcepoint.atlassian.net/browse/DIA-4943) implemented `SPCoordinator.reportAction`
* [DIA-4948](https://sourcepoint.atlassian.net/browse/DIA-4948) implemented `SPCoordinator.loadMessages`
* [DIA-5341](https://sourcepoint.atlassian.net/browse/DIA-5341) fixed an issue causing `SPClient` to throw exception when something goes wrong with `/custom-metrics` call

## 0.0.11 (March, 07, 2025)
* [DIA-5293](https://sourcepoint.atlassian.net/browse/DIA-5293) fixed an issue causing the `/meta-data` response to fail parsing.

## 0.0.10 (December, 12, 2024)
* [DIA-3496](https://sourcepoint.atlassian.net/browse/DIA-3496) Implement `GET` consent-all and reject-all calls
* [DIA-3497](https://sourcepoint.atlassian.net/browse/DIA-3497) Implement `POST` gdpr choice call
* [DIA-3498](https://sourcepoint.atlassian.net/browse/DIA-3498) Implement `POST` ccpa choice call
* [DIA-3499](https://sourcepoint.atlassian.net/browse/DIA-3499) Implement `POST` usnat choice call
* [DIA-4801](https://sourcepoint.atlassian.net/browse/DIA-4801) Implement `POST` ReportIdfaStatus

## 0.0.9 (October, 25, 2024)
* fix `MessagesRequest.Body.CCPA` renaming `consentStatus` to `status`
* fix `MessagesResponse.Campaigns` by making their derived consent classes optional

## 0.0.8 (October, 23, 2024)
* add CCPA to `MessagesResponse`
* add ios14 to `MessagesResponse`
* refactor `SPIDFAStatus` to use `SerialName`
* add missing languages to `SPMessageLanguage`
* make enums serialization case insensitive
* refactored `MessagesRequest` for native SDK integration

## 0.0.7 (October, 17, 2024)
* update kotlin 1.9.x -> 2.0.21
* update gradle -> 8.9
* update ktor 2.x -> 3.0.0
* update other minor dependencies to latest stable version

## 0.0.6 (October, 16, 2024)
* Implement `POST /custom` consent call
* Implement `DELETE /custom` consent call
* Fix a bug causing `CCPAConsentStatus` to always assume `Unknown` value

## 0.0.5 (September, 18, 2024)
* implement `/custom-metrics` call
* implement network request timeout
* improve network error handling
* call `/custom-metrics` on error

## 0.0.4 (September, 12, 2024)
* rename `GDPRConsent.ConsentStatusGCMStatus` to `GDPRConsent.GCMStatus`
* add missing properties to `ConsentStatus`
* typealias `IABData` to `GPPData` and `TCData`

## 0.0.3 (September, 10, 2024)
* Add Support to CCPA
* Add missing fields to MetaData response
* Improve GitHub action build time

## 0.0.2 (September, 06, 2024)
* Initial release to Cocoapods

## 0.0.1 (September, 03, 2024)
* Initial release to Maven
