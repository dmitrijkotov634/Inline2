require "com.wavecat.inline.libs.strings"

local actions = {}

local function checkArgs(args, query, count)
    if #args < count then
        inline:toast "Not enough arguments"
        query:answer()
        return false
    end
    return true
end

local function replace(input, query)
    local args = strings:parseArgs(query:getArgs())

    if checkArgs(args, query, 2) then
        actions[#actions + 1] = query:getText():gsub(strings:escape(query:getMatch()), "")
        inline:setText(input, actions[#actions]:gsub(strings:escape(args[1]), args[2]))
    end
end

local function find(input, query)
    query:answer()

    if query:getArgs() ~= "" then
        local index = query:getText():find(strings:escape(query:getArgs())) - 1
        inline:setSelection(input, index, index + strings:length(query:getArgs()))
    end
end

local function repeat_(_, query)
    local args = strings:split(query:getArgs(), " ", 2)

    if checkArgs(args, query, 2) then
        query:answer(args[2]:rep(args[1]))
    end
end

local function undo(input, query)
    if #actions == 0 then
        query:answer()
        return
    end

    inline:setText(input, actions[#actions])
    actions[#actions] = nil
end

return function(module)
    module:registerCommand("replace", replace)
    module:registerCommand("undo", undo)
    module:registerCommand("find", find)
    module:registerCommand("repeat", repeat_)
end