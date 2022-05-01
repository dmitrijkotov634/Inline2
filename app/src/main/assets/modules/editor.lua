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
        actions[#actions + 1] = query:replaceExpression("")
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
    actions[#actions + 1] = query:replaceExpression("")

    inline:setText(input, actions[#actions]:gsub(
            "[\1-\x7F\xC2-\xF4][\x80-\xBF]*",
            replacements
    ))
end

local function erase(input, query)
    actions[#actions + 1] = query:replaceExpression("")
    inline:setText(input, "")
end

return function(module)
    module:setCategory "Editor"
    module:registerCommand("replace", replace, "Changes text in which all occurrences of a substring are replaced by another substring")
    module:registerCommand("undo", undo, "Returns text to the old state")
    module:registerCommand("find", find, "Selects the found fragment of text")
    module:registerCommand("repeat", repeat_, "Returns a string repeated the desired number of times")
    module:registerCommand("invert", invert, "Changes some characters to similar ones")
    module:registerCommand("erase", erase, "Erases all text")
end