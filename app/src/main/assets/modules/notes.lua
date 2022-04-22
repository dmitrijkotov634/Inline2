local preferences = inline:getSharedPreferences "notes"
local list = preferences:getStringSet("notes", luajava.newInstance("java.util.HashSet"))

local function getNoteName(query)
    return query:getArgs() == "" and "temp" or query:getArgs()
end

local function save(_, query)
    query:answer()

    local arg = getNoteName(query)
    local editor = preferences:edit()
                              :putString(arg, query:getText())

    if not list:contains(arg) then
        list:add(arg)
        editor:putStringSet("notes", list)
    end

    editor:apply()
end

local function note(_, query)
    query:answer(preferences:getString(query:getArgs(), ""))
end

local function delnote(_, query)
    query:answer("")

    local arg = getNoteName(query)

    if list:contains(arg) then
        list:remove(arg)
        preferences:edit()
                   :remove(arg)
                   :putStringSet("notes", list)
                   :apply()
    end
end

local function notes(_, query)
    local result = "Notes: "

    local iterator = list:iterator()
    while iterator:hasNext() do
        result = result .. "- " .. iterator:next()
    end

    query:answer(result)
end

return function(module)
    module:registerCommand("save", save)
    module:registerCommand("note", note)
    module:registerCommand("delnote", delnote)
    module:registerCommand("notes", notes)
end