package de.libexample

import de.libexample.gson.GsonBuilder
import de.libexample.gson.JsonDeserializationContext
import de.libexample.gson.JsonDeserializer
import de.libexample.gson.JsonElement
import java.lang.reflect.Type
import java.util.*

class Json {

    private val gson = GsonBuilder().registerTypeAdapter(Date::class.java, DateJsonDeserializer()).create()

    fun toJson(obj: Any?) = gson.toJson(obj)

    fun <T> fromJson(str: String, type: Type): T = gson.fromJson(str, type)

    private inner class DateJsonDeserializer: JsonDeserializer<Date> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext) =
            Date(json.asJsonPrimitive.asLong)
    }

}