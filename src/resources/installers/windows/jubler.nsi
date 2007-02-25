; jubler.nsi
;--------------------------------


; MACROS FOR FILE ASSOCIATION

; fileassoc.nsh
; File association helper macros
; Written by Saivert
;
; Features automatic backup system and UPDATEFILEASSOC macro for
; shell change notification.
;
; |> How to use <|
; To associate a file with an application so you can double-click it in explorer, use
; the APP_ASSOCIATE macro like this:
;
;   Example:
;   !insertmacro APP_ASSOCIATE "txt" "myapp.textfile" "$INSTDIR\myapp.exe,0" \
;     "Open with myapp" "$INSTDIR\myapp.exe $\"%1$\""
;
; And finally: To remove the association from the registry use the APP_UNASSOCIATE
; macro. Here is another example just to wrap it up:
;   !insertmacro APP_UNASSOCIATE "txt" "myapp.textfile"
;
 
!macro APP_ASSOCIATE EXT FILECLASS DESCRIPTION ICON COMMANDTEXT COMMAND
  ; Backup the previously associated file class
  ReadRegStr $R0 HKCR ".${EXT}" ""
  WriteRegStr HKCR ".${EXT}" "${FILECLASS}_backup" "$R0"
 
  WriteRegStr HKCR ".${EXT}" "" "${FILECLASS}"
 
  WriteRegStr HKCR "${FILECLASS}" "" `${DESCRIPTION}`
  WriteRegStr HKCR "${FILECLASS}\DefaultIcon" "" `${ICON}`
  WriteRegStr HKCR "${FILECLASS}\shell" "" "open"
  WriteRegStr HKCR "${FILECLASS}\shell\open" "" `${COMMANDTEXT}`
  WriteRegStr HKCR "${FILECLASS}\shell\open\command" "" `${COMMAND}`
!macroend
 
!macro APP_UNASSOCIATE EXT FILECLASS
  ; Backup the previously associated file class
  ReadRegStr $R0 HKCR ".${EXT}" `${FILECLASS}_backup`
  WriteRegStr HKCR ".${EXT}" "" "$R0"
 
  DeleteRegKey HKCR `${FILECLASS}`
!macroend
 
 
; !defines for use with SHChangeNotify
!ifdef SHCNE_ASSOCCHANGED
!undef SHCNE_ASSOCCHANGED
!endif
!define SHCNE_ASSOCCHANGED 0x08000000
!ifdef SHCNF_FLUSH
!undef SHCNF_FLUSH
!endif
!define SHCNF_FLUSH        0x1000
 
!macro UPDATEFILEASSOC
; Using the system.dll plugin to call the SHChangeNotify Win32 API function so we
; can update the shell.
  System::Call "shell32::SHChangeNotify(i,i,i,i) (${SHCNE_ASSOCCHANGED}, ${SHCNF_FLUSH}, 0, 0)"
!macroend
 
;EOF

; END OF MACROS FOR FILE ASSOCIATION




!include "MUI.nsh"

; The name of the installer
Name "Jubler subtitle editor"

; The file to write
OutFile "Jubler-${VERSION}.exe"

; The default installation directory
InstallDir "$PROGRAMFILES\Jubler"

; Registry key to check for directory (so if you install again, it will 
; overwrite the old one automatically)
InstallDirRegKey HKLM "Software\Jubler" "Install_Dir"

;--------------------------------

!define MUI_BGCOLOR aabbaa
!define MUI_ABORTWARNING
!define MUI_ICON "resources/installers/windows/install.ico"
!define MUI_UNICON "resources/installers/windows/install.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "resources/installers/windows/logo-install.bmp"
!define MUI_COMPONENTSPAGE_SMALLDESC

; Other parameters
LicenseForceSelection checkbox



;--------------------
; Pages

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "LICENCE"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"

;--------------------------------


