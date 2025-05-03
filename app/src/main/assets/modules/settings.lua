require "utils"

local function help(_, query)
    local categories = {}
    local iterator = inline:getAllCommands():entrySet():iterator()

    while iterator:hasNext() do
        local entry = iterator:next()
        local cmd_name = entry:getKey()
        local cmd_obj = entry:getValue()
        local category = cmd_obj:getCategory() or "Other"

        if not categories[category] then
            categories[category] = {}
        end

        categories[category][cmd_name] = cmd_obj:getDescription()
    end

    local args = query:getArgs()
    if args == "" then
        local result = { "Help for Inline\nFor more help, type help <category>" }
        for category, commands in pairs(categories) do
            local cmd_list = {}
            for cmd_name in pairs(commands) do
                table.insert(cmd_list, cmd_name)
            end
            table.insert(result, "• " .. category .. ": " .. table.concat(cmd_list, ", "))
        end
        query:answer(table.concat(result, "\n"))
    else
        local lower_args = args:lower()
        local found_category

        for category in pairs(categories) do
            local lower_category = category:lower()
            if lower_category == lower_args or lower_category:find(lower_args, 1, true) == 1 then
                found_category = category
                break
            end
        end

        if found_category then
            local result = { "Help for " .. found_category .. ":" }
            for name, desc in pairs(categories[found_category]) do
                table.insert(result, "• " .. name .. (desc ~= "" and (" : " .. desc) or ""))
            end
            query:answer(table.concat(result, "\n"))
        else
            query:answer("Category not found")
        end
    end
end

local function reload(_, query)
    query:answer()
    inline:createEnvironment()
end

local function hotload(_, query)
    inline:getLazyLoadSharedPreferences()
          :edit()
          :clear()
          :apply()

    reload(_, query)
end

local function getPreferences(prefs)
    local notificationTimeout = inline:getDefaultSharedPreferences():getInt("notification_timeout", 0)
    local currentValue = prefs.text("Current value: " .. notificationTimeout .. " ms")

    return {
        "This slider controls how frequently Inline receives system events. Higher frequency improves command responsiveness but may increase CPU and battery usage.",
        prefs.spacer(8),
        currentValue,
        prefs.spacer(12),
        prefs.seekBar("notification_timeout", 3000):setOnProgressChanged(function(progress)
            currentValue:setText("Current value: " .. progress .. " ms")
        end),
        prefs.spacer(8),
        "Disabling this feature breaks the functionality of interactive menus, stops receiving cursor position change events, and may interfere with text insertion from floating windows.",
        prefs.spacer(8),
        prefs.checkBox("receive_selection_changes", "Receive selection changes"):setDefault(true),
        prefs.spacer(8),
        "Changes will apply after restarting the service from the device settings!",
        prefs.spacer(8),
        prefs.checkBox("disable_html", "Disable HTML formatting"),
        prefs.spacer(8),
        {
            prefs.button("Disable service", function()
                inline:disableSelf()
                prefs:cancel()
            end)
        },
        prefs.spacer(8)
    }
end

local function pkgname(input, query)
    query:answer(input:getPackageName())
end

return function(module)
    module:setCategory "Settings"
    module:setDescription "Manage environment and access help"

    module:registerCommand("help", help, "Displays help")
    module:registerCommand("reload", reload, "Recreate environment, initializes modules")
    module:registerCommand("hotload", hotload, "Instantly loads modules without using lazy loading")
    module:registerCommand("pkgname", pkgname, "Gives the package name of the app")

    module:registerPreferences(getPreferences)
    module:saveLazyLoad()
end