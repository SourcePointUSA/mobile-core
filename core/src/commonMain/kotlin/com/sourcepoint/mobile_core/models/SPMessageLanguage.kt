@file:Suppress("unused")
package com.sourcepoint.mobile_core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * List of languages supported by the TCF
 * https://register.consensu.org/translations/translationsEu
 */
@Serializable
enum class SPMessageLanguage(val shortCode: String) {
   @SerialName("sq") ALBANIAN("sq"),
   @SerialName("ar") ARABIC("ar"),
   @SerialName("eu") BASQUE("eu"),
   @SerialName("bs") BOSNIAN_LATIN("bs"),
   @SerialName("bg") BULGARIAN("bg"),
   @SerialName("ca") CATALAN("ca"),
   @SerialName("zh") CHINESE_SIMPLIFIED("zh"),
   @SerialName("zh-hant") CHINESE_TRADITIONAL("zh-hant"),
   @SerialName("hr") CROATIAN("hr"),
   @SerialName("cs") CZECH("cs"),
   @SerialName("da") DANISH("da"),
   @SerialName("nl") DUTCH("nl"),
   @SerialName("en") ENGLISH("en"),
   @SerialName("et") ESTONIAN("et"),
   @SerialName("fi") FINNISH("fi"),
   @SerialName("fr") FRENCH("fr"),
   @SerialName("gl") GALICIAN("gl"),
   @SerialName("ka") GEORGIAN("ka"),
   @SerialName("de") GERMAN("de"),
   @SerialName("el") GREEK("el"),
   @SerialName("he") HEBREW("he"),
   @SerialName("hi") HINDI("hi"),
   @SerialName("hu") HUNGARIAN("hu"),
   @SerialName("id") INDONESIAN("id"),
   @SerialName("it") ITALIAN("it"),
   @SerialName("ja") JAPANESE("ja"),
   @SerialName("ko") KOREAN("ko"),
   @SerialName("lv") LATVIAN("lv"),
   @SerialName("lt") LITHUANIAN("lt"),
   @SerialName("mk") MACEDONIAN("mk"),
   @SerialName("ms") MALAY("ms"),
   @SerialName("mt") MALTESE("mt"),
   @SerialName("no") NORWEGIAN("no"),
   @SerialName("pl") POLISH("pl"),
   @SerialName("pt-br") PORTUGUESE_BRAZIL("pt-br"),
   @SerialName("pt-pt") PORTUGUESE_PORTUGAL("pt-pt"),
   @SerialName("ro") ROMANIAN("ro"),
   @SerialName("ru") RUSSIAN("ru"),
   @SerialName("sr-cyrl") SERBIAN_CYRILLIC("sr-cyrl"),
   @SerialName("sr-latn") SERBIAN_LATIN("sr-latn"),
   @SerialName("sk") SLOVAK("sk"),
   @SerialName("sl") SLOVENIAN("sl"),
   @SerialName("es") SPANISH("es"),
   @SerialName("sw") SWAHILI("sw"),
   @SerialName("sv") SWEDISH("sv"),
   @SerialName("tl") TAGALOG("tl"),
   @SerialName("th") THAI("th"),
   @SerialName("tr") TURKISH("tr"),
   @SerialName("uk") UKRAINIAN("uk"),
   @SerialName("vi") VIETNAMESE("vi"),
   @SerialName("cy") WELSH("cy")
}
