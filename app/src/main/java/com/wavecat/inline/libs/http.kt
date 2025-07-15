@file:Suppress("ClassName", "unused")

package com.wavecat.inline.libs

import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.extensions.oneArgFunction
import com.wavecat.inline.extensions.threeArgFunction
import com.wavecat.inline.extensions.twoArgFunction
import com.wavecat.inline.extensions.zeroArgFunction
import com.wavecat.inline.libs.http.Companion.newInstance
import com.wavecat.inline.libs.http.Companion.toRequest
import com.wavecat.inline.utils.runOnUiThread
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.luaj.vm2.LuaNil
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import java.io.IOException

/**
 * Lua library for executing HTTP requests using OkHttp3.
 *
 * Provides comprehensive HTTP client functionality including support for
 * various HTTP methods, request body types (JSON, form data, multipart),
 * custom headers, and asynchronous request handling with callbacks.
 *
 * @see OkHttpClient
 */
class http : TwoArgFunction() {

    /**
     * Initializes the Lua library with HTTP client functions.
     *
     * Creates and configures a Lua table with all HTTP functionality
     * using the default OkHttpClient instance.
     *
     * @param name The name of the library (unused)
     * @param env The Lua environment to register the library in
     * @return LuaValue The created library table
     * @see newInstance
     */
    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library = newInstance(client)

        env["http"] = library
        env["package"]["loaded"]["http"] = library