; The stuff to install
Section "Jubler editor" SecJubler

  SectionIn RO
  
  ; Set output path to the installation directory.
  SetOutPath $INSTDIR
  File dist\Jubler.exe
  File ChangeLog
  Rename ChangeLog ChangeLog.txt
  File LICENCE
  Rename LICENCE LICENCE.txt
  File README
  Rename README README.txt
   
  ; Create library
  SetOutPath $INSTDIR\lib
  File dist\lib\ffdecode.dll
  File resources\installers\windows\subtitle.ico

  ; Create help directory
  SetOutPath $INSTDIR\help
  File dist\help\jubler-faq.html
  File resources\help\question.png


  ; Write the installation path into the registry
  WriteRegStr HKLM "Software\Jubler" "Install_Dir" "$INSTDIR"
  
  ; Write the uninstall keys for Windows
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Jubler" "DisplayName" "Jubler subtitle editor"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Jubler" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Jubler" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Jubler" "NoRepair" 1
  WriteUninstaller "uninstall.exe"
  
  ; Associate subtitle files
  !insertmacro APP_ASSOCIATE "ass" "jubler.subfile.ass" "ASS Subtitle file" "$INSTDIR\lib\subtitle.ico,0" "Open with Jubler" "$INSTDIR\Jubler.exe $\"%1$\""
  !insertmacro APP_ASSOCIATE "ssa" "jubler.subfile.ssa" "SSA Subtitle file" "$INSTDIR\lib\subtitle.ico,0" "Open with Jubler" "$INSTDIR\Jubler.exe $\"%1$\""
  !insertmacro APP_ASSOCIATE "sub" "jubler.subfile.sub" "SUB Subtitle file" "$INSTDIR\lib\subtitle.ico,0" "Open with Jubler" "$INSTDIR\Jubler.exe $\"%1$\""
  !insertmacro APP_ASSOCIATE "srt" "jubler.subfile.srt" "SRT Subtitle file" "$INSTDIR\lib\subtitle.ico,0" "Open with Jubler" "$INSTDIR\Jubler.exe $\"%1$\""
;  !insertmacro UPDATEFILEASSOC

SectionEnd



;--------------------------------
; Shortcuts etc.


Section "Start Menu Shortcuts" SecStartMenu

  CreateDirectory "$SMPROGRAMS\Jubler"
  CreateShortCut "$SMPROGRAMS\Jubler\Uninstall.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
  CreateShortCut "$SMPROGRAMS\Jubler\Jubler subtitle editor.lnk" "$INSTDIR\Jubler.exe" "" "$INSTDIR\Jubler.exe" 0

SectionEnd

;--------------------------------

Section "Desktop Icon" SecDesktop

  CreateShortCut "$DESKTOP\Jubler subtitle editor.lnk" "$INSTDIR\Jubler.exe" "" "$INSTDIR\Jubler.exe" 0

SectionEnd




;--------------------------------

LangString DESC_SecJublerMain ${LANG_ENGLISH} "Required Jubler subtitle editor program files."
LangString DESC_SecStartMenu ${LANG_ENGLISH} "Add Start Menu Icons."
LangString DESC_SecDesktop ${LANG_ENGLISH} "Add Desktop Icon."


!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecJubler} $(DESC_SecJublerMain)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecStartMenu} $(DESC_SecStartMenu)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecDesktop} $(DESC_SecDesktop)
!insertmacro MUI_FUNCTION_DESCRIPTION_END






;--------------------------------

; Uninstaller

Section "Uninstall"
  
  ; Remove registry keys
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Jubler"
  DeleteRegKey HKLM "Software\Jubler"

  ; Remove files and uninstaller
  Delete "$INSTDIR\lib\*.*"
  Delete "$INSTDIR\help\*.*"
  Delete "$INSTDIR\*.*"

  ; Remove shortcuts, if any
  Delete "$SMPROGRAMS\Jubler\*.*"

  ; Remove Desctop shortcut
  Delete "$DESKTOP\Jubler subtitle editor.lnk"
  Delete "$DESKTOP\Jubler.lnk"

  ; Remove directories used
  RMDir "$SMPROGRAMS\Jubler"
  RMDir "$INSTDIR\lib"
  RMDir "$INSTDIR\help"
  RMDir "$INSTDIR"

  ; Remove associations
  !insertmacro APP_UNASSOCIATE "ass" "jubler.subfile.ass"
  !insertmacro APP_UNASSOCIATE "ssa" "jubler.subfile.ssa"
  !insertmacro APP_UNASSOCIATE "sub" "jubler.subfile.sub"
  !insertmacro APP_UNASSOCIATE "srt" "jubler.subfile.srt"
;  !insertmacro UPDATEFILEASSOC

SectionEnd
