require "iutf8"
require "utils"

local function replace(input, query, args)
    inline:setText(input, query:replaceExpression(""):gsub(utils.escape(args[1]), args[2]))
end

local function find(input, query)
    query:answer()
    local index = query:getText():find(utils.escape(query:getArgs())) - 1
    inline:setSelection(input, index, index + utf8.len(query:getArgs()))
end

local function repeat_(_, query)
    local args = utils.split(query:getArgs(), " ", 2)
    if #args < 2 then
        return query:answer "Not enough arguments"

    end
    local count = tonumber(args[1])
    if count == nil then
        return query:answer "Number of repetitions must be a number"
    end
    query:answer(args[2]:rep(count))
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

    inline:setText(input, query:replaceExpression(""):gsub(
        utf8.charpattern,
        replacements
    ))
end

local function erase(input, _)
    inline:setText(input, "")
end

local function paste(input, query)
    query:answer()
    inline:paste(input)
end

local function toggleCase(input, query)
    local text = query:replaceExpression(""):gsub(utf8.charpattern, function(c)
        return utf8.isLower(c) and string.upper(c) or string.upper(c)
    end)

    inline:setText(input, text)
end

return function(module)
    module:setCategory "Editor"
    module:setDescription "Text editing and transformation tools"
    module:registerCommand("replace", utils.command(replace, 2), "Changes text in which all occurrences of a substring are replaced by another substring")
    module:registerCommand("find", utils.hasArgs(find), "Selects the found fragment of text")
    module:registerCommand("repeat", repeat_, "Returns a string repeated the desired number of times")
    module:registerCommand("invert", invert, "Changes some characters to similar ones")
    module:registerCommand("erase", erase, "Erases all text")
    module:registerCommand("paste", paste, "Pastes text from clipboard")
    module:registerCommand("toggle", toggleCase, "Changes lowercase letters to uppercase and vice versa.")
    module:saveLazyLoad()
end