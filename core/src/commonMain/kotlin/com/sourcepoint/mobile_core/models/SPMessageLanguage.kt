package com.sourcepoint.mobile_core.models

import kotlinx.serialization.SerialName

enum class SPMessageLanguage(val value: String) {
   @SerialName("BG") BULGARIAN("BG"),
   @SerialName("CA") CATALAN("CA"),
   @SerialName("ZH") CHINESE("ZH"),
   @SerialName("HR") CROATIAN("HR"),
   @SerialName("CS") CZECH("CS"),
   @SerialName("DA") DANISH("DA"),
   @SerialName("NL") DUTCH("NL"),
   @SerialName("EN") ENGLISH("EN"),
   @SerialName("ET") ESTONIAN("ET"),
   @SerialName("FI") FINNISH("FI"),
   @SerialName("FR") FRENCH("FR"),
   @SerialName("GD") GAELIC("GD"),
   @SerialName("DE") GERMAN("DE"),
   @SerialName("EL") GREEK("EL"),
   @SerialName("HU") HUNGARIAN("HU"),
   @SerialName("IS") ICELANDIC("IS"),
   @SerialName("IT") ITALIAN("IT"),
   @SerialName("JA") JAPANESE("JA"),
   @SerialName("LV") LATVIAN("LV"),
   @SerialName("LT") LITHUANIAN("LT"),
   @SerialName("NO") NORWEGIAN("NO"),
   @SerialName("PL") POLISH("PL"),
   @SerialName("PT") PORTUGUESE("PT"),
   @SerialName("RO") ROMANIAN("RO"),
   @SerialName("RU") RUSSIAN("RU"),
   @SerialName("SR-CYRL") SERBIAN_CYRILLIC("SR-CYRL"),
   @SerialName("SR-LATN") SERBIAN_LATIN("SR-LATN"),
   @SerialName("SK") SLOVAKIAN("SK"),
   @SerialName("SL") SLOVENIAN("SL"),
   @SerialName("ES") SPANISH("ES"),
   @SerialName("SV") SWEDISH("SV"),
   @SerialName("TR") TURKISH("TR"),
   @SerialName("TL") TAGALOG("TL");
}
