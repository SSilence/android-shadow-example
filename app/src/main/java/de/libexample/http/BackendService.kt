package de.libexample.http

import android.os.Handler
import android.os.Looper
import de.libexample.gson.reflect.TypeToken
import java.util.concurrent.Executors

class BackendService(private val restClient: RestClient) {

    // example: returns sunrise for geo location
    fun exampleGetCall(callback: (SunriseResponse) -> Unit) {
        val request =
            HttpRequest.get("https://api.sunrise-sunset.org/json?lat=48.13722074356059&lng=11.575504314173754")
        runOnIoThread {
            val result = restClient.exchange(request, object : TypeToken<SunriseResponse>() {})
            runOnMainThread {
                callback(result)
            }
        }
    }

    // example: returns given text capitalized
    fun examplePostCall(text: String, callback: (ExamplePostCallResponse) -> Unit) {
        val body = ExamplePostCallRequest(text)
        val request = HttpRequest.post("HTTPS://API.SHOUTCLOUD.IO/V1/SHOUT", body)
        runOnIoThread {
            val result = restClient.exchange(request, object : TypeToken<ExamplePostCallResponse>() {})
            runOnMainThread {
                callback(result)
            }
        }
    }
}

fun runOnIoThread(block: () -> Unit) {
    Executors.newSingleThreadExecutor().execute {
        block()
    }
}

fun runOnMainThread(block: () -> Unit) {
    Handler(Looper.getMainLooper()).post {
        block()
    }
}

class ExamplePostCallRequest(val INPUT: String)

class ExamplePostCallResponse(val INPUT: String,
                              val OUTPUT: String)


class SunriseResponse(val results: SunriseResult,
                      val status: String)

class SunriseResult(val sunrise: String,
                    val sunset: String,
                    val solar_noon: String,
                    val day_length: String,
                    val civil_twilight_begin: String,
                    val civil_twilight_end: String,
                    val nautical_twilight_begin: String,
                    val nautical_twilight_end: String,
                    val astronomical_twilight_begin: String,
                    val astronomical_twilight_end: String)