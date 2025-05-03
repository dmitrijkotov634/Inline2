require "windows"

local preferences = inline:getSharedPreferences "notes"

local function fnote(input, query)
    windows.createAligned(input, {
        noLimits = true,
        sharedPreferences = preferences
    }, function(ui)
        local inputText = ui.textInput(query:getArgs(), "Text")
        local pasteButton = ui.smallButton("Paste", function()
            if not windows.insertText(inputText:getText()) then
                return inline:toast("Please focus on the desired input")
            end
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
    module:setDescription "Floating tools"

    if (windows.isSupported()) then
        module:setCategory "Notes"
        module:registerCommand("fnote", fnote, "Floating note")
        windows.supportInsert()
    end

    module:saveLazyLoad()
end