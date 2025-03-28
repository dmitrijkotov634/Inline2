require "utils"
require "menu"

local preferences = inline:getSharedPreferences "binder2"
local enabled = inline:getDefaultSharedPreferences():getBoolean("binder", true)

local Query = luajava.bindClass "com.wavecat.inline.service.Query"

local function bind(_, query)
    local args = utils.split(query:getArgs(), " ", 2)
    if #args < 2 then
        inline:toast "Invalid arguments"
        return
    end
    local data = utils.split(args[2], " ", 2)
    if not inline:getAllCommands():containsKey(data[1]) then
        inline:toast "Command not found"
        return
    end
    preferences:edit():putString(utils.escape(args[1]), args[2]):apply()
    query:answer()
end

local function unbind(_, query)
    preferences:edit():remove(utils.escape(query:getArgs())):apply()
    query:answer()
end

local function unbindall(_, query)
    preferences:edit():clear():apply()
    query:answer()
end

local function activate(_, query)
    enabled = not enabled
    inline:getDefaultSharedPreferences():edit():putBoolean("binder", enabled):apply()
    query:answer()
end

local function echo(_, query)
    query:answer(query:getArgs())
end

local function binder(input)
    if enabled then
        local text = input:getText()
        if text ~= nil and text.toString ~= nil then
            text = text:toString()
            local iterator = preferences:getAll():entrySet():iterator()
            while iterator:hasNext() do
                local entry = iterator:next()
                text = text:gsub(
                    entry:getKey(),
                    function(expression)
                        local data = utils.split(entry:getValue(), " ", 2)
                        local command = inline:getAllCommands():get(data[1])
                        if not command then
                            return nil
                        end
                        local callable = command:getCallable()
                        local query = Query.new(input, input:getText(), expression, data[2] or "")
                        callable(input, query)
                        return query:getText()
                    end
                )
            end
        end
    end
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
        result[#result + 1] = {
            caption = "[X]",
            action = function(_, q)
                menu.create(q, {
                    "Unbind ",
                    entry:getKey(),
                    "? ",
                    { caption = "[Yes]", action = function(_, queryYes)
                        preferences:edit():remove(entry:getKey()):apply()
                        binds(_, queryYes)
                    end },
                    " ",
                    { caption = "[No]", action = binds }
                }, binds)
            end
        }
        result[#result + 1] = " " .. entry:getKey() .. " -> " .. entry:getValue() .. "\n"
    end
    menu.create(query, result, binds)
end

local function getPreferences(prefs)
    return {
        prefs.checkBox("binder", "Enable binder"),
        prefs.spacer(8),
        prefs.button("Unbind All", function()
            prefs:cancel()
            prefs:create("Unbind All?", function()
                return {
                    "This button will erase all your binds",
                    prefs.spacer(8),
                    {
                        prefs.button("Yes", function()
                            preferences:edit():clear():apply()
                            prefs:cancel()
                        end),
                        prefs.spacer(8),
                        prefs.button("No", function()
                            prefs:cancel()
                        end)
                    }
                }
            end)
        end)
    }
end

return function(module)
    module:setCategory "Binder"
    module:registerCommand("bind", bind, "Creates a macro")
    module:registerCommand("unbind", unbind, "Deletes a macro")
    module:registerCommand("unbindall", unbindall, "Removes all macros")
    module:registerCommand("binder", activate, "Toggles the state of the processor")
    module:registerCommand("echo", echo, "Prints the arguments passed to the command")
    module:registerCommand("binds", binds, "Bindings manager")
    module:registerPreferences(getPreferences)
    module:registerWatcher(binder)
end