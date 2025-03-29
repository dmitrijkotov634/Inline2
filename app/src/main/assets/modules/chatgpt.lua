require "json"
require "http"
require "menu"

local DEFAULT_API_ENDPOINT = "https://api.openai.com/v1/chat/completions"

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
        prefs.textInput("openai_key", "OpenAI Key")
             :setSingleLine(true),
        prefs.spacer(8),
        historyRemove,
        prefs.spacer(8),
        prefs.seekBar("openai_history_minutes", 30)
             :setDefault(5)
             :setOnProgressChanged(function(progress)
            historyRemove:setText("Chat history is automatically deleted after " .. progress .. " minutes")
        end),
        prefs.spacer(8),
        "The OpenAI API is powered by a diverse set of models with different capabilities and price points.\n",
        prefs.spacer(8),
        prefs.textInput("openai_model", "API Model"):setDefault("gpt-4o-mini"),
        prefs.spacer(8),
        prefs.textInput("openai_url", "API Url"):setDefault(DEFAULT_API_ENDPOINT),
        prefs.spacer(8)
    }
end

local function ask(string, onResult)
    if os.time() - timestamp > preferences:getInt("openai_history_minutes", 5) * 60 then
        history = {}
    end

    timestamp = os.time()

    history[#history + 1] = { role = "user", content = string }

    client.post({
        url = preferences:getString("openai_url", DEFAULT_API_ENDPOINT),
        headers = { Authorization = "Bearer " .. preferences:getString("openai_key", "") },
        json = {
            model = preferences:getString("openai_model", "gpt-4o-mini"),
            messages = history
        }
    },
        function(_, _, str)
            local result = json.load(str)

            if result.choices then
                local message = result.choices[1].message

                onResult(message.content)

                history[#history + 1] = {
                    content = message.content,
                    role = message.role
                }
            else
                onResult(result.error.message)
            end
        end,
        function(_, e)
            onResult("Error: " .. e:getMessage())
        end
    )
end

local function getArgs(query)
    local args = query:getArgs()
    if args == "" then
        args = query:replaceExpression("")
    end
    return args
end

local function cask(_, query)
    local args = getArgs(query)

    query:answer("Loading")

    ask(args, function(result)
        menu.create(
            query,
            {
                result,
                "\n",
                {
                    caption = "[√]",
                    action = function(_, q)
                        q:answer(result)
                    end
                }
            }
        )
    end)
end

local function insertText(ui, text)
    local node = inline:getLatestAccessibilityEvent():getSource()

    if ui:isFocused() or node:getPackageName() == inline:getPackageName() then
        return inline:toast("Please focus on the desired input")
    end

    inline:insertText(node, text)
end

local function fask(_, query)
    local args = getArgs(query)

    inline:showFloatingWindow({ noLimits = true }, function(ui)
        local text = ui.text("Loading...")
        text:setMaxLines(15)

        ask(args, function(result)
            text:setText(result)
        end)

        return {
            text,
            ui.spacer(8),
            {
                ui.smallButton("Close", function()
                    ui:close()
                end),

                ui.spacer(8),

                ui.smallButton("Paste", function()
                    insertText(ui, text:getText())
                end)
            }
        }
    end)

    query:answer()
end

local function fgpt(_, query)
    inline:showFloatingWindow({ noLimits = true }, function(ui)
        local input = ui.textInput("Input")
        local text = ui.text("Empty history")

        input:getEditText():setMaxLines(5)
        text:setMaxLines(15)

        input:setText(query:getArgs())

        local askButton

        askButton = ui.smallButton("Ask", function()
            text:setText("Loading...")
            askButton:setEnabled(false)
            ask(input:getText(), function(result)
                text:setText(result)
                askButton:setEnabled(true)
            end)
        end)

        if #history > 0 then
            text:setText(history[#history].content)
        end

        return {
            input,
            ui.spacer(8),
            text,
            ui.spacer(8),
            {
                askButton,
                ui.spacer(8),

                ui.smallButton("Paste", function()
                    insertText(ui, text:getText())
                end),

                ui.spacer(8),

                ui.smallButton("Clear CTX", function()
                    text:setText("Empty history")
                    history = {}
                end),

                ui.spacer(8),

                ui.smallButton("Close", function()
                    ui:close()
                end),
            }
        }
    end)

    query:answer()
end

return function(module)
    module:setCategory "ChatGPT"

    module:registerCommand("ask", cask, "Asks ChatGPT")
    module:registerCommand("clear", clear, "Clear dialog")
    module:registerCommand("history", show, "Show history")

    if (inline:isFloatingWindowSupported()) then
        module:registerCommand("fask", fask, "Asks ChatGPT with Floating result")
        module:registerCommand("fgpt", fgpt, "Floating ChatGPT Window")
    end

    module:registerPreferences(getPreferences)
end