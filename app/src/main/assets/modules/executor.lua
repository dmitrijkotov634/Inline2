require "com.wavecat.inline.libs.utils"

local function checkWarning(query)
    local preferences = inline:getDefaultSharedPreferences()

    if not preferences:getBoolean("executor", false) then
        query:answer("Warning!!! This command executes any Lua code. Don't use if you don't know what it is. Try again after reading this warning")
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

return function(module)
    module:setCategory "Executor"
    module:registerCommand("eval", utils.hasArgs(eval), "Evaluates lua code")
    module:registerCommand("exec", utils.hasArgs(exec), "Executes lua code")
end