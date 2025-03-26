require "colorama"

local ActivityManager = luajava.bindClass("android.app.ActivityManager")

local BuildConfig = luajava.bindClass("com.wavecat.inline.BuildConfig")
local Build = luajava.bindClass("android.os.Build")

local function fetch(_, query)
    local activityManager = inline:getSystemService(inline.ACTIVITY_SERVICE)
    local mi = ActivityManager.MemoryInfo:new()
    activityManager:getMemoryInfo(mi)

    query:answer(
        colorama.text(
            colorama.newline,
            colorama.bold("{}$ Inline ") .. BuildConfig.VERSION_NAME .. " on Android "
                .. Build.VERSION.RELEASE,
            colorama.bold("------------------------------------"),
            colorama.bold("• Device: ") .. Build.MODEL .. " (" .. Build.BRAND .. ")",
            colorama.bold("• RAM: ") .. math.ceil((mi.totalMem - mi.availMem) / 1048576)
                .. " MB / " .. math.ceil(mi.totalMem / 1048576) .. " MB",
            colorama.bold("• Commands: ") .. inline:getAllCommands():size(),
            colorama.bold("• Watchers: ") .. inline:getAllWatchers():size(),
            "t.me/inline_android"
        )
    )
end

return function(module)
    module:setCategory "Fetch"
    module:registerCommand("fetch", colorama.wrap(fetch), "Displays detailed system and RAM info")
end
