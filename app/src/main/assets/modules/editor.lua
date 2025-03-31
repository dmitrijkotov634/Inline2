require "iutf8"
require "utils"

local Gravity = luajava.bindClass("android.view.Gravity")

local latestInput

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
        query:answer "Not enough arguments"
        return
    end
    query:answer(args[2]:rep(args[1]))
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
        return utf8.isLower(c) and utf8.upper(c) or utf8.lower(c)
    end)

    inline:setText(input, text)
end

local function fspace(input, query)
    local rect = inline:getBoundsInScreen(input)

    inline:showFloatingWindow({
        noLimits = true,
        positionX = rect.left,
        positionY = inline:getScreenHeight() - rect.top,
        gravity = bit32.bor(Gravity.BOTTOM, Gravity.LEFT)
    }, function(ui)
        local inputText = ui.textInput("space_" .. query:getArgs(), "Text")
        local pasteButton = ui.smallButton("Paste", function()
            if latestInput == nil or ui:isFocused() or latestInput:getPackageName() == inline:getPackageName() then
                return inline:toast("Please focus on the desired input")
            end

            inline:insertText(latestInput, inputText:getText())
        end)

        ui.onFocusChanged = function(isFocused)
            pasteButton:setEnabled(not isFocused)
        end

        inputText:setText(query:replaceExpression(""))
        inputText:getEditText():setMaxLines(15)

        return {
            inputText,
            ui.spacer(8),
            {
                pasteButton,
                ui.spacer(8),
                ui.smallButton("Close", function()
                    ui:close()
                end)
            }
        }
    end)

    query:answer()
end

return function(module)
    module:setCategory "Editor"

    module:registerCommand("replace", utils.command(replace, 2), "Changes text in which all occurrences of a substring are replaced by another substring")
    module:registerCommand("find", utils.hasArgs(find), "Selects the found fragment of text")
    module:registerCommand("repeat", repeat_, "Returns a string repeated the desired number of times")
    module:registerCommand("invert", invert, "Changes some characters to similar ones")
    module:registerCommand("erase", erase, "Erases all text")
    module:registerCommand("paste", paste, "Pastes text from clipboard")
    module:registerCommand("toggle", toggleCase, "Changes lowercase letters to uppercase and vice versa.")

    if (inline:isFloatingWindowSupported()) then
        module:registerCommand("fspace", fspace, "Floating text editor")
        module:registerWatcher(function(input)
            latestInput = input
        end)
    end
end