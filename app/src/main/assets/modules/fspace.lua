require "windows"

local function fspace(input, query)
    windows.createAligned(input, { noLimits = true }, function(ui)
        local inputText = ui.textInput("space_" .. query:getArgs(), "Text")
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
    module:setCategory "Editor"

    if (windows.isSupported()) then
        module:registerCommand("fspace", fspace, "Floating text editor")
        windows.supportInsert()
    end

    module:saveLazyLoad()
end