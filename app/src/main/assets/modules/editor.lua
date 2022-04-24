require "com.wavecat.inline.libs.strings"

local actions = {}
local replacements = {
    a = "а", A = "А",
    C = "С", c = "с",
    e = "е", E = "Е",
    o = "о", O = "О",
    P = "Р", p = "р",
    x = "х", X = "Х",
    B = "В",
    H = "Н",
    K = "К",
    M = "М",
    T = "Т",
}

for k, v in pairs(replacements) do
    replacements[v] = k
end

local function checkArgs(args, query, count)
    if #args < count then
        inline:toast "Not enough arguments"
        query:answer()
        return false
    end
    return true
end

local function replace(input, query)
    local args = strings.parseArgs(query:getArgs())

    if checkArgs(args, query, 2) then
        actions[#actions + 1] = query:getText():gsub(strings.escape(query:getMatch()), "")
        inline:setText(input, actions[#actions]:gsub(strings.escape(args[1]), args[2]))
    end
end

local function find(input, query)
    query:answer()

    if query:getArgs() ~= "" then
        local index = query:getText():find(strings.escape(query:getArgs())) - 1
        inline:setSelection(input, index, index + strings.length(query:getArgs()))
    end
end

local function repeat_(_, query)
    local args = strings.split(query:getArgs(), " ", 2)

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

local function invert(input, query)
    actions[#actions + 1] = query:getText():gsub(strings.escape(query:getMatch()), "")

    inline:setText(input, actions[#actions]:gsub(
            "[\1-\x7F\xC2-\xF4][\x80-\xBF]*",
            replacements
    ))
end

return function(module)
    module:registerCommand("replace", replace)
    module:registerCommand("undo", undo)
    module:registerCommand("find", find)
    module:registerCommand("repeat", repeat_)
    module:registerCommand("invert", invert)
end