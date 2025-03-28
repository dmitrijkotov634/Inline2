@file:Suppress("ClassName", "unused")

package com.wavecat.inline.libs

import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.extensions.oneArgFunction
import com.wavecat.inline.extensions.threeArgFunction
import com.wavecat.inline.extensions.twoArgFunction
import com.wavecat.inline.extensions.zeroArgFunction
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

class http : TwoArgFunction() {
    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library = newInstance(client)

        env["http"] = library
        env["package"]["loaded"]["http"] = library

        return library
    }

    companion object {
        private val client = OkHttpClient()

        private val JSON = "application/json".toMediaType()

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
                    val bytes: LuaValue = valueOf(body?.bytes() ?: byteArrayOf())
                    body?.close()
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

        private fun createMethodFunction(methodName: String) =
            threeArgFunction { table, onResponse, onFailure ->
                newLuaCall(
                    request = table.checktable().toRequest(methodName),
                    onResponse = onResponse,
                    onFailure = onFailure
                )
                NIL
            }

        private fun newInstance(client: OkHttpClient): LuaValue = tableOf().apply {
            this["Request"] = CoerceJavaToLua.coerce(Request::class.java)

            this["FormBodyBuilder"] = CoerceJavaToLua.coerce(FormBody.Builder::class.java)
            this["MultipartBodyBuilder"] = CoerceJavaToLua.coerce(MultipartBody.Builder::class.java)
            this["HeadersBuilder"] = CoerceJavaToLua.coerce(Headers.Builder::class.java)

            this["buildUrl"] = twoArgFunction { url, table ->
                val httpBuilder = url.checkjstring().toHttpUrl().newBuilder()

                table.forEach { key, value ->
                    httpBuilder.addQueryParameter(key.checkjstring(), value.tojstring())
                }

                CoerceJavaToLua.coerce(httpBuilder.build())
            }

            this["buildFormBody"] = oneArgFunction { data ->
                CoerceJavaToLua.coerce(
                    FormBody.Builder().apply {
                        data.forEach { key, value -> add(key.checkjstring(), value.tojstring()) }
                    }.build()
                )
            }

            this["buildMultipartBody"] = oneArgFunction { data ->
                CoerceJavaToLua.coerce(
                    MultipartBody.Builder().apply {
                        data.forEach { key, value -> addFormDataPart(key.checkjstring(), value.tojstring()) }
                    }.build()
                )
            }

            this["buildBody"] = twoArgFunction { data, mediaType ->
                CoerceJavaToLua.coerce(
                    data.checkjstring().toRequestBody(mediaType.checkjstring().toMediaType())
                )
            }

            this["buildHeaders"] = oneArgFunction { table ->
                CoerceJavaToLua.coerce(
                    Headers.Builder().apply {
                        table.forEach { key, value -> add(key.checkjstring(), value.tojstring()) }
                    }.build()
                )
            }

            this["buildRequest"] = oneArgFunction { table ->
                CoerceJavaToLua.coerce(table.checktable().toRequest())
            }

            this["newBuilder"] = zeroArgFunction { CoerceJavaToLua.coerce(client.newBuilder()) }

            this["call"] = threeArgFunction { request, onResponse, onFailure ->
                newLuaCall(
                    request = request.checkuserdata(Request::class.java) as Request,
                    onResponse = onResponse,
                    onFailure = onFailure
                )
                NIL
            }

            this["get"] = createMethodFunction("GET")
            this["post"] = createMethodFunction("POST")
            this["put"] = createMethodFunction("PUT")
            this["patch"] = createMethodFunction("PATCH")
            this["delete"] = createMethodFunction("DELETE")

            this.setmetatable(tableOf().apply {
                this[CALL] = twoArgFunction { _, client ->
                    newInstance(client.checkuserdata(OkHttpClient::class.java) as OkHttpClient)
                }
            })
        }
    }
}
