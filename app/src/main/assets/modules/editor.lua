require "com.wavecat.inline.libs.utf8"
require "com.wavecat.inline.libs.utils"

local actions = {}

local function replace(input, query, args)
    actions[#actions + 1] = query:replaceExpression("")
    inline:setText(input, actions[#actions]:gsub(utils.escape(args[1]), args[2]))
end

local function find(input, query)
    query:answer()
    local index = query:getText():find(utils.escape(query:getArgs())) - 1
    inline:setSelection(input, index, index + utf8.len(query:getArgs()))
end

local function repeat_(_, query)
    local args = utils.split(query:getArgs(), " ", 2)
    if #args < 2 then
        query:answer "Not enough arguments"
        return
    end
    query:answer(args[2]:rep(args[1]))
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

    actions[#actions + 1] = query:replaceExpression("")

    inline:setText(input, actions[#actions]:gsub(
            utf8.charpattern,
            replacements
    ))
end

local function erase(input, query)
    actions[#actions + 1] = query:replaceExpression("")
    inline:setText(input, "")
end

return function(module)
    module:setCategory "Editor"
    module:registerCommand("replace", utils.command(replace, 2), "Changes text in which all occurrences of a substring are replaced by another substring")
    module:registerCommand("undo", undo, "Returns text to the old state")
    module:registerCommand("find", utils.hasArgs(find), "Selects the found fragment of text")
    module:registerCommand("repeat", repeat_, "Returns a string repeated the desired number of times")
    module:registerCommand("invert", invert, "Changes some characters to similar ones")
    module:registerCommand("erase", erase, "Erases all text")
end