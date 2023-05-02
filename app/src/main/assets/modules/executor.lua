require "com.wavecat.inline.libs.utils"

local warningMessage = "Warning!!! This command executes any Lua code. Don't use if you don't know what it is. Try again after reading this warning"

local function checkWarning(query)
    local preferences = inline:getDefaultSharedPreferences()

    if not preferences:getBoolean("executor", false) then
        query:answer(warningMessage)
        preferences:edit():putBoolean("executor", true):apply()
        return true
    end

    return false
end

local function eval(_, query)
    if checkWarning(query) then
        return
    end

    local chunk = load("return " .. query:getArgs())
    query:answer(tostring(chunk()))
end

local function exec(_, query)
    if checkWarning(query) then
        return
    end

    local chunk = load(query:getArgs())

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
        prefs.checkBox("executor", "Don't show warning"),
        codePlayground,
        codeResult,
        prefs.button("Execute", function()
            local status, message = pcall(function()
                local chunk = load(codePlayground:getText())

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
end

return function(module)
    module:setCategory "Executor"
    module:registerCommand("eval", utils.hasArgs(eval), "Evaluates lua code")
    module:registerCommand("exec", utils.hasArgs(exec), "Executes lua code")
    module:registerPreferences(getPreferences)
end