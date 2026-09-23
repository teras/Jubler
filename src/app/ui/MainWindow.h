/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <functional>

#include <QMainWindow>
#include <QMap>
#include <memory>

#include "app/ui/SubEditor.h"
#include "core/subs/Subtitles.h"
#include "core/undo/UndoList.h"

class QAction;
class QMenu;
class QSplitter;
class QToolBar;
class QMenuBar;
class QToolButton;
class QLabel;
class QStackedWidget;
class SubtitleTableModel;
class SubtitleTableView;
class SubPreview;
class EncodingBar;
class CelebrationPanel;
class AppMediaFile;

// The document window, port of `JubFrame`: menus, toolbar, the subtitle
// table, the editor panel, the encoding bar and the video preview, plus the
// load/save, undo, selection and multi-window plumbing.
namespace jubler { class Tool; class EventListener; class Document; }

class MainWindow : public QMainWindow, public UndoHost, public EditorHost {
    Q_OBJECT
public:
    // The menus: at the start of the toolbar row (the screen menu bar on macOS).
    QMenuBar *menus() const { return menus_; }
    explicit MainWindow(QWidget *parent = nullptr);
    // Convenience for "Clone current": a visible window holding `data`.
    explicit MainWindow(std::unique_ptr<Subtitles> data);
    ~MainWindow() override;

    // --- document access ---
    Subtitles *getSubtitles() const { return subs_.get(); }
    AppMediaFile *getMediaFile() const { return mfile_.get(); }
    UndoList *getUndoList() { return &undo_; }
    SubPreview *getSubPreview() const { return preview_; }
    SubEditor *getSubEditor() const { return subeditor_; }
    MainWindow *jparent() const { return jparent_; }
    void setJParent(MainWindow *p) { jparent_ = p; }
    QList<int> getSelectedRows() const;
    QList<SubEntryPtr> getSelectedSubs() const;
    SubEntryPtr getSelectedRow() const;
    int getSelectedRowIdx() const;
    bool isEmptyCanvas() const;
    bool isEmptyContent() const;
    bool isUnsaved() const { return unsaved_; }
    bool isToolLocked() const;
    QString documentName() const;

    // --- loading / saving ---
    MainWindow *loadFileFromHere(const SubFile &sfile, bool forceIntoSameWindow);
    MainWindow *loadFile(const SubFile &sfile, bool forceIntoSameWindow);
    void saveFile(const SubFile &sfile);
    void newFromVideo(const QString &video);
    // The subtitles inside a media file as a document. `stream` is a remembered
    // container stream (-1: ask); when it is gone or holds another language,
    // the same language is looked up and only then is the user asked again.
    void openEmbedded(const QString &video, int stream, const QString &language);
    void recentMenuCallback(const SubFile *sfile);
    void setSubs(std::unique_ptr<Subtitles> newsubs);
    void enableWindowControls(bool asNewWindow);
    void enableSaveControls();
    void closeWindow(bool unsaveCheck, bool keepApplicationAlive);
    void openDroppedFiles(const QStringList &paths);
    void makeDraggable(QWidget *w);
    QPoint dragOffset_;
    bool dragIgnored_ = true;

    // --- table / selection ---
    void tableHasChanged(const QList<SubEntryPtr> &oldSelections);
    void tableHasChanged() override { tableHasChanged(getSelectedSubs()); }
    void rowHasChanged(int row, bool updateDisplay) override;
    int setSelectedSub(int which, bool updateVisuals);
    int setSelectedSub(const QList<int> &which, bool updateVisuals);
    void bringSelectedRowIntoView(int row);
    void followPlaybackSelection(int idx);
    bool isPlaybackDrivenSelection() const override { return playbackDrivenSelection_; }
    void displaySubData(bool fromUserSelection = false);
    int addSubEntry(const SubEntryPtr &entry);
    void updateStyleMenu();
    void rebuildRecentMenu();
    void applyShortcuts();
    void mediaChanged();
    void updateToolsAvailability();
    void rebuildExternalsMenu();

    // --- UndoHost ---
    void setDoText(const QString &text, bool isUndo) override;
    void resetUndoMark() override { lastChangedSub_.reset(); }
    void setUnsaved(bool unsaved) override;
    void autoHideEncodingBar() override;
    void showInfo() override;
    std::unique_ptr<Subtitles> swapSubtitles(std::unique_ptr<Subtitles> subs) override;
    void setSelectedRows(const QList<int> &rows, bool updateVisuals) override { setSelectedSub(rows, updateVisuals); }

    // --- EditorHost ---
    void keepUndo(const SubEntryPtr &entry) override;
    void subTextChanged() override;
    int selectedRowIdx() const override { return getSelectedRowIdx(); }
    Subtitles *subtitles() const override { return subs_.get(); }
    void setToolsLockMenu(bool locked) override;
    void addUndo(const Subtitles &subs, const QString &name) override { undo_.addUndo(subs, name); }
    void editorAttached(bool attached) override;
    void previewRepaint() override;

