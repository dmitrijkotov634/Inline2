require "utils"
require "menu"
require "colorama"

local preferences = inline:getSharedPreferences "binder2"
local enabled = inline:getDefaultSharedPreferences():getBoolean("binder", true)

local Query = luajava.bindClass "com.wavecat.inline.service.commands.Query"

local function bind(_, query)
    local args = utils.split(query:getArgs(), " ", 2)

    if #args < 2 then
        return inline:toast("Invalid arguments")
    end

    local rawValue = args[2]
    local key = args[1]

    if not rawValue:find("^!end%s") then
        key = utils.escape(args[1])
    end

    local cleanValue = rawValue:gsub("^!end%s*", "")

    local data = utils.split(cleanValue, " ", 2)
    if not inline:getAllCommands():containsKey(data[1]) then
        return inline:toast("Command not found")
    end

    preferences:edit():putString(key, args[2]):apply()
    query:answer()
end

local function unbind(_, query)
    preferences:edit()
               :remove(utils.escape(query:getArgs()))
               :remove(query:getArgs())
               :apply()

    query:answer()
end

local function activate(_, query)
    enabled = not enabled
    inline:getDefaultSharedPreferences():edit():putBoolean("binder", enabled):apply()
    menu.create(query, { enabled and "Enabled" or "Disabled" })
end

local function echoHtml(_, query)
    colorama.of(query):answer(query:getArgs())
end

local function echo(_, query)
    query:answer(query:getArgs())
end

local function executeBinding(input, text, expression, bindingValue)
    local data = utils.split(bindingValue, " ", 2)
    local command = inline:getAllCommands():get(data[1])
    if not command then
        return nil
    end
    local callable = command:getCallable()
    local query = Query.new(input, text, expression, data[2] or "")
    callable(input, query)
    return query:getText()
end

local function binder(input)
    if enabled then
        local text = input:getText()
        if text ~= nil and text.toString ~= nil then
            text = text:toString()
            local iterator = preferences:getAll():entrySet():iterator()
            while iterator:hasNext() do
                local entry = iterator:next()
                local key = entry:getKey()
                local value = entry:getValue()
                if value:find("^!end%s") then
                    if text:sub(-#key) == key then
                        local cleanValue = value:gsub("^!end%s*", "")
                        executeBinding(input, text, key, cleanValue)
                        break
                    end
                else
                    text = text:gsub(key, function(expression)
                        return executeBinding(input, input:getText(), expression, value)
                    end)
                end
            end
        end
    end
end

local function createUnbindMenuItem(_, key, callback)
    return {
        caption = "[X]",
        action = function(_, q)
            menu.create(q, {
                "Unbind ",
                key,
                "? ",
                { caption = "[Yes]", action = function(_, queryYes)
                    preferences:edit():remove(key):apply()
                    callback(_, queryYes)
                end },
                " ",
                { caption = "[No]", action = callback }
            }, callback)
        end
    }
end

local function binds(_, query)
    enabled = false
    local iterator = preferences:getAll():entrySet():iterator()
    local result = {
        {
            caption = "[X]",
            action = function(_, queryExit)
                queryExit:answer()
                enabled = true
            end
        },
        " List of bindings:\n\n"
    }
    while iterator:hasNext() do
        local entry = iterator:next()
        table.insert(result, createUnbindMenuItem(query, entry:getKey(), binds))
        table.insert(result, " " .. entry:getKey() .. " -> " .. entry:getValue() .. "\n")
    end
    menu.create(query, result)
end

local function getPreferences(prefs)
    return {
        prefs.checkBox("binder", "Enable binder"),
        prefs.spacer(12),
        prefs.smallButton("Unbind All", function()
            prefs:cancel()
            prefs:create("Unbind All?", function()
                return {
                    "This button will erase all your binds",
                    prefs.spacer(16),
                    {
                        prefs.smallButton("Yes", function()
                            preferences:edit():clear():apply()
                            prefs:cancel()
                        end),
                        prefs.spacer(14),
                        prefs.smallButton("No", function()
                            prefs:cancel()
                        end)
                    },
                    prefs.spacer(12),
                }
            end)
        end),
        prefs.spacer(12),
    }
end

return function(module)
    module:setCategory "Binder"
    module:setDescription "Create, manage, and control command bindings and macros"
    module:registerCommand("bind", bind, "Creates a macro")
    module:registerCommand("unbind", unbind, "Deletes a macro")
    module:registerCommand("binder", activate, "Toggles the state of the processor")
    module:registerCommand("echo", echo, "Prints the arguments passed to the command")
    module:registerCommand("echohtml", echoHtml, "Prints the arguments passed to the command with formatting")
    module:registerCommand("binds", binds, "Bindings manager")
    module:registerPreferences(getPreferences)
    module:registerWatcher(binder)
end