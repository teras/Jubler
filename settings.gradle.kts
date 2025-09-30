rootProject.name = "jubler-project"

// Order matters: launcher first (thin wrapper), core second (base functionality),
// then all plugin modules. Installer module removed - packaging handled in root build.
include(
    ":launcher",
    ":core",
    ":basetextsubs",
    ":coretools",
    ":coretheme",
    ":aspell",
    ":mplayer",
    ":vlcj-jubler",
    ":zemberek",
    ":azuretranslate",
    ":autoupdate"
)

// Map module names to their actual directory paths
project(":launcher").projectDir = file("modules/launcher")
project(":core").projectDir = file("modules/core")
project(":basetextsubs").projectDir = file("modules/basetextsubs")
project(":coretools").projectDir = file("modules/coretools")
project(":coretheme").projectDir = file("modules/coretheme")
project(":aspell").projectDir = file("modules/aspell")
project(":mplayer").projectDir = file("modules/mplayer")
project(":vlcj-jubler").projectDir = file("modules/vlcj-jubler")
project(":zemberek").projectDir = file("modules/zemberek")
project(":azuretranslate").projectDir = file("modules/azuretranslate")
project(":autoupdate").projectDir = file("modules/autoupdate")
