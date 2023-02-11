require "com.wavecat.inline.libs.json"
require "com.wavecat.inline.libs.http"

local TimeUnit = luajava.bindClass "java.util.concurrent.TimeUnit"

local preferences = inline:getDefaultSharedPreferences()

local client = http(
        http.newBuilder()
            :readTimeout(60, TimeUnit.SECONDS)
            :writeTimeout(60, TimeUnit.SECONDS)
            :callTimeout(60, TimeUnit.SECONDS)
            :build()
)

local function setopenaikey(_, query)
    query:answer()

    preferences:edit()
               :putString("openai_key", query:getArgs())
               :apply()
end

local function gpt3(_, query)
    local headers = http.buildHeaders({ Authorization = "Bearer " .. preferences:getString("openai_key", "") })
    local request = http.Request.Builder.new():url("https://api.openai.com/v1/completions")
                        :headers(headers)
                        :post(
            http.buildBody(
                    json.dump(
                            {
                                model = "text-davinci-003",
                                prompt = query:getArgs(),
                                max_tokens = 2048,
                                temperature = 1
                            }
                    ),
                    "application/json"
            )
    )                   :build()

    query:answer "Loading"

    client.call(
            request,
            function(_, _, string)
                local result = json.load(string)
                query:answer(query:getArgs() .. (result.choices and result.choices[1].text or "Error"))
            end,
            function(_, e)
                query:answer("Error: " .. e:getMessage())
            end
    )
end

return function(module)
    module:setCategory "GPT3"
    module:registerCommand("set_openai_key", setopenaikey, "Set OpenAI key")
    module:registerCommand("gpt3", gpt3, "Makes a request to GPT-3")
end