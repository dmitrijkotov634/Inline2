require "com.wavecat.inline.libs.json"
require "com.wavecat.inline.libs.http"
require "com.wavecat.inline.libs.menu"

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
        end)
    }
end

local function ask(_, query)
    if os.time() - timestamp > preferences:getInt("openai_history_minutes", 5) * 60 then
        history = {}
        timestamp = os.time()
    end

    local headers = http.buildHeaders({ Authorization = "Bearer " .. preferences:getString("openai_key", "") })

    history[#history + 1] = { role = "user", content = query:getArgs() == "" and query:replaceExpression("") or query:getArgs() }
    local request = http.Request.Builder.new():url("https://api.openai.com/v1/chat/completions"):headers(headers):post(
            http.buildBody(
                    json.dump(
                            {
                                model = "gpt-3.5-turbo",
                                messages = history
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
                    history[#history + 1] = result.choices[1].message
                    if result.usage.total_tokens > 3500 then
                        table.remove(history, 1)
                    end
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
    module:setCategory "OpenAI"
    module:registerCommand("ask", ask, "Asks ChatGPT")
    module:registerCommand("clear", clear, "Clear dialog")
    module:registerCommand("history", show, "Show history")
    module:setCategory "ChatGPT"
    module:registerPreferences(getPreferences)
end