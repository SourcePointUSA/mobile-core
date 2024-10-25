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
