require "com.wavecat.inline.libs.utils"

local function eval(_, query)
    local chunk = load("return " .. query:getArgs())
    query:answer(tostring(chunk()))
end

local function exec(_, query)
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