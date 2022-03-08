package de.libexample.http

import android.util.Log
import de.libexample.gson.JsonParseException
import de.libexample.gson.reflect.TypeToken
import de.libexample.Json
import java.lang.RuntimeException
import java.util.*

class ResponseException(val status: Int, val body: String) : RuntimeException()
class DeserializeException(val body: String) : RuntimeException()

class RestClient(private val httpClient: HttpClient,
                 private val debug: Boolean = false) {

    private val jsonSerializer = Json()

    fun <T> exchange(request: HttpRequest<*>, typeToken: TypeToken<T>): T {
        val requestBytes = convertToBytes(request.body)
        val rawHttpRequest = request
            .withHeader(HttpHeader("Content-Type", "application/json"))
            .withHeader(HttpHeader("Accept-Language", Locale.getDefault().language))
            .withBody(requestBytes)

        if (debug) {
            Log.d(javaClass.simpleName, "Request $request with body ${String(requestBytes)}")
        }

        val rawHttpResponse = httpClient.exchange(rawHttpRequest)
        if (rawHttpResponse.isStatus2xx()) {
            if (debug) {
                val bodyString = if (rawHttpResponse.body != null) String(rawHttpResponse.body) else "[empty]"
                Log.d(javaClass.simpleName, "Request $request resulted with response $bodyString")
            }
            return convertToObject(rawHttpResponse.body, typeToken)
        } else {
            val body = String(rawHttpResponse.body ?: ByteArray(0), Charsets.UTF_8)
            val exception = ResponseException(rawHttpResponse.status, body)
            if (debug) {
                Log.e(RestClient::class.java.simpleName, "Request $request resulted with error: $exception")
            }
            throw exception
        }
    }

    private fun convertToBytes(body: Any?): ByteArray {
        return if (body is ByteArray) {
            body
        } else {
            jsonSerializer.toJson(body).toByteArray(Charsets.UTF_8)
        }
    }

    private fun <T> convertToObject(bytes: ByteArray?, typeToken: TypeToken<T>): T {
        val json = String(bytes!!, Charsets.UTF_8)
        try {
            return jsonSerializer.fromJson(json, typeToken.type)
        } catch (ex: JsonParseException) {
            Log.e("RestClient", "Could not parse JSON", ex)
            throw DeserializeException(json)
        }
    }
}