    // Encoding bar callbacks.
    void reloadFromBar();
    void applyFormatFromBar(const SubFormatPtr &fmt);
    void showEncodingBar();
    void closeEncodingBar();
    void toggleEncodingBar();

    void enablePreview(bool status);
    void setPreviewOrientation(bool horizontal);
    void resetPreviewPanels();
    void setMaxWaveMenu(bool on);
    void setSnapMenu(bool on);
    void addNewSubtitleAfter() { addNewSubtitle(true); }
    void showCelebration();
    void stopCelebration();
    void setNewVersionCallback(std::function<void(QWidget *)> cb);

protected:
    void closeEvent(QCloseEvent *e) override;
    bool event(QEvent *e) override;
    void dragEnterEvent(QDragEnterEvent *e) override;
    void dropEvent(QDropEvent *e) override;
    bool eventFilter(QObject *obj, QEvent *e) override;
    void showEvent(QShowEvent *e) override;

private:
    void init();
    void buildToolbar();
    void buildMenus();
    void buildLayout();
    void buildPopup();
    QAction *addMenuAction(QMenu *menu, const QString &text, const QString &name, const QKeySequence &key, std::function<void()> fn, bool enabledAtStart = false);
    void setCentral(QWidget *w);

    // File menu actions.
    void fileNew();
    void fileNewChild();
    void fileNewFromVideo();
    void fileNewFromEmbedded();
    void addPluginTools();
    void runPluginTool(const std::shared_ptr<jubler::Tool> &tool);
    bool dispatchPluginEvent(const std::function<bool(jubler::EventListener &, jubler::Document &)> &call);
    bool inPluginEvent_ = false;
    QString mediaPath() const;
    void fileOpen();
    void fileRevert();
    void fileSave();
    void fileSaveAs();
    void fileInformation();
    void fileQuality();
    void filePreferences();
    void fileQuit();
    // Edit menu actions.
    void editCut();
    void editCopy();
    void editPaste();
    void editPasteSpecial();
    void editDeleteEmptyLines();
    void editDeleteSelected();
    void editFindReplace();
    void editRegExp();
    void addNewSubtitle(bool isAfter);
    void splitWith(bool asPrevious);
    void splitInPlace();
    void goTo(char cmd);
    void goToTime();
    void selectAll();
    void setMark(int mark);
    void showTableColumn(int col, bool visible);
    void changeSubtitleStyle(const QString &name);
    void focusToggle();
    void undoRedo(bool isUndo);
    void sortAll();
    void helpUrl(const QString &url);

    // State.
    std::unique_ptr<Subtitles> subs_;
    std::unique_ptr<AppMediaFile> mfile_;
    UndoList undo_;
    MainWindow *jparent_ = nullptr;
    bool unsaved_ = false;
    bool ignoreTableSelections_ = false;
    bool playbackDrivenSelection_ = false;
    SubEntryPtr lastChangedSub_;
    bool registered_ = false;
    std::function<void(QWidget *)> newVersionCb_;

    // Widgets.
    SubtitleTableModel *model_;
    SubtitleTableView *table_;
    SubEditor *subeditor_;
    SubPreview *preview_;
    EncodingBar *encodingBar_;
    CelebrationPanel *celebration_ = nullptr;
    QWidget *basicPanel_;
    QStackedWidget *centerStack_;
    QWidget *subEditP_;
    QSplitter *splitter_;
    QToolBar *toolbar_;
    QMenuBar *menus_ = nullptr;
    QToolBar *newsBar_ = nullptr;   // "New version!" at the end of the toolbar row
    QMenu *popup_;
    QMenu *recentMenu_, *styleMenu_, *stylePopup_, *deleteMenu_, *markMenu_, *markPopup_, *columnsMenu_, *columnsPopup_, *toolsMenu_, *previewMenu_, *externalsMenu_;
    QAction *actSave_, *actSaveAs_, *actRevert_, *actChild_, *actInfo_, *actQuality_, *actUndo_, *actRedo_, *actToolsLock_, *actEnablePreview_, *actMaxWave_, *actSnap_, *actPlayAudio_;
    QList<QAction *> columnActs_, columnPopupActs_;
    QAction *tbSave_, *tbEncoding_, *tbInfo_, *tbQuality_, *tbCut_, *tbCopy_, *tbPaste_, *tbUndo_, *tbRedo_, *tbSort_, *tbPreview_, *tbOrientation_, *tbNewVersion_;
    QMap<QString, QAction *> namedActions_;
    QList<QAction *> editMenuActs_;
    QList<QMenu *> editSubmenus_;
};
