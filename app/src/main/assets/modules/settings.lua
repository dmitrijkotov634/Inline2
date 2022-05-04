require "com.wavecat.inline.libs.strings"

local aliases = inline:getSharedPreferences "aliases"

local function help(_, query)
    local categories = {}
    local iterator = inline:getAllCommands():entrySet():iterator()
    while iterator:hasNext() do
        local entry = iterator:next()
        local category = entry:getValue():getCategory()
        if not category then
            category = "Other"
        end
        if not categories[category] then
            categories[category] = {}
        end
        categories[category][entry:getKey()] = entry:getValue():getDescription()
    end
    local result = ""
    if query:getArgs() == "" then
        for name, category in pairs(categories) do
            result = result .. "• " .. name .. ": "
            for cname, _ in pairs(category) do
                result = result .. cname .. ", "
            end
            result = result:sub(1, #result - 2) .. "\n"
        end
    else
        local category = categories[query:getArgs()]
        if category then
            result = query:getArgs() .. ": \n"
            for name, description in pairs(category) do
                result = result .. "• " .. name .. (description == "" and "" or " : " .. description) .. "\n"
            end
        else
            result = "Category not found"
        end
    end
    query:answer(result)
end

local function addalias(_, query)
    local args = strings.split(query:getArgs(), " ", 2);

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

local function reload(_, query)
    inline:createEnvironment()
    query:answer()
end

return function(module)
    module:setCategory "Settings"
    module:registerCommand("help", help, "Displays help")
    module:registerCommand("addalias", addalias, "Set an alias for a command")
    module:registerCommand("delalias", delalias, "Remove an alias for a command")
    module:registerCommand("reload", reload, "Recreate environment, initializes modules")
end