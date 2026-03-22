require "utils"
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

    for key, value in utils.mapEntries(preferences:getAll()) do
        result[#result + 1] = {
            caption = "[X]",
            action = function(_, q)
                menu.create(q, {
                    "Delete ",
                    key,
                    "? ",
                    { caption = "[Yes]", action = function(_, queryYes)
                        preferences:edit()
                                   :remove(key)
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
                    key,
                    ":\n\n",
                    value,
                    "\n\n",
                    { caption = "[Paste]", action = function(_, queryYes)
                        queryYes:answer(value)
                    end }
                }, notes)
            end
        }

        result[#result + 1] = " " .. key .. "\n"
    end

    menu.create(query, result)
end

return function(module)
    module:setCategory "Notes"
    module:setDescription "Create, manage, and delete notes"

    module:registerCommand("save", save, "Save a new note")
    module:registerCommand("note", note, "Gets the note specified")
    module:registerCommand("delnote", delnote, "Deletes a note, specified by note name")
    module:registerCommand("notes", notes, "Notes manager")

    module:saveLazyLoad()
end