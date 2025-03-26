require "menu"

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
    local result = {
        {
            caption = "[X]",
            action = function(_, queryExit)
                queryExit:answer()
            end
        },
        " List of notes:\n\n"
    }

    local iterator = preferences:getAll():entrySet():iterator()

    while iterator:hasNext() do
        local entry = iterator:next()
        result[#result + 1] = {
            caption = "[X]",
            action = function(_, q)
                menu.create(q, {
                    "Delete ",
                    entry:getKey(),
                    "? ",
                    { caption = "[Yes]", action = function(_, queryYes)
                        preferences:edit()
                                   :remove(entry:getKey())
                                   :apply()
                        notes(_, queryYes)
                    end },
                    " ",
                    { caption = "[No]", action = notes }
                }, notes)
            end
        }

        result[#result + 1] = " "

        result[#result + 1] = {
            caption = "[>]",
            action = function(_, q)
                menu.create(q, {
                    { caption = "[<]", action = notes },
                    " Contents of ",
                    entry:getKey(),
                    ":\n\n",
                    entry:getValue(),
                    "\n\n",
                    { caption = "[Paste]", action = function(_, queryYes)
                        queryYes:answer(entry:getValue())
                    end }
                }, notes)
            end
        }

        result[#result + 1] = " " .. entry:getKey() .. "\n"
    end

    menu.create(query, result, notes)
end

return function(module)
    module:setCategory "Notes"
    module:registerCommand("save", save, "Save a new note")
    module:registerCommand("note", note, "Gets the note specified")
    module:registerCommand("delnote", delnote, "Deletes a note, specified by note name")
    module:registerCommand("notes", notes, "Notes manager")
end