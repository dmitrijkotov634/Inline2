local Gravity = luajava.bindClass("android.view.Gravity")

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
    local rect = inline:getBoundsInScreen(input)
    inline:showFloatingWindow({
        noLimits = true,
        positionX = rect.left,
        positionY = inline:getScreenHeight() - rect.top + 50,
        gravity = bit32.bor(Gravity.BOTTOM, Gravity.LEFT)
    }, function(ui)
        local xText = ui.text("")
        local yText = ui.text("")
        local leftText = ui.text(tostring(rect.left))
        local topText = ui.text(tostring(rect.top))
        local rightText = ui.text(tostring(rect.right))
        local bottomText = ui.text(tostring(rect.bottom))
        ui.onMove = function(x, y)
            xText:setText(tostring(x))
            yText:setText(tostring(y))
            input:refresh()
        end
        return { { "X: ", xText, " Y: ", yText },
                 { "Left: ", leftText, " Top: ", topText, " Right: ", rightText, " Bottom: ", bottomText },
                 { "Width: " .. inline:getScreenWidth() .. " Height: " .. inline:getScreenHeight() } }
    end)
end

return function(module)
    module:registerCommand("fwindow", createFloatingWindow)
    module:registerPreferences(createUiMenu)
end