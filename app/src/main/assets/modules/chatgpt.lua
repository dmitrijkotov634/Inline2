require "json"
require "http"
require "menu"

local TimeUnit = luajava.bindClass "java.util.concurrent.TimeUnit"

local preferences = inline:getDefaultSharedPreferences()

local client = http(
    http.newBuilder()
        :readTimeout(60, TimeUnit.SECONDS)
        :writeTimeout(60, TimeUnit.SECONDS)
        :callTimeout(60, TimeUnit.SECONDS
    )   :build()
)

local history = {}
local timestamp = os.time()
local cursor = 1

local function trim(s)
    return s:match "^%s*(.-)%s*$"
end

local function clear(_, query)
    history = {}
    query:answer()
end

local function show(_, query)
    local result = {}

    for i = cursor, cursor + 1 do
        if history[i] then
            result[#result + 1] = history[i].role .. ": " .. history[i].content .. "\n"
        end
    end

    result[#result + 1] = "\n"
    result[#result + 1] = {
        caption = "[X]",
        action = function(_, q)
            cursor = 1
            q:answer()
        end
    }

    if cursor > 2 then
        result[#result + 1] = " "
        result[#result + 1] = {
            caption = "[<]",
            action = function(_, q)
                cursor = cursor - 2
                show(_, q)
            end
        }
    end

    if cursor < #history - 1 then
        result[#result + 1] = " "
        result[#result + 1] = {
            caption = "[>]",
            action = function(_, q)
                cursor = cursor + 2
                show(_, q)
            end
        }
    end

    if #history > 3 then
        result[#result + 1] = " "
        result[#result + 1] = {
            caption = "[>>]",
            action = function(_, q)
                cursor = #history - #history % 2 - 1
                show(_, q)
            end
        }
    end

    menu.create(query, result, show)
end

local function getPreferences(prefs)
    local historyRemove = prefs.text("Chat history is automatically deleted after "
        .. preferences:getInt("openai_history_minutes", 5) .. " minutes")

    return {
        prefs.textInput("openai_key", "OpenAI Key"),
        historyRemove,
        prefs.seekBar("openai_history_minutes", 30)
             :setDefault(5)
             :setOnProgressChanged(function(progress)
            historyRemove:setText("Chat history is automatically deleted after " .. progress .. " minutes")
        end),
        "The OpenAI API is powered by a diverse set of models with different capabilities and price points.\n",
        prefs.spinner("openai_model", { "gpt-4.5-preview", "gpt-4o", "gpt-4o-mini", "o3-mini" })
    }
end

local function ask(_, query)
    if os.time() - timestamp > preferences:getInt("openai_history_minutes", 5) * 60 then
        history = {}
        timestamp = os.time()
    end

    local headers = http.buildHeaders({
        Authorization = "Bearer " .. preferences:getString("openai_key", "")
    })

    history[#history + 1] = {
        role = "user", content = query:getArgs() == "" and query:replaceExpression("") or query:getArgs()
    }

    local request = http.Request.Builder.new():url("https://api.openai.com/v1/chat/completions"):headers(headers):post(
        http.buildBody(
            json.dump({
                model = preferences:getString("openai_model", "gpt-4o-mini"),
                messages = history
            }),
            "application/json"
        )
    )                   :build()

    query:answer "Loading"

    client.call(
        request,
        function(_, _, string)
            local result = json.load(string)

            if result.choices then
                result.choices[1].message.content = trim(result.choices[1].message.content)
                menu.create(
                    query,
                    {
                        result.choices[1].message.content,
                        "\n",
                        {
                            caption = "[√]",
                            action = function(_, q)
                                q:answer(result.choices[1].message.content)
                            end
                        }
                    }
                )

                history[#history + 1] = {
                    content = result.choices[1].message.content,
                    role = result.choices[1].message.role
                }
            else
                query:answer(result.error.message)
            end
        end,
        function(_, e)
            query:answer("Error: " .. e:getMessage())
        end
    )
end

return function(module)
    module:setCategory "ChatGPT"

    module:registerCommand("ask", ask, "Asks ChatGPT")
    module:registerCommand("clear", clear, "Clear dialog")
    module:registerCommand("history", show, "Show history")

    module:registerPreferences(getPreferences)
end