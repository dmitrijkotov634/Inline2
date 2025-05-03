require "utils"

local preferences = inline:getDefaultSharedPreferences()
local warningMessage = "Warning!!! This command executes any Lua code. Don't use if you don't know what it is. Try again after reading this warning"

local env = {
    _preferences = preferences
}

setmetatable(env, { __index = _G })

local function checkWarning(query)
    if not preferences:getBoolean("executor_warning", false) then
        query:answer(warningMessage)
        preferences:edit():putBoolean("executor_warning", true):apply()
        return true
    end
    return false
end

local function eval(node, query)
    if checkWarning(query) then
        return
    end

    env._node = node
    env._query = query

    local chunk = load("return " .. query:getArgs(), "eval", "bt", env)

    if preferences:getBoolean("executor_print_code", true) then
        query:answer(query:getArgs() .. " = " .. tostring(chunk()))
    else
        query:answer(tostring(chunk()))
    end
end

local function exec(node, query)
    if checkWarning(query) then
        return
    end

    env._node = node
    env._query = query

    local chunk = load(query:getArgs(), "exec", "bt", env)

    local result = chunk()
    if result then
        query:answer(tostring(result))
    end
end

local function getPreferences(prefs)
    local codePlayground = prefs.textInput("Code")
    local codeResult = prefs.text("Result:")

    return {
        warningMessage,
        prefs.spacer(4),
        prefs.checkBox("executor_warning", "Don't show warning"),
        prefs.checkBox("executor_print_code", "Print code before eval")
             :setDefault(true),
        prefs.spacer(4),
        codePlayground,
        prefs.spacer(8),
        codeResult,
        prefs.spacer(8),
        {
            prefs.button("Execute", function()
                local status, message = pcall(function()
                    local chunk = load(codePlayground:getText(), "execute", "bt", env)

                    local result = chunk()
                    if result then
                        codeResult:setText("Result: " .. result)
                    end
                end)

                if not status then
                    codeResult:setText("Error: " .. message)
                end
            end)
        }
    }
end

return function(module)
    module:setCategory "Executor"
    module:setDescription "Run and evaluate Lua code"
    module:registerCommand("eval", utils.hasArgs(eval), "Evaluates lua code")
    module:registerCommand("exec", utils.hasArgs(exec), "Executes lua code")
    module:registerPreferences(getPreferences)
    env._module = module
end