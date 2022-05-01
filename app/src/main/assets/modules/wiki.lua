require "com.wavecat.inline.libs.http"

local preferences = inline:getDefaultSharedPreferences()

local function wiki(_, query)
    if query:getArgs() == "" then
        inline:toast "Invalid arguments"
        query:answer()
        return
    end

    local request = http.Request.Builder.new()
                        :url("https://" .. preferences:getString("wiki", "en") .. ".wikipedia.org/api/rest_v1/page/summary/" .. query:getArgs())
                        :get()
                        :build()

    http.call(request, function(_, _, string)
        local json = luajava.newInstance("org.json.JSONObject", string)

        query:answer(json:optString("extract"))
    end)
end

local function wikilang(_, query)
    local language = query:getArgs()

    if language == "ru" or language == "en" then
        preferences:edit()
                   :putString("wiki", language)
                   :apply()

        query:answer()
    end
end

return function(module)
    module:setCategory "Wiki"
    module:registerCommand("wiki", wiki, "Gives a short description from wikipedia")
    module:registerCommand("wikilang", wikilang, "Sets the wikipedia language")
end