## Jubler - a subtitle editor

(C) 2005-2024 Panayotis Katsaloulis
panayotis@panayotis.com


### General

Jubler is a tool to edit text-based subtitles. It can be used an an
authoring software for new subtitles or as a tool to convert,
transform, correct and refine existing subtitles.

It is open source under a liberal (GNU) public licence. It is written
in Java 5.0 (a.k.a. Java 1.5.0 ) in order to be really multi-platform.
It has been tested under Linux (Intel 32 & 64 bit), Windows (XP and 
Vista) and Mac OS X (PPC and Intel). 


### Features

#### General features

* It supports Advanced SubStation, SubStation Alpha, SubRip,
  SubViewer (1 and 2), MicroDVD, MPL2 and Spruce DVD Maestro
  file formats, although it is easy to extend it to support
  other file types.
* All encodings supported by Java platform are also supported here
  (like UTF-8). The user is able to select a list of preferred
  encodings in order to load the localized subtitle files.
* GUI internationalization support through gettext utilities.
* Styles are supported (when saving in SubStation formats or SRT).
  These styles are either specific either per subtitle or per character.
* Translating mode (parent & child editors) is supported
* Graphical preview of the subtitles using the FFmpeg library.
  Current frame, waveform preview and waveform listening is supported.
* Graphically display of subtitles, which can be moved and resized.
* Play the subtitles file using a video player (mplayer). While in
  playing mode the user is able to freely edit the subtitles (and
  inform the player for this change) or add a new subtitle in real
  time
* Mark subtitles with different colors, either when editing or real
  time when playing the video
* Automatically mark any subtitles above a given threshold size
* Spell checking, with support for dictionary selection
* Test the subtitles on the movie and navigate with a full graphical
  interface
* Select areas in the movie just in time and add new subtitles
* Easy installation under all platforms.
  
#### Key editing features:

* Editing individual subtitles
* Splitting
* Joining
* Time shifting
* Frame rate conversion automatically, by user request or using a
  free user factor
* Fixing time inconsistencies such as overlapping with an
  optimization algorithm
* Undo & redo
* Cut, copy, paste, delete areas according to time & color patterns
* Clear areas used for hearing impaired

### Requirements

* Java version 1.8
* MPlayer to view subtitles
* ASpell to spell-check the subtitles


### How to run

If you have downloaded the binary distribution, the installer should have
created an link to your "Start" menu and (possibly) on your desktop. In any
other case go to the Jubler installation directory and issue the following
command:
  java -jar Jubler.jar
On some systems a double click is enough to start the application.

