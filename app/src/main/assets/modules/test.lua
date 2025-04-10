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

return function(module)
    module:setDescription "Testing and debugging module"
    module:registerPreferences(createUiMenu)
end