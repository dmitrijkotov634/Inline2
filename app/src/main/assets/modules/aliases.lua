local aliases = inline:getSharedPreferences "aliases"

local function addalias(_, query)
    local args = utils.split(query:getArgs(), " ", 2);

    if #args ~= 2 then
        inline:toast "Invalid arguments"
        query:answer()
        return
    end

    if not inline:getAllCommands():get(args[2]) then
        inline:toast "Command not found"
        query:answer()
        return
    end

    aliases:edit():putString(args[1], args[2]):apply()
    query:answer()
end

local function delalias(_, query)
    aliases:edit():remove(query:getArgs()):apply()
    query:answer()
end

local function aliases_(_, query)
    local result = "List of aliases:\n"

    local iterator = aliases:getAll():entrySet():iterator()
    while iterator:hasNext() do
        local entry = iterator:next()
        result = result .. entry:getKey() .. " -> " .. entry:getValue() .. "\n"
    end

    query:answer(result)
end

local function finder(name, _, callable)
    if callable == nil then
        local command = inline:getAllCommands():get(aliases:getString(name, ""))
        if command then
            return command:getCallable()
        end
    end
end

return function(module)
    module:setCategory "Settings"
    module:registerCommand("addalias", addalias, "Set an alias for a command")
    module:registerCommand("delalias", delalias, "Remove an alias for a command")
    module:registerCommand("aliases", aliases_, "Shows all aliases")
    module:registerCommandFinder(finder)
end
