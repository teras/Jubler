### Jubler Changelog

#### 8.0.0

Redesigned HiDPI UI with editable scaling. Reintroduced Auto-translate with Azure. Support Youtube subtitles.

#### 7.0.3

Stability fixes. Updated ASS/SSA support.

#### 7.0.0

Embed JRE. Update icon. Timeline updates. Improved keyboard navigation

#### 6.0.2

Fix MPlayer issues with OSX and Windows.

#### 6.0

Fix various issues. Up-to date with modern OS’s. Quality controls, compatible with TED guidelines. Linux distribution as AppImage

#### 5.1

SubRip filter now better recognises broken subtitles. In OS X, only Oracle’s JRE is officially supported and PPC support is dropped.

#### 5.0.6

Mac OS X only version. Fix packaging issues and updated to latest java launcher.

#### 5.0.5

Double-click on project properly works. Support CPM in main subtitle view. Fixed Regular expressions. Fix select-all.

#### 5.0.1

Fix Preferences/About menu issue with Windows and Linux. Support older JRE and PPC under Mac OS X.

#### 5.0

Updates to 32 / 64 bit support depending on the operating system. Various bug fixes.

#### 4.6.1

Fixed ffdecode library for 64 bit Mac OS X. Dutch translation.

#### 4.6

Plugin system introduced. Updates to GoogleTranslate, MPlayer. Many bug fixes ans minor improvements.

#### 4.1.3

Fixed Save not working. Fixed Recents menu, when display non-existent entries.

#### 4.1.2

Fixed a bug in x64 version of Windows.

#### 4.1.1

Updated auto-update library. Fixed bug not updating Recents menu. Fixed bug aggressively truncating save file name. Modular translation files.

#### 4.1

Only one instance of Jubler is now run in a system. Every file remembers loading parameters and uses it when saving. Improved load/save dialog, which encapsulates the information inside the load dialog. General GUI improvements, especially in Mac OSX. Relaxed file loading, when file contains encoding errors. Improved subtitle support. Improved visuals in Video Console window. More compact Preferences dialog. Minor changes and bug fixes.

#### 4.0

Auto-update support. Jubler is able to smart update with minimum user interaction. Improved time fixed tool for overlapping subtitles. Improved FFMPEG color model.

#### 3.9.6

Improved Google translator parsing engine. Fixed compile issues with ffmpeg under OS X 10.4. Fixed Linux GTK L&F bug with time-spinner under Java 6.0.

#### 3.9.5

Autosave/recovery of changed subtitles. Automatic translate of subtitles through Google. Updated to latest FFMPEG. Small GUI improvements. Better SRT support. Minor bug fixes and enhancements.

#### 3.9.0

Improved user interface. Preview interface integrated inside main Jubler window. Subtitle number is now visible, if desired. Use of preset file encodings. Visual display, using color, of affected subtitles. Maximize waveform visualization. Support of AAC audio streams.

#### 3.4.1

MPlayer uses threads to properly handle out and error stream. Updated to latest ffmpeg library. Support of correct MIME type under Linux.

#### 3.4.0

SRT supports font formatting. Subtitle statistics. Better handling of preview bars. Improved splitting interface. Updated Media files configuration. Display number of lines & characters per line while editing. MPlayer display formatted subtitles - use of ASS format instead of SRT. Linux man & doc paths fixed. Support for Quicktime Texttrack subtitles. Fixed an error with MPL2 subtitles. Frame icon now properly display under Windows/Linux. Quit confirmation dialogue properly works under Mac. Check for new version only once per day. Better display of selected subtitles. Fixed serious memory allocation error in Windows. Various bug fixes and updates.

#### 3.3.0

Automatic detection of MPlayer/ASpell executables. Preview window GUI updates. Alt-mousewheel / alt-click slows down rotation in JTimeSpinner boxes. Support for Spruce DVDMaestro (STL) and MPL2 subtitles (without formatting). Fixed autoload for Java 1.6 in Linux. Added missing tooltips. Fixed gettext bug with ’ character. Improved ffmpeg support. Keep application alive if “Close window” is selected. New window will not fully overlap old one. Spanish, French and Serbian translations introduced. Czech, German, Portuguese and Greek translation corrections. Various bug fixes and usability improvements.

