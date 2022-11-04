require "com.wavecat.inline.libs.colorama"

local BuildConfig = luajava.bindClass("com.wavecat.inline.BuildConfig")
local Build = luajava.bindClass("android.os.Build")

local function info(_, query)
    query:answer(
            colorama.text(
                    colorama.newline,
                    colorama.bold("{}$ Inline"),
                    "",
                    colorama.bold("• Version: ") ..
                            BuildConfig.VERSION_NAME .. colorama.italic(" (" .. BuildConfig.VERSION_CODE .. ")"),
                    "",
                    colorama.bold("• Android: ") ..
                            Build.VERSION.RELEASE .. colorama.italic(" (" .. Build.VERSION.SDK_INT .. ")"),
                    colorama.bold("• Device: ")
                            .. Build.MODEL .. colorama.italic(" (" .. Build.BRAND .. ")"),
                    "",
                    colorama.italic(
                            inline:getAllCommands():size() .. " commands, " .. inline:getAllWatchers():size() .. " watchers"
                    ),
                    "",
                    "t.me/inline_android"
            )
    )
end

return function(module)
    module:setCategory "Settings"
    module:registerCommand("info", colorama.wrap(info), "Displays short information")
end