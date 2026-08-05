rootProject.name = "jubler-project"

// Order matters: launcher first (thin wrapper), core second (base functionality),
// then all plugin modules. Installer module removed - packaging handled in root build.
include(
    ":launcher",
    ":core",
    ":basetextsubs",
    ":coretools",
    ":coretheme",
    // ":languagetool",  // set aside in favour of the lighter Hunspell speller (kept in-tree, not built)
    ":hunspell-jubler",
    ":vlcj-jubler",
    ":zemberek",
    ":azuretranslate",
    ":subdownload",
    ":subdownload-subsource",
    ":subdownload-opensubtitles",
    ":subdownload-subdl",
    ":autoupdate"
)

// Map module names to their actual directory paths
project(":launcher").projectDir = file("modules/launcher")
project(":core").projectDir = file("modules/core")
project(":basetextsubs").projectDir = file("modules/basetextsubs")
project(":coretools").projectDir = file("modules/coretools")
project(":coretheme").projectDir = file("modules/coretheme")
// project(":languagetool").projectDir = file("modules/languagetool")  // set aside, see include() above
project(":hunspell-jubler").projectDir = file("modules/hunspell-jubler")
project(":vlcj-jubler").projectDir = file("modules/vlcj-jubler")
project(":zemberek").projectDir = file("modules/zemberek")
project(":azuretranslate").projectDir = file("modules/azuretranslate")
project(":subdownload").projectDir = file("modules/subdownload")
project(":subdownload-subsource").projectDir = file("modules/subdownload-subsource")
project(":subdownload-opensubtitles").projectDir = file("modules/subdownload-opensubtitles")
project(":subdownload-subdl").projectDir = file("modules/subdownload-subdl")
project(":autoupdate").projectDir = file("modules/autoupdate")

// i18n-tools is a standalone build tool, not part of the main build
// Run it on-demand with: gradle -p modules/i18n-tools extract|merge|update
