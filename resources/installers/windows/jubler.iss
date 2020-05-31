#define AppName "Jubler"
#define AppDescr "Jubler Subtitle Editor"
#define AppUrl "https://jubler.org"

[Setup]
AppName={#AppName}
DefaultDirName={commonpf}\{#AppName}
DefaultGroupName={#AppName}
AppVersion=@VERSION@
SetupIconFile=install.ico
OutputBaseFilename={#AppName}-@VERSION@
AppPublisher=Panayotis Katsaloulis
AppPublisherURL={#AppUrl}
DisableReadyPage=yes
UninstallDisplayIcon={app}\{#AppName}.exe
AllowNoIcons=yes
ArchitecturesInstallIn64BitMode=@INSTALLMODE@
ChangesAssociations=yes
WizardImageFile=logo-install.bmp
WizardSmallImageFile=logo-install-small@2x.bmp,logo-install-small.bmp

[Files]
Source:"jubler\*"; DestDir:"{app}"; Flags: recursesubdirs

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppName}.exe"; WorkingDir: "{app}"
Name: "{group}\Uninstall {#AppName}"; Filename: "{uninstallexe}"
Name: "{commondesktop}\{#AppName}"; Filename: "{app}\{#AppName}.exe"

[Registry]
Root: HKCR; Subkey: ".ass";                          ValueData: "{#AppName}";  Flags: uninsdeletevalue; ValueType: string;  ValueName: ""
Root: HKCR; Subkey: ".ssa";                          ValueData: "{#AppName}";  Flags: uninsdeletevalue; ValueType: string;  ValueName: ""
Root: HKCR; Subkey: ".sub";                          ValueData: "{#AppName}";  Flags: uninsdeletevalue; ValueType: string;  ValueName: ""
Root: HKCR; Subkey: ".srt";                          ValueData: "{#AppName}";  Flags: uninsdeletevalue; ValueType: string;  ValueName: ""
Root: HKCR; Subkey: ".stl";                          ValueData: "{#AppName}";  Flags: uninsdeletevalue; ValueType: string;  ValueName: ""
Root: HKCR; Subkey: ".son";                          ValueData: "{#AppName}";  Flags: uninsdeletevalue; ValueType: string;  ValueName: ""
Root: HKCR; Subkey: "{#AppName}";                    ValueData: "{#AppDescr}"; Flags: uninsdeletekey;   ValueType: string;  ValueName: ""
Root: HKCR; Subkey: "{#AppName}\DefaultIcon";        ValueData: "{app}\subtitle.ico,0";          ValueType: string; ValueName: ""
Root: HKCR; Subkey: "{#AppName}\shell\open\command"; ValueData: """{app}\{#AppName}.exe"" ""%1"""; ValueType: string; ValueName: ""
