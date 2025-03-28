require "colorama"

local ActivityManager = luajava.bindClass("android.app.ActivityManager")

local BuildConfig = luajava.bindClass("com.wavecat.inline.BuildConfig")
local Build = luajava.bindClass("android.os.Build")
local Typeface = luajava.bindClass("android.graphics.Typeface")
local SystemClock = luajava.bindClass("android.os.SystemClock")

local BYTES_TO_MB = 1048576

local activityManager = inline:getSystemService(inline.ACTIVITY_SERVICE)
local mi = ActivityManager.MemoryInfo:new()

local timer = inline:getTimer()

local backgroundColor = 3355508864
local whiteColor = 4291348735
local closeColor = 4288059135

local windowConfig = {
    backgroundColor = backgroundColor,
    noLimits = true,
    autoFocus = false,
    paddingTop = 12,
    paddingBottom = 12,
    paddingLeft = 12,
    paddingRight = 12
}

local function formatUptime(milliseconds)
    local days = math.floor(milliseconds / 86400000)  -- 86400000 ms in a day
    local hours = math.floor((milliseconds % 86400000) / 3600000)
    local minutes = math.floor((milliseconds % 3600000) / 60000)
    local seconds = math.floor((milliseconds % 60000) / 1000)
    return string.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds)
end

local function getMemoryInfo()
    activityManager:getMemoryInfo(mi)
    local usedMemoryMB = math.ceil((mi.totalMem - mi.availMem) / BYTES_TO_MB)
    local totalMemoryMB = math.ceil(mi.totalMem / BYTES_TO_MB)
    return usedMemoryMB, totalMemoryMB
end

local function fetch(_, query)
    local usedMemoryMB, totalMemoryMB = getMemoryInfo()
    query:answer(
        colorama.text(
            colorama.newline,
            colorama.bold("{}$ Inline ") .. BuildConfig.VERSION_NAME .. " on Android "
                .. Build.VERSION.RELEASE,
            colorama.bold("------------------------------------"),
            colorama.bold("• Device: ") .. Build.MODEL .. " (" .. Build.BRAND .. ")",
            colorama.bold("• RAM: ") .. usedMemoryMB .. " MB / " .. totalMemoryMB .. " MB",
            colorama.bold("• Commands: ") .. inline:getAllCommands():size(),
            colorama.bold("• Watchers: ") .. inline:getAllWatchers():size(),
            "t.me/inline_android"
        )
    )
end

local function flogo(_, query)
    local logo = [[

     ███    ███        ███
    ██░    ░░░██      ██████
   ██        ░░██    ███░░░
 ███          ░░███ ░░█████
░░░██          ██░   ░░░░███
  ░░██        ██     ██████
   ░░███    ███     ░░░███
    ░░░    ░░░        ░░░
]]

    inline:showFloatingWindow(windowConfig, function(ui)
        local title = ui.text("Inline " .. BuildConfig.VERSION_NAME .. " on Android " .. Build.VERSION.RELEASE)
        local text = ui.text(logo)

        local time = ui.text("")
        local info = ui.text("")
        local uptime = ui.text("")

        local timerTask = inline:timerTask(function()
            local usedMemoryMB, totalMemoryMB = getMemoryInfo()
            time:setText("Date: " .. os.date("%Y-%m-%d %H:%M:%S"))
            info:setText("RAM: " .. usedMemoryMB .. " MB / " .. totalMemoryMB .. " MB")
            local uptimeMillis = SystemClock:uptimeMillis()
            uptime:setText("Uptime: " .. formatUptime(uptimeMillis))
        end)

        local close = ui.smallButton("[X] ", function()
            timerTask:cancel()
            ui.close()
        end)

        timer:schedule(timerTask, 0, 1000)

        title:setTextColor(whiteColor)
        text:setTextColor(whiteColor)
        time:setTextColor(whiteColor)
        info:setTextColor(whiteColor)
        uptime:setTextColor(whiteColor)
        close:setTextColor(closeColor)

        title:setTypeface(Typeface.MONOSPACE)
        text:setTypeface(Typeface.MONOSPACE)
        time:setTypeface(Typeface.MONOSPACE)
        info:setTypeface(Typeface.MONOSPACE)
        uptime:setTypeface(Typeface.MONOSPACE)
        close:setTypeface(Typeface.MONOSPACE)

        local textSize = 12

        title:setTextSize(textSize)
        text:setTextSize(textSize)
        time:setTextSize(textSize)
        info:setTextSize(textSize)
        uptime:setTextSize(textSize)
        close:setTextSize(textSize)

        return {
            { close, title },
            text,
            info,
            time,
            uptime
        }
    end)

    query:answer()
end

local function fclock(_, query)
    inline:showFloatingWindow(windowConfig, function(ui)
        local time

        local decorativeSymbols = { "░", "▒", "▓" }
        local currentSymbolIndex = 1

        local timerTask = inline:timerTask(function()
            local decorative = decorativeSymbols[currentSymbolIndex]
            time:setText(decorative .. os.date(" %H:%M:%S"))
            currentSymbolIndex = currentSymbolIndex % #decorativeSymbols + 1
        end)

        local close = ui.smallButton("[X] Clock:", function()
            timerTask:cancel()
            ui.close()
        end)

        time = ui.text("")

        timer:schedule(timerTask, 0, 1000)

        time:setTextColor(whiteColor)
        time:setTypeface(Typeface.MONOSPACE)
        time:setTextSize(20)

        close:setTextColor(closeColor)
        close:setTypeface(Typeface.MONOSPACE)
        close:setTextSize(17)

        return { { close, time } }
    end)

    query:answer()
end

return function(module)
    module:setCategory "Fetch"
    module:registerCommand("fetch", colorama.wrap(fetch), "Displays detailed system and memory usage information")

    if (inline:isFloatingWindowSupported()) then
        module:registerCommand("flogo", flogo, "Displays a large Inline logo with system information in a floating window")
        module:registerCommand("fclock", fclock, "Displays a large Inline clock with time in a floating window")
    end
end
