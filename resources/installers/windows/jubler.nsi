; jubler.nsi
;--------------------------------

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


SetCompressor /SOLID lzma

;--------------------------------

!define MUI_BGCOLOR aabbaa
!define MUI_ABORTWARNING
!define MUI_ICON "resources/installers/windows/install.ico"
!define MUI_UNICON "resources/installers/windows/install.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "resources/installers/windows/logo-install.bmp"
!define MUI_COMPONENTSPAGE_SMALLDESC

; Other parameters
LicenseForceSelection checkbox


!include "resources/installers/windows/assoc.nsh"

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
  File ChangeLog.html
  File dist\LICENCE.txt
  File dist\README.txt
   
  ; Create library
  SetOutPath $INSTDIR\lib
  File dist\lib\ffdecode.dll
  File resources\installers\windows\subtitle.ico
  File resources\jupidator\jupidator.jar

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
  !insertmacro APP_ASSOCIATE "stl" "jubler.subfile.stl" "STL Subtitle file" "$INSTDIR\lib\subtitle.ico,0" "Open with Jubler" "$INSTDIR\Jubler.exe $\"%1$\""
  !insertmacro APP_ASSOCIATE "son" "jubler.subfile.son" "SON Subtitle file" "$INSTDIR\lib\subtitle.ico,0" "Open with Jubler" "$INSTDIR\Jubler.exe $\"%1$\""
;  !insertmacro UPDATEFILEASSOC

SectionEnd


; Create Start menu shortcuts
;--------------------------------
Section "Start Menu Shortcuts" SecStartMenu
  CreateDirectory "$SMPROGRAMS\Jubler"
  CreateShortCut "$SMPROGRAMS\Jubler\Uninstall.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
  CreateShortCut "$SMPROGRAMS\Jubler\Jubler subtitle editor.lnk" "$INSTDIR\Jubler.exe" "" "$INSTDIR\Jubler.exe" 0
SectionEnd


; Create Desktop shortcuts
;--------------------------------
Section "Desktop Icon" SecDesktop
  CreateShortCut "$DESKTOP\Jubler subtitle editor.lnk" "$INSTDIR\Jubler.exe" "" "$INSTDIR\Jubler.exe" 0
SectionEnd



; JRE Installation
;--------------------------------
!define JRE_VERSION "1.6"
!define JRE_URL "http://www.jubler.org/support/jre-6-windows-i586.exe"
Section "Java Runtime Environment" SecJRE

  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  StrCmp $2 ${JRE_VERSION} done	; We have already the correct version of JRE

  StrCpy $2 "$TEMP\Java Runtime Environment 6.exe"
  nsisdl::download /TIMEOUT=30000 ${JRE_URL} $2
  Pop $R0 ;Get the return value
  StrCmp $R0 "success" done
    MessageBox MB_OK|MB_ICONEXCLAMATION "Download failed ($R0).$\nRemember to manually download Java 6 before launching Jubler."
    Quit
  done:
  HideWindow
  ExecWait $2 $0
  BringToFront
  Delete $2

SectionEnd

; Mplayer Installation



;--------------------------------

LangString DESC_SecJublerMain ${LANG_ENGLISH} "Required Jubler subtitle editor program files."
LangString DESC_SecJRE ${LANG_ENGLISH} "Test for Java Runtime Environment and download if needed."
LangString DESC_SecStartMenu ${LANG_ENGLISH} "Add Start Menu Icons."
LangString DESC_SecDesktop ${LANG_ENGLISH} "Add Desktop Icon."


!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecJubler} $(DESC_SecJublerMain)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecJRE} $(DESC_SecJRE)
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
  !insertmacro APP_UNASSOCIATE "stl" "jubler.subfile.stl"
  !insertmacro APP_UNASSOCIATE "son" "jubler.subfile.son"
;  !insertmacro UPDATEFILEASSOC

SectionEnd
