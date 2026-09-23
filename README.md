## Jubler - Subtitle Editor

![GitHub release (latest by date)](https://img.shields.io/github/v/release/teras/Jubler)
![GitHub all releases](https://img.shields.io/github/downloads/teras/Jubler/total)
![GitHub](https://img.shields.io/github/license/teras/Jubler?v=2)
![C++](https://img.shields.io/badge/C%2B%2B-17-blue)
![Qt](https://img.shields.io/badge/Qt-6-green)
![Platform](https://img.shields.io/badge/platform-Linux%20%7C%20macOS%20%7C%20Windows-blue)
![GitHub Release Date](https://img.shields.io/github/release-date/teras/Jubler)

Jubler is a free and open source subtitle editor for creating, editing, and converting text-based subtitles. It provides tools for authoring new subtitles and refining existing ones with video preview, quality validation, and extensive format support.

Jubler is written in C++ with Qt 6, and uses libmpv for video and audio. Up to version 10.0.0 it was a Java application; that code lives on the [`java`](https://github.com/teras/Jubler/tree/java) branch.

(C) 2005-2026 Panayotis Katsaloulis
panayotis@panayotis.com

Licensed under the GNU Affero General Public License v3 (AGPL-3.0)

---

## Features

### Subtitle Formats
- **20+ formats supported** including SubRip (SRT), Advanced SubStation Alpha (ASS/SSA), WebVTT, MicroDVD, SubViewer, MPL2, Spruce DVD Maestro, TTML, DFXP, ITT, QuickTime Texttrack, Adobe Encore and YouTube subtitles
- **Encoding detection** on every load, with UTF-8, UTF-16 and the legacy code pages
- **Character and paragraph-level styling** for formats that support it (ASS/SSA, SRT)
- **Format conversion** between all supported types
- **Import of the text subtitles embedded** in video files

### Video Integration
- **mpv-based** video preview with hardware decoding, audio waveform and video key frames
- **Real-time subtitle editing** while watching video
- **Interactive timeline** with draggable subtitle blocks
- **Two-point synchronization** for timing alignment
- **Play the current subtitle** straight from the player

### Editing Tools
- **Time manipulation** - shift, frame rate conversion, round timing
- **Content operations** - split/join entries, split/join files
- **Text processing** - spell checker, find & replace with regex support
- **Smart time fixer** with overlap detection and resolution
- **Translation mode** for side-by-side subtitle translation
- **Undo/redo** with full history
- **Auto-save and recovery**

### Quality Control
- **Subtitle validation** with configurable quality rules
- **CPS (characters-per-second) metrics** displayed in subtitle table
- **TED guidelines compliance** checking
- **Color-coded validation** to highlight issues
- **Statistics and analysis** tools

### Subtitles and Languages
- **Subtitle search and download** from OpenSubtitles, SubDL and SubSource, with video-hash matching
- **Hunspell spell checker** with a bundled English dictionary and other languages downloaded on demand
- **Azure Translator support** for automated translation

### User Interface
- **HiDPI support** with adjustable scaling
- **Dark theme** available
- **Customizable keyboard shortcuts**
- **Multi-language interface**
- **Color marking** for organizing subtitle groups
- **Preferences of the Java version** offered for import on the first run

### Automation
- **Command-line tools** for batch processing
- **External tool integration** - run command-line tools (speech recognition, sync and more) on the open subtitles, audio or video
- **Plugin system** for subtitle download providers (see [docs/PLUGINS.md](docs/PLUGINS.md))

---

## Getting Started

### Installation

Download binaries from the [releases page](https://github.com/teras/Jubler/releases).

Or build from source (see [BUILD_AND_RUN.md](BUILD_AND_RUN.md))

### Running

Launch from your application menu or desktop shortcut. You can also run it from a terminal, with the subtitle files to open:
```bash
jubler subtitle.srt
```

`jubler --help` lists the command-line options, and `jubler --list-tools` the tools available for batch processing.

---

## Contributing

Jubler is an open source project that welcomes contributions. Whether you're fixing bugs, adding features, improving documentation, or translating the interface, your help is appreciated.

---

## Credits

Free code signing on Windows provided by [SignPath.io](https://signpath.io/), certificate by [SignPath Foundation](https://signpath.org/)
