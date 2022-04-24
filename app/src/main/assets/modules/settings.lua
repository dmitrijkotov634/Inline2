require "com.wavecat.inline.libs.strings"

local aliases = inline:getSharedPreferences "aliases"

local dispatcher

local function addalias(_, query)
    local args = strings.split(query:getArgs(), " ", 2);

    if #args ~= 2 then
        inline:toast "Invalid arguments"
        query:answer()
        return
    end

    local command = dispatcher:getCommand(args[2])

    if command == nil then
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

return function(module)
    module:registerCommand("addalias", addalias)
    module:registerCommand("delalias", delalias)

    dispatcher = module
end