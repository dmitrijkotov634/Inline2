local counter = 0

local function createUiMenu(ui)
    local toast = function(v)
        inline:toast(tostring(v))
    end

    -- Dynamic elements for interaction demos
    local counterText
    local progressLabel
    local inputEcho
    local visibilityTarget
    local enableTarget

    return {
        -- Section 1: Typography
        ui.card {
            ui.text "Typography":bold():size(18),
            ui.spacer(4),
            ui.text "Regular text — default style",
            ui.text "Bold text":bold(),
            ui.text "Italic text":italic(),
            ui.text "Bold Italic text":boldItalic(),
            ui.text "Small (12sp)":size(12),
            ui.text "Large (20sp)":size(20),
            ui.text "Colored text":color "#E91E63",
            ui.text "Centered text":center(),
            ui.text "Max 1 line — this is a very long text that should be truncated because it exceeds the maximum number of lines allowed":maxLines(1),
        },

        ui.spacer(12),

        -- Section 2: Buttons
        ui.card {
            ui.text "Buttons":bold():size(18),
            ui.spacer(4),
            ui.button("Material Button", function(btn)
                counter = counter + 1
                counterText:setText("Clicked " .. counter .. " times")
            end),
            ui.spacer(4),
            ui.row {
                ui.smallButton("Small 1", toast),
                ui.smallButton("Small 2", toast),
                ui.smallButton("Small 3", toast),
            },
            ui.spacer(4),
            counterText = ui.text "Clicked 0 times":size(12):center(),
        },

        ui.spacer(12),

        -- Section 3: Inputs
        ui.card {
            ui.text "Inputs":bold():size(18),
            ui.spacer(4),
            ui.textInput("Text input", function(text)
                inputEcho:setText("Echo: " .. text)
            end),
            ui.spacer(4),
            inputEcho = ui.text "Echo: ":size(12),
            ui.spacer(8),
            ui.textInput "Number input"
                 :setInputType { "TYPE_CLASS_NUMBER", "TYPE_NUMBER_FLAG_SIGNED" },
            ui.spacer(8),
            ui.textInput "Password":hidePassword(),
        },

        ui.spacer(12),

        -- Section 4: Controls
        ui.card {
            ui.text "Controls":bold():size(18),
            ui.spacer(4),
            ui.checkBox "Standalone checkbox",
            ui.checkBox("Checkbox with listener", function(checked)
                inline:toast("Checked: " .. tostring(checked))
            end),
            ui.spacer(8),
            ui.text "SeekBar:":size(14),
            ui.spacer(4),
            progressLabel = ui.text "Progress: 0":size(12),
            ui.seekBar(toast, 100):setOnProgressChanged(function(progress)
                progressLabel:setText("Progress: " .. progress)
            end),
            ui.spacer(8),
            ui.text "Spinner:":size(14),
            ui.spacer(4),
            ui.spinner({ "Option A", "Option B", "Option C" }, function(value)
                inline:toast("Selected: " .. value)
            end),
        },

        ui.spacer(12),

        -- Section 5: Layout
        ui.card {
            ui.text "Layout":bold():size(18),
            ui.spacer(4),

            ui.text "Row with flex spacer:":size(14),
            ui.spacer(4),
            ui.row {
                ui.text "Left",
                ui.flexSpacer(),
                ui.text "Right",
            },

            ui.spacer(8),
            ui.divider(),
            ui.spacer(8),

            ui.text "Row of buttons:":size(14),
            ui.spacer(4),
            ui.row {
                ui.smallButton("A", toast),
                ui.flexSpacer(),
                ui.smallButton("B", toast),
                ui.flexSpacer(),
                ui.smallButton("C", toast),
            },

            ui.spacer(8),
            ui.divider(),
            ui.spacer(8),

            ui.text "Horizontal scroll:":size(14),
            ui.spacer(4),
            ui.hscroll {
                ui.button("Scroll", toast),
                ui.button("Scroll", toast),
                ui.button("Scroll", toast),
                ui.button("Scroll", toast),
                ui.button("Scroll", toast),
            },
        },

        ui.spacer(12),

        -- Section 6: Visibility & State
        ui.card {
            ui.text "Visibility & State":bold():size(18),
            ui.spacer(4),

            visibilityTarget = ui.text "I can be hidden!":bold(),
            ui.spacer(4),
            ui.row {
                ui.smallButton("Show", function()
                    ui.visible(visibilityTarget, true)
                end),
                ui.smallButton("Hide", function()
                    ui.visible(visibilityTarget, false)
                end),
            },

            ui.spacer(8),
            ui.divider(),
            ui.spacer(8),

            enableTarget = ui.button "I can be disabled",
            ui.spacer(4),
            ui.row {
                ui.smallButton("Enable", function()
                    ui.enabled(enableTarget, true)
                end),
                ui.smallButton("Disable", function()
                    ui.enabled(enableTarget, false)
                end),
            },
        },

        ui.spacer(12),

        -- Section 7: Card Customization
        ui.card {
            ui.text "Default card style",
        },
        ui.spacer(8),
        ui.card {
            ui.text "Custom corner radius (24dp)",
        }:setCornerRadius(24),
        ui.spacer(8),
        ui.card {
            ui.text "Custom stroke color":color "#4CAF50",
        }:setStrokeColor(0xFF4CAF50),
        ui.spacer(8),
        ui.card {
            ui.text "With elevation":bold(),
        }:setCardElevation(4),

        ui.spacer(12),

        -- Section 8: Nesting
        ui.card {
            ui.text "Nested Cards":bold():size(18),
            ui.spacer(8),
            ui.card {
                ui.text "Inner card 1":size(14),
                ui.spacer(4),
                ui.text "Cards can be nested for grouping":size(12),
            },
            ui.spacer(8),
            ui.card {
                ui.text "Inner card 2":size(14),
                ui.spacer(4),
                ui.row {
                    ui.smallButton("Action A", toast),
                    ui.flexSpacer(),
                    ui.smallButton("Action B", toast),
                },
            },
        },

        ui.spacer(12),
        ui.text "End of UI showcase":size(12):center(),
    }
end

return function(module)
    module:setCategory "Test"
    module:setDescription "UI widget showcase and testing module"
    module:registerPreferences(createUiMenu)
end
