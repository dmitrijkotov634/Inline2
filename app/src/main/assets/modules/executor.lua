local function checkArg(query)
    if query:getArgs() == "" then
        inline:toast "Empty value"
        query:answer()
        return false
    end
    return true
end

local function eval(_, query)
    if checkArg(query) then
        local chunk = load("return " .. query:getArgs())

        if chunk then
            query:answer(tostring(chunk()))
        end
    end
end

local function exec(_, query)
    if checkArg(query) then
        local chunk = load(query:getArgs())

        if chunk then
            local result = chunk()

            if result then
                query:answer(tostring(result))
            end
        end
    end
end

return function(module)
    module:setCategory "Executor"
    module:registerCommand("eval", eval, "Evaluates lua code")
    module:registerCommand("exec", exec, "Executes lua code")
end