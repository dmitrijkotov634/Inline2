require "com.wavecat.inline.libs.http"

local preferences = inline:getDefaultSharedPreferences()

local function wiki(_, query)
    if query:getArgs() == "" then
        inline:toast "Invalid arguments"
        query:answer()
        return
    end

    local request = http:newRequestBuilder()
                        :url(http:buildUrl("https://" .. preferences:getString("wiki", "en") .. ".wikipedia.org/w/api.php", {
        format = "json",
        action = "query",
        prop = "extracts",
        exintro = "",
        explaintext = "",
        redirects = 1,
        titles = query:getArgs()
    }))
                        :get()
                        :build()

    http:call(request, function(_, response)
        local json = luajava.newInstance("org.json.JSONObject", response:body():string())
        local pages = json:getJSONObject("query"):getJSONObject("pages")

        query:answer(pages:getJSONObject(pages:names():optString(0)):optString("extract"))
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
    module:registerCommand("wiki", wiki)
    module:registerCommand("wikilang", wikilang)
end