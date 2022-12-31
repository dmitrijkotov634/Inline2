require "com.wavecat.inline.libs.utils"

local preferences = inline:getSharedPreferences "binder2"
local enabled = inline:getDefaultSharedPreferences():getBoolean("binder", false)

local Query = luajava.bindClass "com.wavecat.inline.Query"

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

local function binds(_, query)
    local iterator = preferences:getAll():entrySet():iterator()
    local result = ""
    while iterator:hasNext() do
        local entry = iterator:next()
        result = result .. entry:getKey() .. " -> " .. entry:getValue() .. "\n"
    end
    if enabled then
        enabled = false
        inline:toast "Binder disabled"
    end
    query:answer(result)
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

return function(module)
    module:setCategory "Binder"
    module:registerCommand("bind", bind, "Creates a macro")
    module:registerCommand("unbind", unbind, "Deletes a macro")
    module:registerCommand("unbindall", unbindall, "Removes all macros")
    module:registerCommand("binder", activate, "Toggles the state of the processor")
    module:registerCommand("binds", binds, "Outputs all macros")
    module:registerCommand("echo", echo, "Prints the arguments passed to the command")
    module:registerWatcher(binder)
end