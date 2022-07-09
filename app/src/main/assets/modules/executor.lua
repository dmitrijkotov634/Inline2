require "com.wavecat.inline.libs.utils"

local function eval(_, query, args)
    local chunk = load("return " .. args[1])
    if chunk then
        query:answer(tostring(chunk()))
    end
end

local function exec(_, query, args)
    local chunk = load(args[1])
    if chunk then
        local result = chunk()
        if result then
            query:answer(tostring(result))
        end
    end
end

return function(module)
    module:setCategory "Executor"
    module:registerCommand("eval", utils.hasArgs(eval), "Evaluates lua code")
    module:registerCommand("exec", utils.hasArgs(exec), "Executes lua code")
end