package com.itsorderkds.data.util

class FlexibleDoubleAdapter : com.google.gson.JsonDeserializer<Double?> {
    override fun deserialize(
        json: com.google.gson.JsonElement?,
        typeOfT: java.lang.reflect.Type?,
        context: com.google.gson.JsonDeserializationContext?
    ): Double? {
        if (json == null || json.isJsonNull) return null
        val p = json.asJsonPrimitive
        return when {
            p.isNumber -> p.asDouble
            p.isString -> p.asString.trim().toDoubleOrNull()
            else -> null
        }
    }
}