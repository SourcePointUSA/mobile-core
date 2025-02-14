package com.sourcepoint.mobile_core.models

import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.descriptors.*

@Serializable(with = SPPropertyName.Serializer::class)
open class SPPropertyName private constructor(val rawValue: String) {
    override fun toString(): String = rawValue.removePrefix("https://")

    companion object {
        private val VALID_REGEX = Regex("^[a-zA-Z.:/0-9-]*$")

        @Throws(InvalidPropertyNameError::class)
        fun create(rawValue: String): SPPropertyName {
            val validatedValue = validate(rawValue)
            val finalValue = if (!validatedValue.startsWith("https://") && !validatedValue.startsWith("http://")) {
                "https://$validatedValue"
            } else {
                validatedValue
            }
            return SPPropertyName(finalValue)
        }

        @Throws(InvalidPropertyNameError::class)
        private fun validate(string: String): String {
            if (!VALID_REGEX.matches(string)) {
                throw InvalidPropertyNameError("PropertyName can only include letters, numbers, '.', ':', '-' and '/'. '$string' is invalid")
            }
            return string
        }
    }

    override fun equals(other: Any?): Boolean =
        other is SPPropertyName && rawValue == other.rawValue

    override fun hashCode(): Int = rawValue.hashCode()

    object Serializer : KSerializer<SPPropertyName> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            serialName = "SPPropertyName",
            kind = PrimitiveKind.STRING
        )

        override fun serialize(encoder: Encoder, value: SPPropertyName) {
            encoder.encodeString(value.rawValue)
        }

        override fun deserialize(decoder: Decoder): SPPropertyName =
            create(decoder.decodeString())
    }
}

class InvalidPropertyNameError(message: String) : IllegalArgumentException(message)
