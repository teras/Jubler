; (c) 2005-2026 by Panayotis Katsaloulis
; SPDX-License-Identifier: AGPL-3.0-only
; This file is part of Jubler.

; The Windows installer, as the Java release's (per machine, start menu, desktop
; icon, uninstaller, the same file associations). Build with
;   ISCC /DAppVersion=<version> /DAppNumericVersion=<a.b.c.0> /DSourceDir=<deployed files> jubler.iss

#define AppName "Jubler"
#ifndef AppVersion
  #define AppVersion "0.0.0"
#endif
#ifndef AppNumericVersion
  #define AppNumericVersion "0.0.0.0"
#endif
#ifndef SourceDir
  #define SourceDir "app"
#endif

[Setup]
; The Java installer's id: installing over a Java Jubler upgrades it.
AppId={{84E58A62-0000-0000-0000-000000000000}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher=Panayotis Katsaloulis
AppPublisherURL=https://jubler.org
AppSupportURL=https://github.com/teras/Jubler/issues
VersionInfoVersion={#AppNumericVersion}
DefaultDirName={commonpf}\{#AppName}
DefaultGroupName={#AppName}
OutputDir=.
OutputBaseFilename={#AppName}-{#AppVersion}-x64
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
DisableReadyPage=yes
DisableWelcomePage=no
UninstallDisplayIcon={app}\jubler.exe
CreateAppDir=yes
UsePreviousAppDir=no
WizardResizable=no
ShowLanguageDialog=no
ArchitecturesInstallIn64BitMode=x64compatible
ArchitecturesAllowed=x64compatible
SetupIconFile=install.ico
ChangesAssociations=yes

[Messages]
WelcomeLabel1=Welcome to the [name] Setup Wizard
WelcomeLabel2=This will install [name/ver] on your computer.%n%nIt is recommended that you close all other applications before continuing.
ClickNext=Click Next to continue.
FinishedHeadingLabel=[name] has been successfully installed

[InstallDelete]
; What a Java installation left there: its application jars and Java runtime.
Type: filesandordirs; Name: "{app}\app"
Type: filesandordirs; Name: "{app}\runtime"

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "document.ico"; DestDir: "{app}"; Flags: ignoreversion

[UninstallDelete]
Type: dirifempty; Name: "{app}"

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\jubler.exe"; WorkingDir: "{app}"
Name: "{group}\Uninstall {#AppName}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\jubler.exe"

[Registry]
Root: HKCR; Subkey: ".srt"; ValueType: string; ValueName: ""; ValueData: "{#AppName}"; Flags: uninsdeletevalue
Root: HKCR; Subkey: ".vtt"; ValueType: string; ValueName: ""; ValueData: "{#AppName}"; Flags: uninsdeletevalue
Root: HKCR; Subkey: ".ass"; ValueType: string; ValueName: ""; ValueData: "{#AppName}"; Flags: uninsdeletevalue
Root: HKCR; Subkey: ".ssa"; ValueType: string; ValueName: ""; ValueData: "{#AppName}"; Flags: uninsdeletevalue
Root: HKCR; Subkey: ".ttml"; ValueType: string; ValueName: ""; ValueData: "{#AppName}"; Flags: uninsdeletevalue
Root: HKCR; Subkey: ".dfxp"; ValueType: string; ValueName: ""; ValueData: "{#AppName}"; Flags: uninsdeletevalue
Root: HKCR; Subkey: ".itt"; ValueType: string; ValueName: ""; ValueData: "{#AppName}"; Flags: uninsdeletevalue
Root: HKCR; Subkey: ".txt"; ValueType: string; ValueName: ""; ValueData: "{#AppName}"; Flags: uninsdeletevalue
Root: HKCR; Subkey: ".sub"; ValueType: string; ValueName: ""; ValueData: "{#AppName}"; Flags: uninsdeletevalue
Root: HKCR; Subkey: ".stl"; ValueType: string; ValueName: ""; ValueData: "{#AppName}"; Flags: uninsdeletevalue
Root: HKCR; Subkey: ".xml"; ValueType: string; ValueName: ""; ValueData: "{#AppName}"; Flags: uninsdeletevalue
Root: HKCR; Subkey: ".sbv"; ValueType: string; ValueName: ""; ValueData: "{#AppName}"; Flags: uninsdeletevalue
Root: HKCR; Subkey: "{#AppName}"; ValueType: string; ValueName: ""; ValueData: "Jubler subtitle file"; Flags: uninsdeletekey
Root: HKCR; Subkey: "{#AppName}\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\document.ico,0"; Flags: uninsdeletekey
Root: HKCR; Subkey: "{#AppName}\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\jubler.exe"" ""%1"""; Flags: uninsdeletekey

[Run]
Filename: "{app}\jubler.exe"; Description: "Launch {#AppName}"; Flags: nowait postinstall skipifsilent