#### 3.2.0

Doube click on icons launch Jubler (in Mac OSX, Windows and Debian systems). MPlayer fixes. Jubler installs in systems with JRE 1.6. MPlayer supports different audio stream. Video playback doesn’t change system mixers. FFMPEG time offset and WMV bug fixes. Better handling of audio caches. Updated to newer version of FFMPEG. ASpell updates supported languages when opening preferences. Major internal changes and cleanups. Initial Debian package.

#### 3.1.0

Read frames per second from the movie now supported. Movie synchronization on the fly, based on time differences between time points supported. Updated recoring of subtitles on the fly. A lot of fine tuning. Zemberek updates.

#### 3.0.0

FFDecode bugs fixed, preview should be much more stable and accurate. Preview bug fixed, where wrong duration was reported. Tools remember last values. Column width is now saved. Now it is compatible with latest version of MPlayer. Updated to newest ffmpeg sources. Turkish translation added. Support for zemberek added. Preliminary built-in FAQ subsystem. Tweaks and code changes. New icons and splash screen. Linux installation script fixes. Supports cocoASpell under Mac OSX.

#### 3.0.0-rc4

Fixed bugs: audio line not closing, saving file with an illegal character set, banner not closing, Jubler crashing when audio codec could not be opened, Jubler didn’t start under Linux. Mac OSX port is now universal binary. Synchronize subtitle texts and timestamps. Subtitle preview updates live on key events. Automatic version check through internet. Smaller code changes.

#### 3.0.0-rc3

Initial Mac OS X version. Big endian bugs fixed. “Quit” introduced. Menu shortcuts are now editable. MPlayer threading issues fixed. Cache is created only when needed. Full AC3 support.

#### 3.0.0-rc2

Great speedup improvements on subtitle preview. Alpha channel values in SSA/ASS format flipped. MPlayer options now editable. Recents are sorted according to their call. FIxed bug with the installer not properly detecting JRE. Subtitle preview fixes.

#### 3.0.0-rc1

Czech translation. Subtitle text display on frame preview (draft & full details) using Java 2D. Default display frame when no library is present, or no actual frame preview is required. When FFDecode library is missing, a notice is displayed and no more a popup dialog appers. Subtitle is movable on wave panel too (in preview). Display subtitle time when moving subtitles in preview. Revert, clone & open recent subtitles in File menu. Video preview in normal and half size. Reparent current subtitles (for translations). MPlayer now supports default font size (and font name in Linux). Only static version of ffmpeg for linux is provided. Varius bug fixes.

#### 2.9.9

Preview of subtitles added (dependance on FFMPEG library). Frame preview of the current frame, waveform preview and waveform listening is supported. Graphically display of subtitles, which can be moved and resized. Jubler no longer distributed as JAR file but using self expandable platform specific installers (due to FFMPEG). Mouse wheel over subtitle time changes its value. Various bug fixes and other enchacements.

#### 2.5.1

Fix while trying to save a splitted subtitle file.

#### 2.5.0

Suport for SubStation Alpha (SSA), Advanced SubStation (ASS), and SubViewer (1+2) subtitle formats. A German translation. Styles are supported (when saving in SubStation formats). A translating mode. A lot of fine tuning and bugfixes.

#### 2.0.0

Mostly code cleanup and changes in the “about” box.

#### 2.0-rc1

This release adds I18n support through gettext.

#### 1.9.3

Find & replace and global replace features were added. The focus of the subtitle textbox was fixed.

#### 1.9.1

Enhancements were made to the command line arguments of mplayer in order to play correctly under the Windows ports of MPlayer. A few unimportant corrections were made to the ASpell options.

#### 1.9.0

This release has great improvements in the mplayer frontend, ASpell support, GUI lifting, and new tools.

#### 0.9

Initial announcement

