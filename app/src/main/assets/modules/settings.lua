require "com.wavecat.inline.libs.utils"

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
        result = result .. "Help for Inline\nFor more help on how to use a command, type help <category name>\n"
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
            result = "Help for " .. query:getArgs() .. ": \n"
            for name, description in pairs(category) do
                result = result .. "• " .. name .. (description == "" and "" or " : " .. description) .. "\n"
            end
        else
            result = "Category not found"
        end
    end
    query:answer(result)
end

local function reload(_, query)
    inline:createEnvironment()
    query:answer()
end

return function(module)
    module:setCategory "Settings"
    module:registerCommand("help", help, "Displays help")
    module:registerCommand("reload", reload, "Recreate environment, initializes modules")
end