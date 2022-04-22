package com.wavecat.inline.libs;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("unused")
public class http extends TwoArgFunction {

    private static OkHttpClient client;

    private static class RequestNetworkLib {
        public static Request.Builder newRequestBuilder() {
            return new Request.Builder();
        }

        public static HttpUrl buildUrl(String url, LuaValue table) {
            HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();

            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = table.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                httpBuilder.addQueryParameter(k.checkjstring(), n.arg(2).tojstring());
            }

            return httpBuilder.build();
        }

        public static FormBody buildBody(LuaValue table) {
            FormBody.Builder builder = new FormBody.Builder();

            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = table.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                builder.add(k.checkjstring(), n.arg(2).tojstring());
            }

            return builder.build();
        }

        public static Headers buildHeaders(LuaTable table) {
            Headers.Builder builder = new Headers.Builder();

            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = table.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                builder.add(k.checkjstring(), n.arg(2).tojstring());
            }

            return builder.build();
        }

        public static void call(Request request, LuaValue onFailure, LuaValue onResponse) {
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (onFailure != null)
                        new Handler(Looper.getMainLooper()).post(() ->
                                onFailure.call(CoerceJavaToLua.coerce(call), CoerceJavaToLua.coerce(e)));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if (onResponse != null)
                        new Handler(Looper.getMainLooper()).post(() ->
                                onResponse.call(CoerceJavaToLua.coerce(call), CoerceJavaToLua.coerce(response)));
                }
            });
        }

        public static void call(Request request, LuaValue onResponse) {
            call(request, null, onResponse);
        }
    }

    @Override
    public LuaValue call(LuaValue name, LuaValue env) {
        LuaValue lib = CoerceJavaToLua.coerce(RequestNetworkLib.class);

        env.set("http", lib);
        env.get("package").get("loaded").set("http", lib);

        if (client == null)
            client = new OkHttpClient();

        return lib;
    }
}
