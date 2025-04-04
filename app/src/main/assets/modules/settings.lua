require "utils"

local function help(_, query)
    local categories = {}
    local iterator = inline:getAllCommands():entrySet():iterator()

    while iterator:hasNext() do
        local entry = iterator:next()
        local cmd_name = entry:getKey()
        local cmd_obj = entry:getValue()
        local category = cmd_obj:getCategory() or "Other"

        if not categories[category] then
            categories[category] = {}
        end

        categories[category][cmd_name] = cmd_obj:getDescription()
    end

    local args = query:getArgs()
    if args == "" then
        local result = { "Help for Inline\nFor more help, type help <category>" }
        for category, commands in pairs(categories) do
            local cmd_list = {}
            for cmd_name in pairs(commands) do
                table.insert(cmd_list, cmd_name)
            end
            table.insert(result, "• " .. category .. ": " .. table.concat(cmd_list, ", "))
        end
        query:answer(table.concat(result, "\n"))
    else
        local lower_args = args:lower()
        local found_category

        for category in pairs(categories) do
            local lower_category = category:lower()
            if lower_category == lower_args or lower_category:find(lower_args, 1, true) == 1 then
                found_category = category
                break
            end
        end

        if found_category then
            local result = { "Help for " .. found_category .. ":" }
            for name, desc in pairs(categories[found_category]) do
                table.insert(result, "• " .. name .. (desc ~= "" and (" : " .. desc) or ""))
            end
            query:answer(table.concat(result, "\n"))
        else
            query:answer("Category not found")
        end
    end
end

local function reload(_, query)
    inline:createEnvironment()
    query:answer()
end

return function(module)
    module:setCategory "Settings"
    module:registerCommand("help", help, "Displays help")
    module:registerCommand("reload", reload, "Recreate environment, initializes modules")
    module:saveLazyLoad()
end