require "http"
require "colorama"

local dirPath

local function delete(_, query)
    luajava.newInstance("java.io.File", dirPath .. query:getArgs()):delete()
    query:answer(colorama.font("Success", "#D7FFD5"))
end

local function files(_, query)
    local list = luajava.newInstance("java.io.File", dirPath .. query:getArgs()):listFiles()

    local result = colorama.bold("Files in /") .. query:getArgs() .. ":" .. colorama.newline
    for i = 1, #list do
        result = result .. "• " .. colorama.font(list[i]:getName(),
            list[i]:isDirectory() and "#FFBA00" or "#FF7373") .. colorama.newline
    end
    query:answer(result)
end

local function load_(_, query)
    local request = http.Request.Builder.new()
                        :url(query:getArgs())
                        :get()
                        :build()

    query:answer(colorama.font("Loading...", "#8AB4F8"))

    http.call(
        request,
        function(_, _, string)
            local chunk, err = load(string)

            if not chunk then
                query:answer(colorama.font("Unable to load module: " .. err, "#FF7373"))
                return
            end

            local filename = query:getArgs():sub(query:getArgs():match("^.*()/") + 1, #query:getArgs())
            local file = io.open(dirPath .. filename, "w+")
            file:write(string)
            file:close()

            query:answer(colorama.font("Module installed: " .. filename, "#D7FFD5"))
            inline:createEnvironment()
        end,
        function(_, exception)
            query:answer(colorama.font("Unable to download module: " .. exception:getMessage(), "#FF7373"))
        end
    )
end

return function(module)
    module:setCategory "Loader"
    module:registerCommand("load", colorama.wrap(load_), "Load module by url")
    module:registerCommand("files", colorama.wrap(files), "List of downloaded files")
    module:registerCommand("delete", colorama.wrap(delete), "Delete a file")

    dirPath = inline:getExternalFilesDirs()[1]:getAbsolutePath() .. "/modules/"
end