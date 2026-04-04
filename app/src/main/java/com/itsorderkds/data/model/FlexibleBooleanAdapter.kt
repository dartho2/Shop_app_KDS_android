package com.itsorderkds.data.model

import com.google.gson.*
import java.lang.reflect.Type

class FlexibleBooleanAdapter : JsonDeserializer<Boolean>, JsonSerializer<Boolean> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Boolean {
        if (json == null || json.isJsonNull) return false
        val p = json.asJsonPrimitive
        return when {
            p.isBoolean -> p.asBoolean
            p.isNumber  -> p.asInt != 0
            p.isString  -> when (p.asString.trim().lowercase()) {
                "true", "1"  -> true
                "false", "0", "" -> false
                else -> throw JsonParseException("Not a boolean-like: ${p.asString}")
            }
            else -> throw JsonParseException("Unsupported JSON for boolean")
        }
    }

    // opcjonalnie — wysyłamy dalej jako normalny boolean
    override fun serialize(src: Boolean?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src == true)
    }
}
