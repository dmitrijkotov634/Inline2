@file:Suppress("ClassName", "unused")

package com.wavecat.inline.libs

import android.os.Handler
import android.os.Looper
import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.extensions.oneArgFunction
import com.wavecat.inline.extensions.threeArgFunction
import com.wavecat.inline.extensions.twoArgFunction
import com.wavecat.inline.extensions.zeroArgFunction
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
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

            this["newBuilder"] = zeroArgFunction { CoerceJavaToLua.coerce(client.newBuilder()) }

            this["call"] = threeArgFunction { request, onResponse, onFailure ->
                val handler = Handler(Looper.getMainLooper())

                client.newCall(request.checkuserdata(Request::class.java) as Request)
                    .enqueue(object : Callback {
                        override fun onFailure(call: okhttp3.Call, e: IOException) {
                            if (!onFailure.isnil())
                                handler.post {
                                    onFailure.call(
                                        CoerceJavaToLua.coerce(call),
                                        CoerceJavaToLua.coerce(e)
                                    )
                                }
                        }

                        override fun onResponse(call: okhttp3.Call, response: Response) {
                            val bytes: LuaValue = valueOf(response.body?.bytes() ?: byteArrayOf())

                            if (!onResponse.isnil()) handler.post {
                                onResponse.call(
                                    CoerceJavaToLua.coerce(call),
                                    CoerceJavaToLua.coerce(response),
                                    bytes
                                )
                            }
                        }
                    })

                NIL
            }

            this.setmetatable(tableOf().apply {
                this[CALL] = twoArgFunction { _, client ->
                    newInstance(client.checkuserdata(OkHttpClient::class.java) as OkHttpClient)
                }
            })
        }
    }
}