        return library
    }

    companion object {
        /**
         * Default OkHttpClient instance used for HTTP requests.
         *
         * Shared client instance with default configuration for
         * all HTTP operations unless overridden.
         */
        private val client = OkHttpClient()

        /**
         * JSON media type constant for request body creation.
         *
         * Pre-defined MediaType for JSON content used in POST/PUT requests.
         */
        private val JSON = "application/json".toMediaType()


        /**
         * Converts a Lua table to an OkHttp Request object.
         *
         * Builds an HTTP request from Lua table configuration, handling
         * URL construction, query parameters, request body (JSON/form data),
         * headers, and HTTP method specification.
         *
         * @receiver LuaValue The Lua table containing request configuration
         * @param methodName The HTTP method to use (default: "GET")
         * @return Request The constructed OkHttp request
         * @throws IllegalArgumentException If required parameters are missing or invalid
         * @see Request.Builder
         * @see FormBody.Builder
         * @see Headers.Builder
         */
        private fun LuaValue.toRequest(methodName: String = "GET"): Request {
            val urlBuilder = get("url").checkjstring().toHttpUrl().newBuilder()

            get("params").takeIf { it.istable() }?.forEach { key, value ->
                urlBuilder.addQueryParameter(key.checkjstring(), value.tojstring())
            }

            val url = urlBuilder.build().toString()
            var body: RequestBody? = null

            when (val data = get("json")) {
                is LuaTable -> body = json.dumpTable(data, HashSet()).toString().toRequestBody(JSON)
                is LuaString -> body = data.tojstring().toRequestBody(JSON)
                is LuaNil -> {}
                else -> throw IllegalArgumentException("Incorrect json value")
            }

            if (body == null) {
                when (val data = get("data")) {
                    is LuaTable -> body = FormBody.Builder().apply {
                        data.forEach { key, value ->
                            add(key.checkjstring(), value.tojstring())
                        }
                    }
                        .build()

                    is LuaString -> {
                        val mediaType = get("mediaType").takeIf { it.isstring() }?.tojstring()?.toMediaType()
                            ?: throw IllegalArgumentException("mediaType not specified")
                        body = data.tojstring().toRequestBody(mediaType)
                    }

                    is LuaNil -> {}
                    else -> throw IllegalArgumentException("Incorrect data value")
                }
            }

            val headers = if (get("headers").istable()) {
                Headers.Builder().also {
                    get("headers").forEach { key, value -> it.add(key.checkjstring(), value.tojstring()) }
                }.build()
            } else {
                Headers.headersOf()
            }

            return Request.Builder()
                .url(url)
                .method(get("method").optjstring(methodName).uppercase(), body)
                .headers(headers)
                .build()
        }

        /**
         * Creates and executes an asynchronous HTTP call with Lua callbacks.
         *
         * Executes the HTTP request asynchronously and invokes the appropriate
         * Lua callback functions on the UI thread for response handling or error processing.
         *
         * @param request The OkHttp request to execute
         * @param onResponse Lua function to call on successful response
         * @param onFailure Lua function to call on request failure
         * @see Callback
         */
        private fun newLuaCall(
            request: Request,
            onResponse: LuaValue,
            onFailure: LuaValue,
        ) =
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    if (!onFailure.isnil())
                        runOnUiThread {
                            onFailure.call(
                                CoerceJavaToLua.coerce(call),
                                CoerceJavaToLua.coerce(e)
                            )
                        }
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    val body = response.body
                    val bytes: LuaValue = valueOf(body.bytes())
                    body.close()
                    if (!onResponse.isnil())
                        runOnUiThread {
                            onResponse.call(
                                CoerceJavaToLua.coerce(call),
                                CoerceJavaToLua.coerce(response),
                                bytes
                            )
                        }
                }
            })

        /**
         * Creates a Lua function for a specific HTTP method.
         *
         * Generates a closure that creates and executes HTTP requests
         * with the specified method name using table-based configuration.
         *
         * @param methodName The HTTP method name (GET, POST, PUT, etc.)
         * @return LuaValue A three-argument Lua function for the HTTP method
         */
        private fun createMethodFunction(methodName: String) =
            threeArgFunction { table, onResponse, onFailure ->
                newLuaCall(
                    request = table.checktable().toRequest(methodName),
                    onResponse = onResponse,
                    onFailure = onFailure
                )
                NIL
            }

        /**
         * Creates a new HTTP library instance with the specified OkHttpClient.
         *
         * Builds a complete Lua table with all HTTP functionality, utility builders,
         * and method-specific functions using the provided client instance.
         *
         * @param client The OkHttpClient instance to use for requests
         * @return LuaValue A Lua table containing all HTTP functions and utilities
         */
        private fun newInstance(client: OkHttpClient): LuaValue = tableOf().apply {
            /**
             * OkHttp Request class for manual request construction.
             */
            this["Request"] = CoerceJavaToLua.coerce(Request::class.java)

            /**
             * FormBody.Builder class for creating form data request bodies.
             */
            this["FormBodyBuilder"] = CoerceJavaToLua.coerce(FormBody.Builder::class.java)

            /**
             * MultipartBody.Builder class for creating multipart request bodies.
             */
            this["MultipartBodyBuilder"] = CoerceJavaToLua.coerce(MultipartBody.Builder::class.java)

            /**
             * Headers.Builder class for creating HTTP headers.
             */
            this["HeadersBuilder"] = CoerceJavaToLua.coerce(Headers.Builder::class.java)

            /**
             * Builds an HttpUrl object from base URL and query parameters.
             *
             * param url Base URL string
             * param table Lua table containing query parameters
             * @return HttpUrl The constructed URL with query parameters
             */
            this["buildUrl"] = twoArgFunction { url, table ->
                val httpBuilder = url.checkjstring().toHttpUrl().newBuilder()

                table.forEach { key, value ->
                    httpBuilder.addQueryParameter(key.checkjstring(), value.tojstring())
                }

                CoerceJavaToLua.coerce(httpBuilder.build())
            }

            /**
             * Builds a FormBody from a Lua table of form data.
             *
             * param data Lua table containing form field key-value pairs
             * @return FormBody The constructed form body
             */
            this["buildFormBody"] = oneArgFunction { data ->
                CoerceJavaToLua.coerce(
                    FormBody.Builder().apply {
                        data.forEach { key, value -> add(key.checkjstring(), value.tojstring()) }
                    }.build()
                )
            }

            /**
             * Builds a MultipartBody from a Lua table of form data.
             *
             * param data Lua table containing multipart form field key-value pairs
             * @return MultipartBody The constructed multipart body
             */
            this["buildMultipartBody"] = oneArgFunction { data ->
                CoerceJavaToLua.coerce(
                    MultipartBody.Builder().apply {
                        data.forEach { key, value -> addFormDataPart(key.checkjstring(), value.tojstring()) }
                    }.build()
                )
            }

            /**
             * Builds a RequestBody from string data and media type.
             *
             * param data String content for the request body
             * param mediaType Media type string (e.g., "application/json")
             * @return RequestBody The constructed request body
             */
            this["buildBody"] = twoArgFunction { data, mediaType ->
                CoerceJavaToLua.coerce(
                    data.checkjstring().toRequestBody(mediaType.checkjstring().toMediaType())
                )
            }

            /**
             * Builds Headers from a Lua table of header key-value pairs.
             *
             * param table Lua table containing header names and values
             * @return Headers The constructed headers object
             */
            this["buildHeaders"] = oneArgFunction { table ->
                CoerceJavaToLua.coerce(
                    Headers.Builder().apply {
                        table.forEach { key, value -> add(key.checkjstring(), value.tojstring()) }
                    }.build()
                )
            }

            /**
             * Builds a Request object from a Lua table configuration.
             *
             * param table Lua table containing request configuration
             * @return Request The constructed HTTP request
             * @see toRequest
             */
            this["buildRequest"] = oneArgFunction { table ->
                CoerceJavaToLua.coerce(table.checktable().toRequest())
            }

            /**
             * Creates a new OkHttpClient.Builder instance.
             *
             * @return OkHttpClient.Builder A new client builder for customization
             */
            this["newBuilder"] = zeroArgFunction { CoerceJavaToLua.coerce(client.newBuilder()) }

            /**
             * Executes an HTTP request asynchronously with callbacks.
             *
             * param request The Request object to execute
             * param onResponse Callback function for successful responses
             * param onFailure Callback function for request failures
             */
            this["call"] = threeArgFunction { request, onResponse, onFailure ->
                newLuaCall(
                    request = request.checkuserdata(Request::class.java) as Request,
                    onResponse = onResponse,
                    onFailure = onFailure
                )
                NIL
            }

            /**
             * Executes HTTP GET request from table configuration.
             *
             * param table Request configuration table
             * param onResponse Success callback function
             * param onFailure Error callback function
             */
            this["get"] = createMethodFunction("GET")

            /**
             * Executes HTTP POST request from table configuration.
             *
             * param table Request configuration table
             * param onResponse Success callback function
             * param onFailure Error callback function
             */
            this["post"] = createMethodFunction("POST")

            /**
             * Executes HTTP PUT request from table configuration.
             *
             * param table Request configuration table
             * param onResponse Success callback function
             * param onFailure Error callback function
             */
            this["put"] = createMethodFunction("PUT")

            /**
             * Executes HTTP PATCH request from table configuration.
             *
             * param table Request configuration table
             * param onResponse Success callback function
             * param onFailure Error callback function
             */
            this["patch"] = createMethodFunction("PATCH")

            /**
             * Executes HTTP DELETE request from table configuration.
             *
             * param table Request configuration table
             * param onResponse Success callback function
             * param onFailure Error callback function
             */
            this["delete"] = createMethodFunction("DELETE")

            /**
             * Metatable allowing the library to be called as a function.
             *
             * Enables creating new HTTP library instances with custom OkHttpClient.
             * Usage: http(customClient) returns new library instance.
             */
            this.setmetatable(tableOf().apply {
                this[CALL] = twoArgFunction { _, client ->
                    newInstance(client.checkuserdata(OkHttpClient::class.java) as OkHttpClient)
                }
            })
        }
    }
}
