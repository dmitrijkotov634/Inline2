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

local function gpt3(_, query)
    local headers = http.buildHeaders({ Authorization = "Bearer " .. preferences:getString("openai_key", "") })
    local request = http.Request.Builder.new():url("https://api.openai.com/v1/completions")
                        :headers(headers)
                        :post(
            http.buildBody(
                    json.dump(
                            {
                                model = preferences:getString("openai_gpt3_model", "text-davinci-003"),
                                prompt = query:getArgs() == "" and query:replaceExpression("") or query:getArgs(),
                                max_tokens = 2048,
                                temperature = 0.7
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
                query:answer(query:getArgs() .. (result.choices and result.choices[1].text or result.error.message))
            end,
            function(_, e)
                query:answer("Error: " .. e:getMessage())
            end
    )
end

local function getPreferences(prefs)
    return {
        prefs.textInput("openai_key", "OpenAI Key"),
        "The OpenAI API is powered by a diverse set of models with different capabilities and price points.\n",
        prefs.spinner("openai_gpt3_model",
                { "text-davinci-003", "text-davinci-002", "text-curie-001", "text-babbage-001", "text-ada-001" }),
    }
end

return function(module)
    module:setCategory "OpenAI"
    module:registerCommand("gpt3", gpt3, "Makes a request to GPT-3")
    module:setCategory "GPT-3"
    module:registerPreferences(getPreferences)
end