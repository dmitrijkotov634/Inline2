require "com.wavecat.inline.libs.colorama"

local Runtime = luajava.bindClass("java.lang.Runtime")
local ActivityManager = luajava.bindClass("android.app.ActivityManager")

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

local function raminfo(_, query)
    local activityManager = inline:getSystemService(inline.ACTIVITY_SERVICE)
    local mi = ActivityManager.MemoryInfo:new()
    activityManager:getMemoryInfo(mi)
    local runtime = Runtime:getRuntime()
    query:answer(
            colorama.text(
                    colorama.newline,
                    colorama.bold("()_) RAM"),
                    "",
                    colorama.bold("• Total: ") .. math.ceil(mi.totalMem / 1048576) .. " MB",
                    colorama.bold("• Available: ") .. math.ceil(mi.availMem / 1048576) .. " MB",
                    "",
                    colorama.bold("• Low Memory: ") .. (mi.lowMemory and "yes" or "no"),
                    "",
                    colorama.bold("• Runtime Max Memory: ") .. math.ceil(runtime:maxMemory() / 1048576) .. " MB",
                    colorama.bold("• Runtime Total Memory: ") .. math.ceil(runtime:totalMemory() / 1048576) .. " MB",
                    colorama.bold("• Runtime Free Memory: ") .. math.ceil(runtime:freeMemory() / 1048576) .. " MB"
            )
    )
end

return function(module)
    module:setCategory "Info"
    module:registerCommand("info", colorama.wrap(info), "Displays short information")
    module:registerCommand("raminfo", colorama.wrap(raminfo), "Displays brief information about RAM")
end