# Writing Jubler plugins

A plugin is one shared library (`.so`, `.dylib`, `.dll`) in the per-user plugins folder
(`~/.local/share/jubler/plugins` on Linux, `~/Library/Application Support/Jubler/plugins` on macOS,
`%APPDATA%\Jubler\plugins` on Windows). Jubler lists every plugin it finds in Preferences ▸ Plugins; a plugin is
loaded (at the next start) only when it is enabled there.

## The API

Include `jubler/PluginApi.h` (in `src/plugin-api`). The library's root object implements `jubler::Plugin`
(interface id `org.jubler.Plugin/2.0`) and registers its extensions in `registerExtensions()`:

| Extension | Class | Where it appears |
|---|---|---|
| Tool | `jubler::Tool` | Tools menu (with an undo step and an entry in the shortcut table) |
| Command-line tool | `jubler::CommandLineTool` | `jubler --load file.srt -x <name>:key=value` |
| Subtitle format | `jubler::Format` | Open (auto-detection), Save As, the format lists |
| Translator | `jubler::Translator` | Tools ▸ Translate (runs in a worker thread; optional `configure()` behind the Configure button) |
| Spell checker | `jubler::SpellChecker` | Tools ▸ Spell check |
| Event listener | `jubler::EventListener` | window created, document opened, before/after save (can veto), selection changed |
| Preferences page | `jubler::PreferencesPage` | a page of the Preferences dialog |
| Subtitle provider | `jubler::SubtitleProvider` | the subtitle download window (see `SubDownloadApi.h`) |

Documents are seen through `jubler::Document`: rows of `jubler::Subtitle` (times in seconds, text with SubStation
Alpha override tags for inline styling, style name, mark, layer). `Registrar::host()` gives the host services: HTTP,
encrypted secrets, translation of UI strings, the log, and plain settings (`setting()`/`setSetting()`).

The header only grows: new functions are appended, so a plugin keeps working with newer Jubler versions. A plugin
built against a newer API than the running Jubler is not loaded (a line in the log says why).

The provider-only interface (`org.jubler.SubtitleProviderPlugin/2.0`) is also loaded.

No exception may cross between Jubler and a plugin: every module has its own copy of an inline exception class's
type information, so a throw from one module is not reliably caught by type in another (macOS in particular).
Providers therefore return `jubler::ProviderResult<T>` (a value or a `jubler::ProviderError`), and the host reports
its own failures the same way (`HttpResponse::error`, `extractSubtitleBytes()`). Inside a plugin exceptions are
fine: throw `jubler::ProviderException` and wrap each provider call in `jubler::guarded()`, which turns it into the
returned error within the plugin. Interface 1.0 plugins (which threw) are no longer loaded; rebuild them against
this API.

## Building

`examples/plugin` uses every extension point; `examples/provider-plugin` is a provider-only plugin. Both build with

    cmake -S examples/plugin -B build-plugin -DJUBLER_API_DIR=<jubler-qt>/src/plugin-api
    cmake --build build-plugin

Plugin metadata (`plugin.json`, referenced by `Q_PLUGIN_METADATA`) needs a `name` and may have a `description`.
Build with the same Qt kit (Qt version no newer than Jubler's, same compiler family) as Jubler: the plugin
shares Jubler's Qt and C++ runtime. Libraries the plugin needs beyond Qt are the plugin's to ship: link them
statically on macOS and Windows.
