# How to Build and Run Jubler

## Prerequisites

- A C++17 compiler and CMake 3.19 or newer
- Qt 6: Core, Gui, Widgets, OpenGLWidgets, Network, Svg, Concurrent, Xml and LinguistTools
- libmpv, and the development files of the FFmpeg libraries it links (libavformat, libavcodec, libavutil,
  libswresample). The headers must be the same major versions libmpv uses; CMake checks this.
- hunspell, libcrypto (OpenSSL) and zlib
- pkg-config

## Build & run

```bash
cmake -S . -B build
cmake --build build -j8
./build/jubler
```

With arguments:

```bash
./build/jubler subtitle.srt
./build/jubler --help
./build/jubler --list-tools
./build/jubler --load input.ass --save output.srt
```

## Common tasks

```bash
ctest --test-dir build                  # run the tests
cmake -S . -B build -DJUBLER_BUILD_APP=OFF   # only the core library and the tests
tools/i18n-tools.py update              # after changing user-visible strings
```

The interface strings are the English ones in the source (`__("…")`); `tools/i18n-tools.py update` collects them
into `resources/i18n-src/*.json`, where the translations live, and regenerates the Qt `.ts` files.

## Installation layout (Linux)

`cmake --install build` installs the `jubler` executable together with its desktop entry, AppStream metadata,
MIME types for the subtitle formats and the icon.

## Versioning

The version is derived automatically from the latest `v*` git tag (e.g. tag `v10.0.0` builds version `10.0.0`);
there is no manual version-bump step.
