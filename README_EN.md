# Inline

[![Android](https://img.shields.io/badge/Android-23%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Lua](https://img.shields.io/badge/Lua-2C2D72?logo=lua&logoColor=white)](https://www.lua.org)
[![Version](https://img.shields.io/badge/version-1.4.1-blue)](https://github.com/hugecatdev/Inline2/releases)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/5dc24ea6b1ed4c72b716f8ebd476046b)](https://www.codacy.com/gh/hugecatdev/Inline2/dashboard?utm_source=github.com&utm_medium=referral&utm_content=hugecatdev/Inline2&utm_campaign=Badge_Grade)
[![wakatime](https://wakatime.com/badge/user/9506e297-1af0-4411-a6c7-9831dccdbc9d/project/468b3ecd-efde-4667-a387-67963c8e85e7.svg)](https://wakatime.com/badge/user/9506e297-1af0-4411-a6c7-9831dccdbc9d/project/468b3ecd-efde-4667-a387-67963c8e85e7)
[![Stars](https://img.shields.io/github/stars/hugecatdev/Inline2?style=flat&logo=github)](https://github.com/hugecatdev/Inline2/stargazers)
[![Repo Size](https://img.shields.io/github/repo-size/hugecatdev/Inline2)](https://github.com/hugecatdev/Inline2)

Real-time text formatting android tool | [Русский](README.md)

Type `{command}$` in any text field - messenger, browser, notes - and Inline will execute your Lua function on the spot, replacing the expression with the result. No root required.

<img src="screenrecord.gif" alt="Inline Demo" height="500px" />

---

### Getting started

1. Install the app
2. On newer Android versions you need to allow restricted settings before enabling the service: **App Info** > **⋮** (menu in the top right corner) > **Allow restricted settings**
3. Enable the Inline accessibility service in device settings (button in the app menu)
4. On the main screen of the app you'll find a list of modules - download and configure the ones you need
5. Type `{help}$` in a text field to see available commands

> Not all text fields are supported. Compatibility depends on the Android skin - works well on Pixel and AOSP, unstable on Samsung, poorly supported on Xiaomi (MIUI/HyperOS).

### Features

- **Commands right in your text** - type `{name}$` in any input field of any app and your Lua script processes the text on the fly
- **Custom Lua modules** - each module is a `.lua` file with commands you define yourself
- **Android API access** - LuaJava bridge gives direct access to the Android SDK from scripts
- **In-app management** - modules are downloaded and configured through the UI, each module can have its own settings screen

### Module development

Save `hello.lua` to `/sdcard/inline` and press **Reload** in the app menu (**⋮**):

```lua
local function hellocmd(_, query)
    query:answer "Hello, world!"
end

return function(module)
    module:registerCommand("hello", hellocmd, "Prints hello world")
end
```

Type `{hello}$` in a text field - done. More about the API, libraries and module capabilities - in the [Wiki](https://github.com/hugecatdev/Inline2/wiki/Inline).

> To access the `/sdcard/inline` directory, enable Storage Permission in the app menu (**⋮**).
