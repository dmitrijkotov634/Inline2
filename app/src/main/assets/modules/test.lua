local function createUiMenu(ui)
    local callback = function(v)
        inline:toast(tostring(v))
    end

    return {
        ui.button("Button", callback),
        ui.smallButton("SmallButton", callback),
        ui.checkBox("CheckBox"),
        ui.seekBar(callback, 20),
        ui.spinner({ "Spinner", "Spinner", "Spinner" }, callback),
        ui.text("Text"),
        ui.textInput("TextInput", callback),
        "In Horizontal:",
        {
            ui.button("Button", callback),
            ui.smallButton("SmallButton", callback),
            ui.checkBox("CheckBox"),
            ui.seekBar(callback, 20),
            ui.spinner({ "Spinner", "Spinner", "Spinner" }, callback),
            ui.text("Text"),
            ui.textInput("TextInput", callback),
        }
    }
end

local function createFloatingWindow(input, query)
    query:answer()
    inline:showFloatingWindow({
        noLimits = true,
    }, function(ui)
        local xText = ui.text("")
        local yText = ui.text("")
        local leftText = ui.text("")
        local topText = ui.text("")
        local rightText = ui.text("")
        local bottomText = ui.text("")
        ui.onMove = function(x, y)
            xText:setText(tostring(x))
            yText:setText(tostring(y))
            input:refresh()
            local rect = inline:getBoundsInScreen(input)
            leftText:setText(tostring(rect.left))
            rightText:setText(tostring(rect.right))
            bottomText:setText(tostring(rect.bottom))
            topText:setText(tostring(rect.top))
        end
        return { { "X: ", xText, " Y: ", yText },
                 { "Left: ", leftText, " Top: ", topText, " Right: ", rightText, " Bottom: ", bottomText } }
    end)
end

return function(module)
    module:registerCommand("fwindow", createFloatingWindow)
    module:registerPreferences(createUiMenu)
end