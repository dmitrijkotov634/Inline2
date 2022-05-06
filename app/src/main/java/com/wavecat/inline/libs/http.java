package com.wavecat.inline.libs;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@SuppressWarnings("unused")
public class http extends TwoArgFunction {

    private static OkHttpClient client;

    @Override
    public LuaValue call(LuaValue name, LuaValue env) {
        LuaValue library = tableOf();

        library.set("Request", CoerceJavaToLua.coerce(Request.class));
        library.set("buildUrl", new buildUrl());
        library.set("buildFormBody", new buildFormBody());
        library.set("buildMultipartBody", new buildMultipartBody());
        library.set("buildBody", new buildBody());
        library.set("buildHeaders", new buildHeaders());
        library.set("call", new call_());

        env.set("http", library);
        env.get("package").get("loaded").set("http", library);

        if (client == null)
            client = new OkHttpClient();

        return library;
    }

    static class buildUrl extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue url, LuaValue table) {
            HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(url.checkjstring())).newBuilder();

            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = table.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                httpBuilder.addQueryParameter(k.checkjstring(), n.arg(2).tojstring());
            }

            return CoerceJavaToLua.coerce(httpBuilder.build());
        }
    }

    static class buildFormBody extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue data) {
            FormBody.Builder builder = new FormBody.Builder();

            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = data.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                builder.add(k.checkjstring(), n.arg(2).tojstring());
            }

            return CoerceJavaToLua.coerce(data);
        }
    }

    static class buildMultipartBody extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue data) {
            MultipartBody.Builder builder = new MultipartBody.Builder();

            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = data.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                builder.addFormDataPart(k.checkjstring(), n.arg(2).tojstring());
            }

            return CoerceJavaToLua.coerce(data);
        }
    }

    static class buildBody extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue data, LuaValue mediaType) {
            return CoerceJavaToLua.coerce(
                    RequestBody.create(data.checkjstring(), MediaType.parse(mediaType.checkjstring())));
        }
    }

    static class buildHeaders extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue table) {
            Headers.Builder builder = new Headers.Builder();

            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = table.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                builder.add(k.checkjstring(), n.arg(2).tojstring());
            }

            return CoerceJavaToLua.coerce(builder.build());
        }
    }

    static class call_ extends ThreeArgFunction {
        @Override
        public LuaValue call(LuaValue request, LuaValue onResponse, LuaValue onFailure) {
            client.newCall((Request) CoerceLuaToJava.coerce(request, Request.class)).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (!onFailure.isnil())
                        new Handler(Looper.getMainLooper()).post(() ->
                                onFailure.call(CoerceJavaToLua.coerce(call), CoerceJavaToLua.coerce(e)));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!onResponse.isnil()) {
                        ResponseBody responseBody = response.body();

                        LuaValue bytes = valueOf(responseBody == null ? new byte[]{} : responseBody.bytes());
                        new Handler(Looper.getMainLooper()).post(() ->
                                onResponse.call(CoerceJavaToLua.coerce(call), CoerceJavaToLua.coerce(response), bytes));
                    }
                }
            });

            return NIL;
        }
    }
}
