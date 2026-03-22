# Inline

[![Android](https://img.shields.io/badge/Android-23%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Lua](https://img.shields.io/badge/Lua-2C2D72?logo=lua&logoColor=white)](https://www.lua.org)
[![Version](https://img.shields.io/badge/version-1.4.1-blue)](https://github.com/hugecatdev/Inline2/releases)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/5dc24ea6b1ed4c72b716f8ebd476046b)](https://www.codacy.com/gh/hugecatdev/Inline2/dashboard?utm_source=github.com&utm_medium=referral&utm_content=hugecatdev/Inline2&utm_campaign=Badge_Grade)
[![wakatime](https://wakatime.com/badge/user/9506e297-1af0-4411-a6c7-9831dccdbc9d/project/468b3ecd-efde-4667-a387-67963c8e85e7.svg)](https://wakatime.com/badge/user/9506e297-1af0-4411-a6c7-9831dccdbc9d/project/468b3ecd-efde-4667-a387-67963c8e85e7)
[![Stars](https://img.shields.io/github/stars/hugecatdev/Inline2?style=flat&logo=github)](https://github.com/hugecatdev/Inline2/stargazers)
[![Repo Size](https://img.shields.io/github/repo-size/hugecatdev/Inline2)](https://github.com/hugecatdev/Inline2)

Инструмент для форматирования текста в реальном времени на Android | [English](README_EN.md)

Введите `{команда}$` в любом текстовом поле - мессенджере, браузере, заметках - и Inline выполнит вашу Lua-функцию прямо на месте, заменив выражение результатом. Без root.

<img src="screenrecord.gif" alt="Inline Demo" height="500px" />

---

### Быстрый старт

1. Установите приложение
2. На новых версиях Android перед включением службы нужно разрешить защищенные настройки: **О приложении** > **⋮** (меню в правом верхнем углу) > **Разрешить ограниченные настройки**
3. Включите службу специальных возможностей Inline в настройках устройства (кнопка в меню приложения)
4. На главном экране приложения доступен список модулей - скачивайте нужные и настраивайте
5. Введите `{help}$` в текстовом поле для показа доступных команд

> Поддерживаются не все текстовые поля. Работа приложения зависит от оболочки Android - корректно работает на Pixel и AOSP, нестабильно на Samsung, плохо поддерживается на Xiaomi (MIUI/HyperOS).

### Возможности

- **Команды прямо в тексте** - пишите `{имя}$` в любом поле ввода любого приложения, и ваш Lua-скрипт обработает текст на лету
- **Свои модули на Lua** - каждый модуль это `.lua` файл с командами, которые вы определяете сами
- **Доступ к Android API** - мост LuaJava дает прямой доступ к Android SDK из скриптов
- **Управление через приложение** - модули скачиваются и настраиваются через UI, каждый модуль может иметь свой экран настроек

### Разработка модулей

Сохраните файл `hello.lua` в `/sdcard/inline` и нажмите **Reload** в меню приложения (**⋮**):

```lua
local function hellocmd(_, query)
    query:answer "Hello, world!"
end

return function(module)
    module:registerCommand("hello", hellocmd, "Prints hello world")
end
```

Введите `{hello}$` в текстовом поле - готово. Подробнее об API, библиотеках и возможностях модулей - в [Wiki](https://github.com/hugecatdev/Inline2/wiki/Inline).

> Для доступа к директории `/sdcard/inline` включите Storage Permission в меню приложения (**⋮**).
