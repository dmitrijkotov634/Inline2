local preferences = inline:getSharedPreferences "notes"

local function save(_, query)
    query:answer()

    preferences:edit()
               :putString(query:getArgs(), query:getText())
               :apply()
end

local function note(_, query)
    query:answer(preferences:getString(query:getArgs(), ""))
end

local function delnote(_, query)
    query:answer()

    preferences:edit()
               :remove(query:getArgs())
               :apply()
end

local function notes(_, query)
    local result = "List of notes:\n"

    local iterator = preferences:getAll():entrySet():iterator()
    while iterator:hasNext() do
        result = result .. " - " .. iterator:next():getKey() .. "\n"
    end

    query:answer(result)
end

return function(module)
    module:setCategory "Notes"
    module:registerCommand("save", save, "Save a new note")
    module:registerCommand("note", note, "Gets the note specified")
    module:registerCommand("delnote", delnote, "Deletes a note, specified by note name")
    module:registerCommand("notes", notes, "List the saved notes")
end