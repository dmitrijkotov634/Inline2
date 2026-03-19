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
            query:answer "Category not found"
        end
    end
end

local function reload(_, query)
    query:answer()
    inline:createEnvironment()
end

local function getPreferences(prefs)
    local notificationTimeout = inline:getDefaultSharedPreferences():getInt("notification_timeout", 0)
    local currentValue = prefs.text("Current value: " .. notificationTimeout .. " ms")

    return {
        prefs.card {
            prefs.text "Event Frequency":bold():size(16),
            prefs.spacer(4),
            prefs.text "Controls how frequently Inline receives system events. Higher frequency improves command responsiveness but may increase CPU and battery usage.",
            prefs.spacer(8),
            currentValue,
            prefs.spacer(8),
            prefs.slider("notification_timeout", 3000)
                 :useInt()
                 :setStep(100)
                 :setOnProgressChanged(function(progress)
                currentValue:setText("Current value: " .. progress .. " ms")
            end),
        },
        prefs.spacer(12),
        prefs.card {
            prefs.text "Selection Events":bold():size(16),
            prefs.spacer(8),
            prefs.text "Disabling this breaks interactive menus, stops receiving cursor position changes, and may interfere with text insertion from floating windows.",
            prefs.spacer(8),
            prefs.switch("receive_selection_changes", "Receive selection changes"):setDefault(true),
        },
        prefs.spacer(12),
        prefs.card {
            prefs.text "Formatting":bold():size(16),
            prefs.spacer(4),
            prefs.switch("disable_html", "Disable HTML formatting"),
        },
        prefs.spacer(12),
        prefs.text "Changes will apply after restarting the service from the device settings!":size(12):center(),
    }
end

local function short_name(path)
    return path:match("[^/]+$") or path
end

local function modules(_, query)
    local loaded = inline:getLoadedModules()
    local lazyPrefs = inline:getLazyLoadSharedPreferences()
    local allLazyKeys = lazyPrefs:getAll()

    local loaded_list = {}
    local lazy_list = {}

    local loaded_iter = loaded:entrySet():iterator()
    while loaded_iter:hasNext() do
        local entry = loaded_iter:next()
        table.insert(loaded_list, "✓ " .. short_name(entry:getKey()))
    end

    local lazy_iter = allLazyKeys:entrySet():iterator()
    while lazy_iter:hasNext() do
        local entry = lazy_iter:next()
        local key = entry:getKey()
        local value = entry:getValue()

        if type(value) == "userdata" and not loaded:containsKey(key) then
            table.insert(lazy_list, "⏳ " .. short_name(key))
        end
    end

    table.sort(loaded_list)
    table.sort(lazy_list)

    local total = #loaded_list + #lazy_list
    local result = { "Modules: " .. total .. " (" .. #loaded_list .. " loaded)" }

    for _, v in ipairs(loaded_list) do
        table.insert(result, v)
    end

    for _, v in ipairs(lazy_list) do
        table.insert(result, v)
    end

    query:answer(table.concat(result, "\n"))
end

local function pkgname(input, query)
    query:answer(input:getPackageName())
end

return function(module)
    module:setCategory "Settings"
    module:setDescription "Manage environment and access help"

    module:registerCommand("help", help, "Displays help")
    module:registerCommand("reload", reload, "Recreate environment, initializes modules")
    module:registerCommand("modules", modules, "Shows loaded and lazy modules")
    module:registerCommand("pkgname", pkgname, "Gives the package name of the app")

    module:registerPreferences(getPreferences)
    module:saveLazyLoad()
end