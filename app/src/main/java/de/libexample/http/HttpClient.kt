package de.libexample.http

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.X509Certificate
import javax.net.ssl.*

private const val TIMEOUT = 40000

typealias RawHttpRequest = HttpRequest<ByteArray>
typealias RawHttpResponse = HttpResponse<ByteArray>

data class HttpRequest<T>(
    val url: String,
    val method: HttpMethod,
    val body: T,
    val headers: Map<String, List<String>> = mapOf()
) {

    fun <E> withBody(newBody: E): HttpRequest<E> =
        HttpRequest(
            url = this.url,
            method = this.method,
            body = newBody,
            headers = this.headers
        )

    fun withHeader(header: HttpHeader): HttpRequest<T> {
        val newHeaders = this.headers.plus(Pair(header.name, listOf(header.value)))
        return this.copy(headers = newHeaders)
    }

    override fun toString() = "$method $url"

    companion object {
        fun get(url: String, headers: Map<String, List<String>> = mapOf()) = HttpRequest(url = url, method = HttpMethod.GET, body = ByteArray(0), headers = headers)
        fun delete(url: String, headers: Map<String, List<String>> = mapOf()) = HttpRequest(url = url, method = HttpMethod.DELETE, body = ByteArray(0), headers = headers)
        fun <T> delete(url: String, body: T, headers: Map<String, List<String>> = mapOf()) = HttpRequest(url = url, method = HttpMethod.DELETE, body = body, headers = headers)
        fun <T> post(url: String, body: T, headers: Map<String, List<String>> = mapOf()) = HttpRequest(url = url, method = HttpMethod.POST, body = body, headers = headers)
    }
}

data class HttpHeader(val name: String, val value: String)

class HttpResponse<T>(
    val status: Int,
    val body: T? = null,
    val headers: Map<String, List<String>> = mapOf()
) {

    fun isStatus2xx(): Boolean = status in 200..299
    fun isStatus4xx(): Boolean = status in 400..499
    fun isStatus5xx(): Boolean = status in 500..599
}

enum class HttpMethod {
    GET,
    PUT,
    POST,
    DELETE
}

interface HttpRequestInterceptor {
    fun <T> intercept(request: HttpRequest<T>): HttpRequest<T>
}

class BasicAuthInterceptor(private val basicAuthHeader: String) : HttpRequestInterceptor {
    override fun <T> intercept(request: HttpRequest<T>): HttpRequest<T> {
        return request.withHeader(HttpHeader("Authorization", basicAuthHeader))
    }
}

class HttpClient(private val sslEnabled: Boolean = true) {

    private val unsecureTrustManager = object: X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
    }

    private val unsecureHostnameVerifier = object: HostnameVerifier {
        override fun verify(hostname: String?, session: SSLSession?): Boolean = true
    }

    private val unsecureSslSocketFactory: SSLSocketFactory by lazy {
        val sc = SSLContext.getInstance("SSL")
        sc.init(null, arrayOf(unsecureTrustManager), java.security.SecureRandom())
        sc.socketFactory
    }

    private val requestInterceptors: MutableList<HttpRequestInterceptor> = mutableListOf()

    fun addRequestInterceptor(interceptor: HttpRequestInterceptor) {
        requestInterceptors.add(interceptor)
    }

    fun exchange(inputRequest: RawHttpRequest): RawHttpResponse {
        val request = applyInterceptors(inputRequest)

        val connection = createConnection(request.url)
        connection.requestMethod = request.method.name
        request.headers.forEach { (headerName, headerValues) ->
            headerValues.forEach { headerValue -> connection.addRequestProperty(headerName, headerValue) }
        }

        connection.connect()
        if (request.body.isNotEmpty()) {
            connection.outputStream.use { it.write(request.body) }
        }

        val statusCode = connection.responseCode
        val inputStream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        return HttpResponse(
            status = connection.responseCode,
            headers = connection.headerFields,
            body = readBytesAndClose(inputStream)
        )
    }

    private fun applyInterceptors(request: RawHttpRequest): RawHttpRequest {
        var transformedRequest = request
        for (interceptor in requestInterceptors) {
            transformedRequest = interceptor.intercept(transformedRequest)
        }

        return transformedRequest
    }

    private fun createConnection(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        if (!sslEnabled && connection is HttpsURLConnection) {
            connection.sslSocketFactory = unsecureSslSocketFactory
            connection.hostnameVerifier = unsecureHostnameVerifier
        }
        connection.connectTimeout = TIMEOUT
        connection.readTimeout = TIMEOUT
        return connection
    }

    private fun readBytesAndClose(stream: InputStream): ByteArray {
        return try {
            stream.buffered().use { it.readBytes() }
        } catch (ex: IOException) {
            ByteArray(0)
        }
    }
}