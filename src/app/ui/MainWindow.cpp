/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include <QPointer>
#include <QFutureWatcher>
#include <QInputDialog>
#include <QShortcut>
#include <QRegularExpression>
#include <QProgressDialog>
#include <QtConcurrent>
#include <atomic>

#include "app/ui/MainWindow.h"

#include <QAction>
#include <QApplication>
#include <QWindow>
#include <QMouseEvent>
#include <QMimeData>
#include <QDropEvent>
#include <QDragEnterEvent>
#include <QCloseEvent>
#include <QDesktopServices>
#include <QFileDialog>
#include <QDir>
#include <QFileInfo>
#include <QHeaderView>
#include <QLabel>
#include <QMenu>
#include <QMenuBar>
#include <QMessageBox>
#include <QSplitter>
#include <QStackedWidget>
#include <QToolBar>
#include <QToolButton>
#include <QUrl>
#include <QVBoxLayout>
#include <QWindowStateChangeEvent>

#include "app/AppContext.h"
#include "app/Theme.h"
#include "app/dialogs/AboutDialog.h"
#include "app/dialogs/EncodingBar.h"
#include "app/dialogs/InformationDialog.h"
#include "app/dialogs/PasterDialog.h"
#include "app/dialogs/QualityDialog.h"
#include "app/dialogs/ReplaceDialog.h"
#include "app/dialogs/SubFileDialog.h"
#include "app/dialogs/TimeSelectionDialogs.h"
#include "app/externals/RecipeDialogs.h"
#include "app/media/EmbeddedSubtitles.h"
#include "app/media/MediaProbe.h"
#include "app/media/AppMediaFile.h"
#include "app/media/SubPreview.h"
#include "app/options/PreferencesDialog.h"
#include "app/tools/ToolRunner.h"
#include "app/update/AutoUpdater.h"
#include "app/ui/CelebrationPanel.h"
#include "app/ui/SubtitleTableModel.h"
#include "app/ui/SubtitleTableView.h"
#include "core/i18n/I18N.h"
#include "core/plugins/PluginHost.h"
#include "core/options/AutoSaveOptions.h"
#include "core/options/Options.h"
#include "core/os/AutoSaver.h"
#include "core/os/Debug.h"
#include "core/os/FileCommunicator.h"
#include "core/os/SystemDependent.h"
#include "core/tools/CoreTools.h"

namespace {
constexpr double NEW_SUB_DURATION = 2.0;   // seconds
constexpr double NEW_SUB_GAP = 0.0;
// A menu item that is not a command of the shortcuts page: the recent files
// (their Ctrl+1..9 come with the list) and the help links. As the Java.
const QString IGNORED = QStringLiteral("ignore");
}  // namespace

MainWindow::MainWindow(QWidget *parent) : QMainWindow(parent), undo_(this) {
    init();
}

MainWindow::MainWindow(std::unique_ptr<Subtitles> data) : QMainWindow(nullptr), undo_(this) {
    init();
    show();
    setSubs(std::move(data));
}

MainWindow::~MainWindow() = default;

void MainWindow::init() {
    setAttribute(Qt::WA_DeleteOnClose, false);
    AppContext::setCurrentWindow(this);
    mfile_ = std::make_unique<AppMediaFile>();
    setWindowIcon(Theme::logo());
    setWindowTitle(QStringLiteral("Jubler"));
    setUnifiedTitleAndToolBarOnMac(true);
    model_ = new SubtitleTableModel(this);
#ifdef Q_OS_MACOS
    menus_ = menuBar();   // the screen menu bar
#else
    // The menus share the toolbar's row (the window keeps its native title
    // bar). The window's own menu bar is an empty hidden placeholder: styles
    // such as Breeze call QMainWindow::menuBar() while polishing, which would
    // otherwise create a visible empty one.
    menus_ = new QMenuBar(this);
    menus_->setNativeMenuBar(false);   // never the Plasma global menu (as the Java)
    auto *placeholder = new QMenuBar(this);
    placeholder->setNativeMenuBar(false);
    setMenuBar(placeholder);
    placeholder->hide();
#endif
    buildToolbar();
    buildLayout();
    buildMenus();
#ifndef Q_OS_MACOS
    // Exactly as big as the menus. A QMenuBar is made to span a whole window
    // (it grows in both directions): in the toolbar it took the free room, and
    // its menus sat at the top of the stretched row. At its own size the
    // toolbar centres it like any other widget.
    menus_->setSizePolicy(QSizePolicy::Fixed, QSizePolicy::Fixed);
#endif
    buildPopup();
    ToolRunner::registerMenus(this, toolsMenu_, deleteMenu_, markMenu_, styleMenu_);
    addPluginTools();
    rebuildExternalsMenu();
    subeditor_->setToolsLocked(false);
    enablePreview(false);
    setPreviewOrientation(AutoSaveOptions::getPreviewOrientation());
    applyShortcuts();
    AppContext::putWindowPosition(this);
    resize(std::max(width(), AppContext::DEFAULT_WIDTH), std::max(height(), AppContext::DEFAULT_HEIGHT));
    if (!AppContext::celebrationShown()) {
        AppContext::setCelebrationShown();
        showCelebration();
    }
    setDoText(QString(), true);
    setDoText(QString(), false);
    showInfo();
    for (const auto &l : PluginHost::eventListeners()) {
        try {
            l->windowCreated(this);
        } catch (...) {
            Debug::debug(QStringLiteral("A plugin failed while a window was created"));
        }
    }
}

// Runs `call` for every plugin listener on the current document (stops at
// the first false). Changes a listener makes become one undo step.
bool MainWindow::dispatchPluginEvent(const std::function<bool(jubler::EventListener &, jubler::Document &)> &call) {
    if (!subs_ || PluginHost::eventListeners().isEmpty() || inPluginEvent_) return true;
    inPluginEvent_ = true;
    const QPointer<MainWindow> self(this);
    const QList<SubEntryPtr> selected = getSelectedSubs();
    auto snapshot = std::make_unique<UndoEntry>(*subs_, __("Plugin changes"));
    PluginDocument doc(*subs_, getSelectedRows(), mediaPath());
    bool ok = true;
    for (const auto &l : PluginHost::eventListeners()) {
        try {
            if (!call(*l, doc)) ok = false;
        } catch (...) {
            Debug::debug(QStringLiteral("A plugin event listener failed"));
        }
        if (!self) return false;   // a listener closed the window
        if (!ok) break;
    }
    if (doc.changed()) {
        undo_.addUndo(std::move(snapshot));
        tableHasChanged(selected);
    }
    inPluginEvent_ = false;
    return ok;
}

// Java String.trim(): only characters up to U+0020 (Qt's trimmed() also
// strips no-break and other Unicode spaces).
QString javaTrim(const QString &s) {
    int a = 0, b = int(s.size());
    while (a < b && s.at(a).unicode() <= 0x20) ++a;
    while (b > a && s.at(b - 1).unicode() <= 0x20) --b;
    return s.mid(a, b - a);
}

// ---- plugins ------------------------------------------------------------------------------------

// The plugins' tools, in the Tools menu before Preview/Externals.
void MainWindow::addPluginTools() {
    const auto &tools = PluginHost::tools();
    if (tools.isEmpty()) return;
    QAction *before = previewMenu_->menuAction();
    toolsMenu_->insertSeparator(before);
    for (const auto &tool : tools) {
        QString id = tool->id();
        id.replace(QRegularExpression(QStringLiteral("[^A-Za-z0-9_]")), QStringLiteral("_"));
        QAction *a = addMenuAction(toolsMenu_, tool->menuText(), QStringLiteral("PLG_") + id, QKeySequence(), [this, tool]() { runPluginTool(tool); }, true);
        toolsMenu_->removeAction(a);
        toolsMenu_->insertAction(before, a);
    }
}

QString MainWindow::mediaPath() const {
    const VideoFile *v = mfile_ ? mfile_->getVideoFile() : nullptr;
    return v && v->exists() ? v->getPath() : QString();
}

// One undo step when the tool changed the document.
void MainWindow::runPluginTool(const std::shared_ptr<jubler::Tool> &tool) {
    if (!subs_) return;
    const QList<SubEntryPtr> selected = getSelectedSubs();
    auto snapshot = std::make_unique<UndoEntry>(*subs_, tool->menuText());
    PluginDocument doc(*subs_, getSelectedRows(), mediaPath());
    const QPointer<MainWindow> self(this);
    bool changed = false;
    try {
        changed = tool->run(doc, this);
    } catch (...) {
        Debug::debug(QStringLiteral("The plugin tool %1 failed").arg(tool->menuText()));
    }
    if (!self || !subs_) return;
    if (changed || doc.changed()) {
        undo_.addUndo(std::move(snapshot));
        tableHasChanged(selected);
    }
}

// ---- construction --------------------------------------------------------------------

void MainWindow::buildToolbar() {
    toolbar_ = addToolBar(QStringLiteral("Jubler"));
    toolbar_->setMovable(false);
    toolbar_->setFloatable(false);
    toolbar_->setIconSize(Theme::naturalSize(QStringLiteral("new")));   // the SVGs' own size (32), as the Java
    toolbar_->layout()->setSpacing(2);   // room between the tight buttons
#ifndef Q_OS_MACOS
    toolbar_->addWidget(menus_);
    toolbar_->addSeparator();
#endif
    auto tb = [&](const QString &icon, const QString &tip, std::function<void()> fn, bool enabled = true) {
        QAction *a = toolbar_->addAction(Theme::icon(icon), tip);
        a->setToolTip(tip);
        // Tight around the icon, as the Java's toolbar buttons (3 px margins).
        if (QWidget *b = toolbar_->widgetForAction(a)) b->setFixedSize(toolbar_->iconSize() + QSize(6, 6));
        a->setEnabled(enabled);
        connect(a, &QAction::triggered, this, [fn]() { fn(); });
        return a;
    };
    tb(QStringLiteral("new"), __("New"), [this]() { fileNew(); });
    tb(QStringLiteral("load"), __("Load"), [this]() { fileOpen(); });
    tbSave_ = tb(QStringLiteral("save"), __("Save"), [this]() { if (actSave_->isEnabled()) fileSave(); else fileSaveAs(); }, false);
    toolbar_->addSeparator();
    tbEncoding_ = tb(QStringLiteral("geardocument"), __("Encoding, frame rate and subtitle format"), [this]() { toggleEncodingBar(); }, false);
    tbEncoding_->setCheckable(true);
    tbInfo_ = tb(QStringLiteral("info"), __("Project Information"), [this]() { fileInformation(); }, false);
    tbQuality_ = tb(QStringLiteral("quality"), __("Quality configuration"), [this]() { fileQuality(); }, false);
    toolbar_->addSeparator();
    tbCut_ = tb(QStringLiteral("cut"), __("Cut"), [this]() { editCut(); }, false);
    tbCopy_ = tb(QStringLiteral("copy"), __("Copy"), [this]() { editCopy(); }, false);
    tbPaste_ = tb(QStringLiteral("paste"), __("Paste"), [this]() { editPaste(); }, false);
    toolbar_->addSeparator();
    tbUndo_ = tb(QStringLiteral("undo"), __("Undo"), [this]() { undoRedo(true); }, false);
    tbRedo_ = tb(QStringLiteral("redo"), __("Redo"), [this]() { undoRedo(false); }, false);
    toolbar_->addSeparator();
    tbSort_ = tb(QStringLiteral("sort"), __("Sort subtitles"), [this]() { sortAll(); }, false);
    toolbar_->addSeparator();
    tbPreview_ = tb(QStringLiteral("previewc"), __("Enable preview"), [this]() { enablePreview(tbPreview_->isChecked()); }, false);
    tbPreview_->setCheckable(true);
    // Right-click on the preview button: bring the selected row into view.
    if (auto *pb = qobject_cast<QToolButton *>(toolbar_->widgetForAction(tbPreview_))) {
        pb->setContextMenuPolicy(Qt::CustomContextMenu);
        pb->setToolTip(__("Enable preview") + QStringLiteral("\n") + __("Right mouse click to bring selected row into view"));
        connect(pb, &QToolButton::customContextMenuRequested, this, [this](const QPoint &) { bringSelectedRowIntoView(getSelectedRowIdx()); });
    }
    tbOrientation_ = tb(QStringLiteral("turndown"), __("Change orientation of Preview panel"), [this]() {
        const bool horizontal = AutoSaveOptions::getPreviewOrientation();
        setPreviewOrientation(!horizontal);
    }, false);
    // "New version!" at the right end of the same row, in a toolbar of its own
    // that never shrinks: when the row is short of room the main toolbar folds
    // its last buttons into its "›" menu instead of hiding the announcement.
    newsBar_ = addToolBar(QStringLiteral("JublerNews"));
    newsBar_->setMovable(false);
    newsBar_->setFloatable(false);
    newsBar_->setIconSize(toolbar_->iconSize());
    auto *spacer = new QWidget(newsBar_);
    spacer->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Preferred);
    newsBar_->addWidget(spacer);
    tbNewVersion_ = newsBar_->addAction(Theme::icon(QStringLiteral("newversion")), __("New version!"));
    tbNewVersion_->setToolTip(__("New version is available"));
    connect(tbNewVersion_, &QAction::triggered, this, [this]() { if (newVersionCb_) newVersionCb_(this); });
    if (auto *nb = qobject_cast<QToolButton *>(newsBar_->widgetForAction(tbNewVersion_)))
        nb->setToolButtonStyle(Qt::ToolButtonTextBesideIcon);
    newsBar_->hide();
}

void MainWindow::buildLayout() {
    basicPanel_ = new QWidget(this);
    auto *bl = new QVBoxLayout(basicPanel_);
    bl->setContentsMargins(0, 0, 0, 0);
    bl->setSpacing(0);
    encodingBar_ = new EncodingBar(basicPanel_);
    encodingBar_->hide();
    connect(encodingBar_, &EncodingBar::reloadRequested, this, &MainWindow::reloadFromBar);
    connect(encodingBar_, &EncodingBar::formatSelected, this, &MainWindow::applyFormatFromBar);
    connect(encodingBar_, &EncodingBar::mediaChanged, this, &MainWindow::mediaChanged);
    bl->addWidget(encodingBar_);

    table_ = new SubtitleTableView(basicPanel_);
    table_->setModel(model_);
    table_->setContextMenuPolicy(Qt::CustomContextMenu);
    connect(table_, &QWidget::customContextMenuRequested, this, [this](const QPoint &p) { popup_->exec(table_->viewport()->mapToGlobal(p)); });
    connect(table_->selectionModel(), &QItemSelectionModel::selectionChanged, this, [this]() {
        if (!table_->selectedRows().isEmpty())
            displaySubData(true);   // the user moved the selection
        dispatchPluginEvent([](jubler::EventListener &l, jubler::Document &d) { l.selectionChanged(d); return true; });
    });
    connect(table_, &SubtitleTableView::filesDropped, this, &MainWindow::openDroppedFiles);
    // Clicking the selected row again brings the preview (and the video) back to it.
    connect(table_, &SubtitleTableView::selectedRowClicked, this, [this](int) {
        if (preview_->isPreviewEnabled()) preview_->subsHaveChanged(getSelectedRows(), true);
    });

    preview_ = new SubPreview(this, basicPanel_);
    splitter_ = new QSplitter(AutoSaveOptions::getPreviewOrientation() ? Qt::Vertical : Qt::Horizontal, basicPanel_);
    splitter_->addWidget(preview_);
    splitter_->addWidget(table_);
    splitter_->setStretchFactor(0, 1);
    splitter_->setStretchFactor(1, 1);

    centerStack_ = new QStackedWidget(basicPanel_);
    centerStack_->addWidget(splitter_);
    bl->addWidget(centerStack_, 1);

    subEditP_ = new QWidget(basicPanel_);
    auto *sl = new QVBoxLayout(subEditP_);
    sl->setContentsMargins(0, 0, 0, 0);
    subeditor_ = new SubEditor(this, subEditP_);
    makeDraggable(toolbar_);
    makeDraggable(subeditor_->stylePanel());
    sl->addWidget(subeditor_);
    bl->addWidget(subEditP_);
    setCentralWidget(basicPanel_);
    setAcceptDrops(true);
}

QAction *MainWindow::addMenuAction(QMenu *menu, const QString &text, const QString &name, const QKeySequence &key, std::function<void()> fn, bool enabledAtStart) {
    QAction *a = menu->addAction(text);
    if (!key.isEmpty()) a->setShortcut(key);
    a->setShortcutContext(Qt::WindowShortcut);
    a->setEnabled(enabledAtStart);
    a->setObjectName(name);
    if (!name.isEmpty() && !name.startsWith(QLatin1String("ignore"), Qt::CaseInsensitive)) namedActions_.insert(name, a);
    connect(a, &QAction::triggered, this, [fn]() { fn(); });
    return a;
}

void MainWindow::buildMenus() {
    QMenu *file = menus_->addMenu(__("&File"));
    QMenu *newM = file->addMenu(__("New..."));
    addMenuAction(newM, __("File"), QStringLiteral("FNF"), QKeySequence(Qt::CTRL | Qt::Key_N), [this]() { fileNew(); }, true);
    actChild_ = addMenuAction(newM, __("Child"), QStringLiteral("FNC"), QKeySequence(), [this]() { fileNewChild(); });
    addMenuAction(newM, __("From video file"), QStringLiteral("FNV"), QKeySequence(), [this]() { fileNewFromVideo(); }, true);
    // Symmetric with New: a subtitle file or the subtitles inside a video.
    QMenu *openM = file->addMenu(__("Open..."));
    addMenuAction(openM, __("File"), QStringLiteral("FOP"), QKeySequence(Qt::CTRL | Qt::Key_O), [this]() { fileOpen(); }, true);
    addMenuAction(openM, __("From embedded subtitles"), QStringLiteral("FNE"), QKeySequence(), [this]() { fileNewFromEmbedded(); }, true);
    actRevert_ = addMenuAction(file, __("Revert"), QStringLiteral("FRE"), QKeySequence(), [this]() { fileRevert(); });
    recentMenu_ = file->addMenu(__("Recent files"));
    actSave_ = addMenuAction(file, __("Save"), QStringLiteral("FSV"), QKeySequence(Qt::CTRL | Qt::Key_S), [this]() { fileSave(); });
    actSaveAs_ = addMenuAction(file, __("Save as ..."), QStringLiteral("FSA"), QKeySequence(), [this]() { fileSaveAs(); });
    addMenuAction(file, __("Close"), QStringLiteral("FCL"), QKeySequence(Qt::CTRL | Qt::Key_W), [this]() { closeWindow(true, true); }, true);
    file->addSeparator();
    actInfo_ = addMenuAction(file, __("Information"), QStringLiteral("FIN"), QKeySequence(Qt::CTRL | Qt::Key_I), [this]() { fileInformation(); });
    actQuality_ = addMenuAction(file, __("Quality"), QStringLiteral("FQO"), QKeySequence(Qt::CTRL | Qt::Key_U), [this]() { fileQuality(); });
    // On macOS Qt moves these into the application menu (menu roles), so Cmd+Q
    // still goes through fileQuit and its unsaved check.
    addMenuAction(file, __("Preferences"), QStringLiteral("FPR"), QKeySequence(Qt::CTRL | Qt::Key_Comma), [this]() { filePreferences(); }, true)
        ->setMenuRole(QAction::PreferencesRole);
    addMenuAction(file, __("Quit"), QStringLiteral("FQU"), QKeySequence(Qt::CTRL | Qt::Key_Q), [this]() { fileQuit(); }, true)
        ->setMenuRole(QAction::QuitRole);

    QMenu *edit = menus_->addMenu(__("&Edit"));
    auto ed = [&](QMenu *m, const QString &text, const QString &name, const QKeySequence &key, std::function<void()> fn) {
        QAction *a = addMenuAction(m, text, name, key, fn);
        if (m == edit) editMenuActs_.append(a);
        return a;
    };
    ed(edit, __("Cut subtitles"), QStringLiteral("ECU"), QKeySequence(), [this]() { editCut(); });
    ed(edit, __("Copy subtitles"), QStringLiteral("ECO"), QKeySequence(), [this]() { editCopy(); });
    ed(edit, __("Paste subtitles"), QStringLiteral("EPA"), QKeySequence(), [this]() { editPaste(); });
    ed(edit, __("Paste special"), QStringLiteral("EPS"), QKeySequence(), [this]() { editPasteSpecial(); });
    edit->addSeparator();
    deleteMenu_ = edit->addMenu(__("Delete..."));
    editSubmenus_.append(deleteMenu_);
    ed(deleteMenu_, __("Empty Lines"), QStringLiteral("EDE"), QKeySequence(), [this]() { editDeleteEmptyLines(); });
    deleteMenu_->addSeparator();
    QMenu *replace = edit->addMenu(__("Replace..."));
    editSubmenus_.append(replace);
    ed(replace, __("Find & replace"), QStringLiteral("ERS"), QKeySequence(Qt::CTRL | Qt::Key_F), [this]() { editFindReplace(); });
    ed(replace, __("Regular Expression"), QStringLiteral("ERG"), QKeySequence(Qt::CTRL | Qt::Key_R), [this]() { editRegExp(); });
    QMenu *insert = edit->addMenu(__("Insert..."));
    editSubmenus_.append(insert);
    ed(insert, __("Before"), QStringLiteral("EIB"), QKeySequence(Qt::CTRL | Qt::Key_Backspace), [this]() { addNewSubtitle(false); });
    ed(insert, __("After"), QStringLiteral("EIA"), QKeySequence(Qt::CTRL | Qt::Key_Return), [this]() { addNewSubtitle(true); });
    QMenu *split = edit->addMenu(__("Split..."));
    editSubmenus_.append(split);
    ed(split, __("With previous subtitle"), QStringLiteral("ESP"), QKeySequence(Qt::CTRL | Qt::SHIFT | Qt::Key_Period), [this]() { splitWith(true); });
    ed(split, __("With next subtitle"), QStringLiteral("ESN"), QKeySequence(Qt::CTRL | Qt::Key_Period), [this]() { splitWith(false); });
    ed(split, __("In place proportionally"), QStringLiteral("ESI"), QKeySequence(), [this]() { splitInPlace(); });
    QMenu *gotoM = edit->addMenu(__("Go to..."));
    editSubmenus_.append(gotoM);
    ed(gotoM, __("Previous entry"), QStringLiteral("EGP"), SystemDependent::getUpDownKeystroke(true), [this]() { goTo('p'); });
    ed(gotoM, __("Next entry"), QStringLiteral("EGN"), SystemDependent::getUpDownKeystroke(false), [this]() { goTo('n'); });
    ed(gotoM, __("Previous page"), QStringLiteral("EGU"), QKeySequence(Qt::CTRL | Qt::Key_PageUp), [this]() { goTo('u'); });
    ed(gotoM, __("Next page"), QStringLiteral("EGD"), QKeySequence(Qt::CTRL | Qt::Key_PageDown), [this]() { goTo('d'); });
    ed(gotoM, __("First entry"), QStringLiteral("EGT"), QKeySequence(Qt::CTRL | Qt::Key_BracketLeft), [this]() { goTo('t'); });
    ed(gotoM, __("Last entry"), QStringLiteral("EGB"), QKeySequence(Qt::CTRL | Qt::Key_BracketRight), [this]() { goTo('b'); });
    gotoM->addSeparator();
    ed(gotoM, __("Selection by time"), QStringLiteral("EGM"), QKeySequence(), [this]() { goToTime(); });
    QMenu *select = edit->addMenu(__("Select..."));
    editSubmenus_.append(select);
    ed(select, __("All"), QStringLiteral("ESA"), QKeySequence(Qt::CTRL | Qt::SHIFT | Qt::Key_A), [this]() { selectAll(); });
    edit->addSeparator();
    markMenu_ = edit->addMenu(__("Mark..."));
    editSubmenus_.append(markMenu_);
    const char *markNames[4] = {"EMN", "EMP", "EMY", "EMC"};
    for (int i = 0; i < 4; ++i)
        ed(markMenu_, SubEntry::markName(i), QLatin1String(markNames[i]), QKeySequence(), [this, i]() { setMark(i); });
    markMenu_->addSeparator();
    columnsMenu_ = edit->addMenu(__("Show columns..."));
    editSubmenus_.append(columnsMenu_);
    const QString colNames[9] = {__("Index"), __("Start"), __("End"), __("Duration"), __("Layer"), __("Style"), __("Characters per minute"), __("Characters per second"), __("Subtitle")};
    const char *colActNames[9] = {"SCI", "SCS", "SCE", "SCD", "SCL", "SCY", "SCM", "SCP", "SCT"};
    for (int i = 0; i < 9; ++i) {
        QAction *a = ed(columnsMenu_, colNames[i], QLatin1String(colActNames[i]), QKeySequence(), [this, i]() { showTableColumn(i, columnActs_[i]->isChecked()); });
        a->setCheckable(true);
        a->setChecked(SubtitleTableModel::isVisibleColumn(i));
        columnActs_.append(a);
    }
    styleMenu_ = edit->addMenu(__("Style..."));
    editSubmenus_.append(styleMenu_);
    styleMenu_->addSeparator();
    edit->addSeparator();
    actToolsLock_ = ed(edit, __("Tools lock"), QStringLiteral("TLO"), QKeySequence(Qt::CTRL | Qt::Key_L), [this]() { subeditor_->setToolsLocked(actToolsLock_->isChecked()); });
    actToolsLock_->setCheckable(true);
    QMenu *focus = edit->addMenu(__("Focus..."));
    editSubmenus_.append(focus);
    ed(focus, __("Change focus from Text area to Time editor"), QStringLiteral("EFJ"), QKeySequence(Qt::CTRL | Qt::Key_D), [this]() { focusToggle(); });
    edit->addSeparator();
    actUndo_ = ed(edit, __("Undo"), QStringLiteral("EUN"), QKeySequence(Qt::CTRL | Qt::Key_Z), [this]() { undoRedo(true); });
    actRedo_ = ed(edit, __("Redo"), QStringLiteral("ERE"), QKeySequence(Qt::CTRL | Qt::Key_Y), [this]() { undoRedo(false); });

    toolsMenu_ = menus_->addMenu(__("&Tools"));
    previewMenu_ = new QMenu(__("Preview"), this);
    actEnablePreview_ = addMenuAction(previewMenu_, __("Enable preview"), QStringLiteral("TPE"), QKeySequence(Qt::Key_F7), [this]() { enablePreview(actEnablePreview_->isChecked()); });
    actEnablePreview_->setCheckable(true);
    previewMenu_->addSeparator();
    actMaxWave_ = addMenuAction(previewMenu_, __("Maximize waveform visualization"), QStringLiteral("TPM"), QKeySequence(), [this]() { preview_->setMaxWave(actMaxWave_->isChecked()); });
    actMaxWave_->setCheckable(true);
    actSnap_ = addMenuAction(previewMenu_, __("Snap to subtitle"), QStringLiteral("TPS"), QKeySequence(), [this]() { preview_->setSnapToSubtitle(actSnap_->isChecked()); });
    actSnap_->setCheckable(true);
    actSnap_->setChecked(true);
    actPlayAudio_ = addMenuAction(previewMenu_, __("Play current subtitle"), QStringLiteral("TPP"), QKeySequence(), [this]() { preview_->playbackWave(); });
    externalsMenu_ = new QMenu(__("Externals"), this);
    if (SystemDependent::isFlatpak())
        externalsMenu_->menuAction()->setVisible(false);
    // ToolRunner::registerMenus inserts the tools before these two.
    toolsMenu_->addMenu(previewMenu_);
    toolsMenu_->addMenu(externalsMenu_);

    QMenu *help = menus_->addMenu(__("&Help"));
    addMenuAction(help, __("Donation"), IGNORED, QKeySequence(), [this]() { helpUrl(QStringLiteral("https://www.jubler.org/donations.html")); }, true);
    addMenuAction(help, __("Issues"), IGNORED, QKeySequence(), [this]() { helpUrl(QStringLiteral("https://github.com/teras/Jubler/issues")); }, true);
    addMenuAction(help, __("FAQ"), IGNORED, QKeySequence(), [this]() { helpUrl(QStringLiteral("https://jubler.org/faq.html")); }, true);
    addMenuAction(help, __("About"), QStringLiteral("HAB"), QKeySequence(Qt::CTRL | Qt::Key_Slash), []() { AppContext::showAbout(); }, true)
        ->setMenuRole(QAction::AboutRole);
    // Non-Latin menu titles ("&ΑAρχείο"): Alt+<Latin helper> opens them as well.
    const std::pair<const char *, QMenu *> tops[] = {{"&File", file}, {"&Edit", edit}, {"&Tools", toolsMenu_}, {"&Help", help}};
    for (const auto &[source, menu] : tops) {
        const QChar latin = latinMnemonic(source);
        if (latin.isNull()) continue;
        auto *sc = new QShortcut(QKeySequence(Qt::ALT | Qt::Key(latin.unicode())), this);
        const auto open = [this, menu = menu]() { menus_->setActiveAction(menu->menuAction()); };
        connect(sc, &QShortcut::activated, this, open);
        // With a Greek layout the same key also matches the title's own mnemonic.
        connect(sc, &QShortcut::activatedAmbiguously, this, open);
    }
    for (QMenu *m : editSubmenus_) m->setEnabled(false);
    previewMenu_->setEnabled(false);
    externalsMenu_->setEnabled(false);
    rebuildRecentMenu();
}

void MainWindow::buildPopup() {
    popup_ = new QMenu(this);
    popup_->addAction(__("Cut"), this, [this]() { editCut(); });
    popup_->addAction(__("Copy"), this, [this]() { editCopy(); });
    popup_->addAction(__("Paste"), this, [this]() { editPaste(); });
    popup_->addAction(__("Delete"), this, [this]() { editDeleteSelected(); });
    markPopup_ = popup_->addMenu(__("Mark"));
    for (int i = 0; i < 4; ++i)
        markPopup_->addAction(SubEntry::markName(i), this, [this, i]() { setMark(i); });
    stylePopup_ = popup_->addMenu(__("Style"));
    popup_->addSeparator();
    columnsPopup_ = popup_->addMenu(__("Show columns"));
    for (int i = 0; i < 9; ++i) {
        QAction *a = columnsPopup_->addAction(columnActs_[i]->text());
        a->setCheckable(true);
        a->setChecked(SubtitleTableModel::isVisibleColumn(i));
        connect(a, &QAction::triggered, this, [this, i, a]() { showTableColumn(i, a->isChecked()); });
        columnPopupActs_.append(a);
    }
}

void MainWindow::applyShortcuts() {
    // Every named action of the menu bar, tools included.
    QMap<QString, QAction *> named = namedActions_;
    std::function<void(QMenu *)> walk = [&](QMenu *m) {
        for (QAction *a : m->actions()) {
            if (a->menu()) walk(a->menu());
            else if (!a->objectName().isEmpty() && !a->objectName().startsWith(QLatin1String("ignore"), Qt::CaseInsensitive))
                named.insert(a->objectName(), a);
        }
    };
    for (QAction *top : menus_->actions())
        if (top->menu()) walk(top->menu());
    PreferencesDialog::applyMenuShortcuts(named);
}

void MainWindow::setCentral(QWidget *w) {
    if (centerStack_->indexOf(w) < 0)
        centerStack_->addWidget(w);
    centerStack_->setCurrentWidget(w);
}

// ---- accessors -------------------------------------------------------------------------

QList<int> MainWindow::getSelectedRows() const { return table_->selectedRows(); }

QList<SubEntryPtr> MainWindow::getSelectedSubs() const {
    QList<SubEntryPtr> out;
    if (!subs_) return out;
    for (int r : table_->selectedRows())
        if (r >= 0 && r < subs_->size()) out.append(subs_->get(r));
    return out;
}

SubEntryPtr MainWindow::getSelectedRow() const {
    const int r = getSelectedRowIdx();
    return subs_ && r >= 0 && r < subs_->size() ? subs_->get(r) : nullptr;
}

int MainWindow::getSelectedRowIdx() const { return table_->selectedRow(); }

bool MainWindow::isEmptyCanvas() const {
    return (!mfile_ || !mfile_->getVideoFile()) && (!subs_ || subs_->isEmpty());
}

bool MainWindow::isEmptyContent() const {
    return !subs_ || subs_->isEmpty() || (subs_->size() == 1 && subs_->get(0)->getText().trimmed().isEmpty());
}

bool MainWindow::isToolLocked() const { return subeditor_->isToolsLocked(); }

QString MainWindow::documentName() const {
    return subs_ ? QFileInfo(subs_->getSubFile().getStrippedFile()).fileName() : QString();
}

// ---- undo host -------------------------------------------------------------------------

void MainWindow::setDoText(const QString &text, bool isUndo) {
    QAction *menu = isUndo ? actUndo_ : actRedo_;
    QAction *tool = isUndo ? tbUndo_ : tbRedo_;
    const QString base = isUndo ? __("Undo") : __("Redo");
    if (text.isNull()) {
        menu->setEnabled(false);
        tool->setEnabled(false);
        menu->setText(base);
    } else {
        menu->setEnabled(true);
        tool->setEnabled(true);
        menu->setText(base + QStringLiteral(" \"") + text + QLatin1Char('"'));
    }
}

void MainWindow::setUnsaved(bool unsaved) { unsaved_ = unsaved; }

std::unique_ptr<Subtitles> MainWindow::swapSubtitles(std::unique_ptr<Subtitles> newsubs) {
    std::unique_ptr<Subtitles> old = std::move(subs_);
    subs_ = std::move(newsubs);
    if (subs_) {
        subs_->updateQuality();
        model_->setSubtitles(subs_.get());
        table_->applyColumnWidths();
        showInfo();
        updateStyleMenu();
        if (preview_->isPreviewEnabled()) preview_->refreshSubtitles();
    }
    return old;
}

void MainWindow::keepUndo(const SubEntryPtr &entry) {
    if (!subs_ || entry == lastChangedSub_) return;
    undo_.addUndo(*subs_, __("Change subtitle"));
    lastChangedSub_ = entry;   // addUndo reset it; remember after
}

void MainWindow::subTextChanged() {
    if (subeditor_->shouldIgnoreSubChanges() || !subs_) return;
    // The subtitle the editor shows (the Java used the selected row, which is
    // none after a Ctrl+click deselect: the typed text was lost — JAVA_BUGS #41).
    const SubEntryPtr entry = subeditor_->entry();
    const int row = entry ? subs_->indexOf(entry) : -1;
    if (row < 0) return;
    keepUndo(entry);
    entry->setText(subeditor_->getSubText());
    subeditor_->updateMetrics(*entry);
    rowHasChanged(row, false);
}

void MainWindow::setToolsLockMenu(bool locked) {
    actToolsLock_->setChecked(locked);
}

void MainWindow::editorAttached(bool attached) {
    if (attached) {
        subEditP_->layout()->addWidget(subeditor_);
        subeditor_->setParent(subEditP_);
        subeditor_->show();
        subEditP_->show();
    } else
        subEditP_->hide();
}

void MainWindow::previewRepaint() {
    if (preview_->isPreviewEnabled()) preview_->refreshSubtitles();
}

// ---- document --------------------------------------------------------------------------

void MainWindow::setSubs(std::unique_ptr<Subtitles> newsubs) {
    const QList<SubEntryPtr> selected = getSelectedSubs();
    swapSubtitles(std::move(newsubs));
    tableHasChanged(selected);
    for (int i = 0; i < 9; ++i) {
        columnActs_[i]->setChecked(SubtitleTableModel::isVisibleColumn(i));
        columnPopupActs_[i]->setChecked(SubtitleTableModel::isVisibleColumn(i));
    }
}

void MainWindow::tableHasChanged(const QList<SubEntryPtr> &oldSelections) {
    if (!subs_) return;
    QList<int> indices;
    if (oldSelections.isEmpty()) {
        if (!subs_->isEmpty()) {
            int cur = getSelectedRowIdx();
            if (cur < 0) cur = 0;
            indices.append(std::min(std::max(cur, 0), subs_->size() - 1));
        }
    } else {
        for (const SubEntryPtr &e : oldSelections)
            indices.append(subs_->indexOf(e));
    }
    showInfo();
    model_->structureChanged();
    table_->applyColumnWidths();
    updateStyleMenu();
    setSelectedSub(indices, true);
    if (preview_->isPreviewEnabled()) preview_->refreshSubtitles();
}

void MainWindow::rowHasChanged(int row, bool updateDisplay) {
    if (row < 0) return;
    model_->rowsChanged(row, row);
    if (updateDisplay) displaySubData();
    if (preview_->isPreviewEnabled()) preview_->refreshSubtitles();
}

void MainWindow::showInfo() {
    subeditor_->setTotal(subs_ ? subs_->size() : 0);
    subeditor_->setUnsaved(isUnsaved());
    if (!subs_ || subs_->getSubFile().getSaveFile().isEmpty()) {
        setWindowTitle(QStringLiteral("Jubler"));
        setWindowModified(false);
        return;
    }
    const SubFormatPtr fmt = subs_->getSubFile().getFormat();
    const QString formatName = fmt ? fmt->getName() : __("Unknown format");
    // "[*]" becomes the Java's leading "*" while the document is unsaved.
    setWindowTitle(QStringLiteral("[*]") + formatName + QStringLiteral(" - ") + SystemDependent::displayPath(subs_->getSubFile().getSaveFile()) +
                   QStringLiteral(" - Jubler"));
    setWindowModified(isUnsaved());
    setWindowFilePath(subs_->getSubFile().getSaveFile());
}

int MainWindow::setSelectedSub(int which, bool updateVisuals) {
    return setSelectedSub(QList<int>{which}, updateVisuals);
}

int MainWindow::setSelectedSub(const QList<int> &which, bool updateVisuals) {
    ignoreTableSelections_ = true;
    table_->selectionModel()->clearSelection();
    int ret = -1;
    if (!which.isEmpty() && subs_ && !subs_->isEmpty()) {
        ret = which.first();
        table_->selectRows(which);
        ret = table_->selectedRow();
    }
    ignoreTableSelections_ = false;
    if (updateVisuals) displaySubData();
    return ret;
}

void MainWindow::bringSelectedRowIntoView(int row) { table_->bringRowIntoView(row); }

void MainWindow::followPlaybackSelection(int idx) {
    if (!preview_->isPreviewEnabled()) return;
    playbackDrivenSelection_ = true;
    if (idx < 0) {
        ignoreTableSelections_ = true;
        table_->selectionModel()->clearSelection();
        ignoreTableSelections_ = false;
    } else
        setSelectedSub(idx, true);
    playbackDrivenSelection_ = false;
}

void MainWindow::displaySubData(bool fromUserSelection) {
    if (ignoreTableSelections_ || !subs_) return;
    const int row = getSelectedRowIdx();
    if (row < 0 || row >= subs_->size()) return;
    const SubEntryPtr entry = subs_->get(row);
    subeditor_->ignoreSubChanges(true);
    subeditor_->setData(entry);
    if (preview_->isPreviewEnabled())
        preview_->subsHaveChanged(getSelectedRows(), fromUserSelection);
    if (jparent_ && jparent_->getSubtitles()) {
        const double mid = (entry->getStartTime().toSeconds() + entry->getFinishTime().toSeconds()) / 2;
        const int pidx = jparent_->getSubtitles()->findSubEntry(mid, true);
        if (pidx >= 0) jparent_->setSelectedSub(pidx, true);
    }
    if (!playbackDrivenSelection_) subeditor_->focusOnText();
    subeditor_->updateMetrics(*entry);
    subeditor_->ignoreSubChanges(false);
}

int MainWindow::addSubEntry(const SubEntryPtr &entry) {
    const QList<SubEntryPtr> sel = getSelectedSubs();
    undo_.addUndo(*subs_, __("Insert subtitle"));
    const int where = subs_->addSorted(entry);
    tableHasChanged(sel);
    return where;
}

void MainWindow::updateStyleMenu() {
    auto build = [this](QMenu *menu, bool shortcuts) {
        // The style items sit before the separator; the tools after it stay.
        QAction *sep = nullptr;
        for (QAction *a : menu->actions()) {
            if (a->isSeparator()) { sep = a; break; }
            menu->removeAction(a);
            a->deleteLater();
        }
        if (!subs_ || subs_->getStyleList().size() < 2) {
            menu->setEnabled(false);
            return;
        }
        menu->setEnabled(true);
        int i = 0;
        for (const SubStylePtr &s : subs_->getStyleList().all()) {
            auto *a = new QAction(s->getName(), menu);
            if (shortcuts && i <= 9) a->setShortcut(QKeySequence(Qt::CTRL | Qt::ALT | (Qt::Key_0 + i)));
            const QString name = s->getName();
            connect(a, &QAction::triggered, this, [this, name]() { changeSubtitleStyle(name); });
            if (sep) menu->insertAction(sep, a); else menu->addAction(a);
            ++i;
        }
    };
    build(stylePopup_, false);
    build(styleMenu_, true);
}

void MainWindow::changeSubtitleStyle(const QString &name) {
    if (!subs_) return;
    const QList<SubEntryPtr> sel = getSelectedSubs();
    undo_.addUndo(*subs_, __("Change style into {0}", name));
    const SubStylePtr style = subs_->getStyleList().getStyleByName(name);
    for (const SubEntryPtr &e : sel) e->setStyle(style);
    tableHasChanged(sel);
}

void MainWindow::enableWindowControls(bool asNewWindow) {
    actRevert_->setEnabled(true);
    actChild_->setEnabled(true);
    actSave_->setEnabled(true);
    actSaveAs_->setEnabled(true);
    actInfo_->setEnabled(true);
    actQuality_->setEnabled(true);
    for (QAction *a : editMenuActs_) a->setEnabled(true);
    // The sub-menu items (including the Delete/Mark/Style tools) as well:
    // they only need a document. (The Java never enabled the three tool
    // items — JAVA_BUGS #16.)
    for (QMenu *m : editSubmenus_) {
        m->setEnabled(true);
        for (QAction *a : m->actions()) a->setEnabled(true);
    }
    for (QAction *a : columnActs_) a->setEnabled(true);
    actToolsLock_->setEnabled(true);
    toolsMenu_->setEnabled(true);
    previewMenu_->setEnabled(true);
    externalsMenu_->setEnabled(true);
    actEnablePreview_->setEnabled(true);
    updateToolsAvailability();
    updateStyleMenu();
    if (!AppContext::windows().isEmpty()) AppContext::windows().first()->updateToolsAvailability();
    if (asNewWindow) {
        setDoText(QString(), true);
        setDoText(QString(), false);
    }
    for (QAction *a : {tbSave_, tbEncoding_, tbInfo_, tbQuality_, tbCut_, tbCopy_, tbPaste_, tbSort_, tbPreview_}) a->setEnabled(true);
    if (asNewWindow) setSelectedSub(0, true);
    subeditor_->removeHelpWanted();
    stopCelebration();
}

void MainWindow::enableSaveControls() {
    undo_.invalidateSaveMark();
    enableWindowControls(true);
    actSave_->setEnabled(false);
    actRevert_->setEnabled(false);
    subeditor_->focusOnText();
}

void MainWindow::updateToolsAvailability() {
    ToolRunner::updateToolsAvailability(this, toolsMenu_);
}

void MainWindow::mediaChanged() {
    updateToolsAvailability();
    // The encoding bar's "FPS from video" and the preview hold the media
    // pointer: re-point them at the current object.
    if (subs_ && encodingBar_->isVisible())
        encodingBar_->showFor(subs_->getSubFile().getEncoding(), mfile_.get(), subs_.get());
    if (preview_->isPreviewEnabled())
        preview_->updateMediaFile(mfile_.get());
}

void MainWindow::rebuildExternalsMenu() {
    if (!SystemDependent::isFlatpak()) RecipeUi::buildExternalsMenu(this, externalsMenu_);
}

// ---- loading / saving ------------------------------------------------------------------

MainWindow *MainWindow::loadFileFromHere(const SubFile &sfile, bool forceIntoSameWindow) {
    AppContext::setWindowPosition(this, false);
    return loadFile(sfile, forceIntoSameWindow);
}

MainWindow *MainWindow::loadFile(const SubFile &sfileIn, bool forceIntoSameWindow) {
    SubFile sfile = sfileIn;
    const QString path = sfile.getSaveFile();
    const bool isAutoload = QFileInfo(path).fileName().startsWith(AutoSaver::AUTOSAVEPREFIX);
    const QByteArray raw = FileCommunicator::loadRawBytes(path);
    const QString data = FileCommunicator::detectAndDecode(sfile, raw, true);
    // A recovered autosave that cannot be reopened is kept aside, never
    // deleted by the next autosave pass.
    const auto unrecovered = [&]() {
        return isAutoload ? QStringLiteral("\n\n") + __("The autosaved file was kept in {0}", QDir::toNativeSeparators(AutoSaver::keepUnrecovered(path)))
                          : QString();
    };
    if (data.isNull()) {
        QMessageBox::critical(this, __("Error while loading file"), __("Could not load file. Possibly an encoding error.") + unrecovered());
        return nullptr;
    }
    SubFormatPtr originalFormat;
    if (isAutoload) {
        // "<prefix>XXXX.<original name>.ass" → the original name in the default
        // dir, saved again in its own format (an older "<name>.ass" stays ASS).
        QString name = QFileInfo(path).fileName().mid(AutoSaver::AUTOSAVEPREFIX.length() + 5);
        const QString inner = QFileInfo(name).completeBaseName();
        if (name.endsWith(QLatin1String(".ass"), Qt::CaseInsensitive) && !QFileInfo(inner).suffix().isEmpty()) {
            if (SubFormatPtr f = Availabilities::formats().findFromExtension(QFileInfo(inner).suffix())) {
                originalFormat = f;
                name = inner;
            }
        }
        sfile.setFile(QFileInfo(SubFile().getSaveFile()).path() + QLatin1Char('/') + name);
    }
    auto newsubs = std::make_unique<Subtitles>(sfile);
    newsubs->setLoadedBytes(raw);
    newsubs->populate(sfile, data, true);
    if (originalFormat && !newsubs->isEmpty()) newsubs->getSubFile().setFormat(originalFormat->newInstance());
    if (newsubs->isEmpty()) {
        QMessageBox::critical(this, __("Error while loading file"), __("File not recognized!") + unrecovered());
        return nullptr;
    }
    // Only now (after a successful parse) decide the target window.
    MainWindow *work = (!subs_ || forceIntoSameWindow) ? this : new MainWindow();
    if (work->subs_)
        work->undo_.addUndo(*work->subs_, __("Reload subtitles"));
    if (isAutoload) work->undo_.invalidateSaveMark(); else work->undo_.setSaveMark();
    const QString encoding = sfile.getEncoding();
    work->setSubs(std::move(newsubs));
    work->encodingBar_->showFor(encoding, work->mfile_.get(), work->subs_.get());
    work->encodingBar_->show();
    work->tbEncoding_->setChecked(true);
    work->enableWindowControls(true);
    work->showInfo();
    work->actSave_->setEnabled(true);
    work->show();
    AppContext::updateRecents();
    if (!isAutoload)
        work->dispatchPluginEvent([](jubler::EventListener &l, jubler::Document &d) { l.documentOpened(d); return true; });
    return work;
}

void MainWindow::saveFile(const SubFile &sfile) {
    if (!subs_) return;
    const QPointer<MainWindow> self(this);
    // A plugin may veto the save (or fix the document first).
    if (!dispatchPluginEvent([this](jubler::EventListener &l, jubler::Document &d) { return l.beforeSave(d, this); }) || !self || !subs_) return;
    const QString err = FileCommunicator::save(*subs_, sfile, mfile_.get());
    if (err.isNull()) {
        enableWindowControls(false);
        undo_.setSaveMark();
        subs_->setSubFile(sfile);
        showInfo();
        AppContext::updateRecents();
        dispatchPluginEvent([](jubler::EventListener &l, jubler::Document &d) { l.afterSave(d); return true; });
    } else
        QMessageBox::critical(this, __("Error while saving file"), err);
}

void MainWindow::fileNew() {
    MainWindow *work = isEmptyCanvas() ? this : new MainWindow();
    work->show();
    work->setUnsaved(true);
    auto s = std::make_unique<Subtitles>();
    s->add(std::make_shared<SubEntry>(Time(0.0), Time(5.0), QString(QLatin1String(""))));
    s->setLoadedBytes(QByteArray(""));  // "armed": the bar shows now and auto-hides on the first edit
    const QString enc = s->getSubFile().getEncoding();
    work->setSubs(std::move(s));
    work->enableSaveControls();
    work->encodingBar_->showFor(enc, work->mfile_.get(), work->subs_.get());
    work->encodingBar_->show();
    work->tbEncoding_->setChecked(true);
    AppContext::updateRecents();
}

void MainWindow::fileNewChild() {
    if (!subs_) return;
    auto *w = new MainWindow();
    w->show();
    auto s = std::make_unique<Subtitles>(*subs_);
    for (const SubEntryPtr &e : s->entries()) e->setText(QString(QLatin1String("")));
    s->setLoadedBytes(QByteArray(""));
    s->getSubFile().appendToFilename(__("_child"));
    const QString enc = s->getSubFile().getEncoding();
    w->setSubs(std::move(s));
    w->setUnsaved(true);
    w->showInfo();
    w->jparent_ = this;
    w->enableSaveControls();
    w->encodingBar_->showFor(enc, w->mfile_.get(), w->subs_.get());
    w->encodingBar_->show();
    w->tbEncoding_->setChecked(true);
    AppContext::updateRecents();
}

void MainWindow::fileNewFromVideo() {
    const QString video = QFileDialog::getOpenFileName(this, __("New from video file"), FileCommunicator::getDefaultDirPath(), AppMediaFile::videoFilterString());
    if (video.isEmpty() || !QFileInfo::exists(video)) return;
    FileCommunicator::setDefaultDir(QFileInfo(video).path());
    newFromVideo(video);
}

// A text subtitle stream of a media file (mkv, mp4, …) as a new document,
// with that media attached. (Not in the Java Jubler: libav makes it native.)
void MainWindow::fileNewFromEmbedded() {
    const VideoFile *current = mfile_ ? mfile_->getVideoFile() : nullptr;
    const QString start = current && current->exists() ? current->getPath() : FileCommunicator::getDefaultDirPath();
    const QString video = QFileDialog::getOpenFileName(this, __("Import embedded subtitles"), start, AppMediaFile::videoFilterString());
    if (video.isEmpty() || !QFileInfo::exists(video)) return;
    FileCommunicator::setDefaultDir(QFileInfo(video).path());
    openEmbedded(video, -1, QString());
}

void MainWindow::openEmbedded(const QString &video, int stream, const QString &language) {
    const QString title = __("Import embedded subtitles");
    if (video.isEmpty() || !QFileInfo::exists(video)) return;

    // Off the GUI thread (a slow or stale mount must not freeze the window).
    const QPointer<MainWindow> self(this);
    QApplication::setOverrideCursor(Qt::WaitCursor);
    QFutureWatcher<QList<SubtitleStreamInfo>> probe;
    {
        QEventLoop wait;
        connect(&probe, &QFutureWatcher<QList<SubtitleStreamInfo>>::finished, &wait, &QEventLoop::quit);
        probe.setFuture(QtConcurrent::run([video]() { return MediaProbe::subtitleStreams(video); }));
        wait.exec(QEventLoop::ExcludeUserInputEvents);
    }
    QApplication::restoreOverrideCursor();
    if (!self) return;
    const QList<SubtitleStreamInfo> all = probe.result();
    QList<SubtitleStreamInfo> streams;
    for (const SubtitleStreamInfo &s : all)
        if (s.extractable) streams.append(s);
    if (streams.isEmpty()) {
        QMessageBox::information(this, title, all.isEmpty() ? __("This file has no subtitle streams.")
                                                            : __("This file has only image-based subtitles, which cannot be imported as text."));
        return;
    }
    // A recent entry names its stream; if the file changed, the same language
    // is taken, and only if that is gone too is the user asked again.
    int remembered = -1;
    for (int i = 0; i < streams.size(); ++i)
        if (streams.at(i).id == stream && streams.at(i).language == language) remembered = i;
    if (remembered < 0 && !language.isEmpty())
        for (int i = streams.size() - 1; i >= 0; --i)
            if (streams.at(i).language == language) remembered = i;

    SubtitleStreamInfo chosen = streams.first();
    if (remembered >= 0) {
        chosen = streams.at(remembered);
    } else if (streams.size() > 1) {
        QStringList items;
        for (const SubtitleStreamInfo &s : streams) {
            QString item = QStringLiteral("#%1").arg(s.index + 1);
            if (!s.language.isEmpty()) item += QStringLiteral(" — ") + s.language;
            if (!s.title.isEmpty()) item += QStringLiteral(" — ") + s.title;
            items.append(item + QStringLiteral(" (") + s.codecDescription + QLatin1Char(')'));
        }
        bool ok = false;
        const QString item = QInputDialog::getItem(this, title, __("Subtitle stream:"), items, 0, false, &ok);
        if (!ok) return;
        chosen = streams.at(std::max(0, int(items.indexOf(item))));
    }

    // The whole file is read (a subtitle stream is spread over it): progress + cancel.
    // The dialog belongs to the window (which may be closed from another one
    // while this waits): on the heap, so its destruction is the window's to do,
    // and watched through a QPointer. The reading thread touches nothing of it
    // — it only writes a percentage that a timer here reads.
    QPointer<QProgressDialog> progress = new QProgressDialog(__("Reading subtitles…"), __("Cancel"), 0, 100, this);
    progress->setWindowTitle(title);
    progress->setWindowModality(Qt::WindowModal);
    progress->setMinimumDuration(400);
    std::atomic<bool> cancel{false};
    auto percent = std::make_shared<std::atomic<int>>(0);
    QFutureWatcher<EmbeddedSubtitles::Result> watcher;
    QEventLoop loop;
    QTimer tick;
    connect(&watcher, &QFutureWatcher<EmbeddedSubtitles::Result>::finished, &loop, &QEventLoop::quit);
    connect(progress, &QProgressDialog::canceled, &loop, [&cancel]() { cancel = true; });
    connect(&tick, &QTimer::timeout, &tick, [progress, percent]() {
        if (progress) progress->setValue(percent->load());
    });
    tick.start(100);
    watcher.setFuture(QtConcurrent::run([video, id = chosen.id, &cancel, percent]() {
        return EmbeddedSubtitles::extract(video, id, cancel, [percent](float p) { percent->store(int(p * 100)); });
    }));
    loop.exec();
    tick.stop();
    const EmbeddedSubtitles::Result res = watcher.result();
    if (!self) return;   // the window was closed meanwhile (the dialog went with it)
    progress->reset();
    progress->deleteLater();
    if (res.text.isNull()) {
        if (!cancel) QMessageBox::critical(this, title, res.error);
        return;
    }

    // Named after the media and the language, never over an existing file.
    const QFileInfo vi(video);
    QString base = vi.path() + QLatin1Char('/') + vi.completeBaseName();
    if (!chosen.language.isEmpty()) base += QLatin1Char('.') + chosen.language;
    QString name = base + QLatin1Char('.') + res.extension;
    for (int n = 2; QFileInfo::exists(name); ++n) name = base + QStringLiteral(".%1.").arg(n) + res.extension;
    SubFile sfile(name, SubFile::EXTENSION_GIVEN);
    sfile.setEncoding(QStringLiteral("UTF-8"));
    auto s = std::make_unique<Subtitles>(sfile);
    s->populate(sfile, res.text, false);
    if (s->isEmpty()) {
        QMessageBox::critical(this, title, __("File not recognized!"));
        return;
    }
    s->setLoadedBytes(QByteArray(""));
    const QString enc = s->getSubFile().getEncoding();
    MainWindow *work = isEmptyCanvas() ? this : new MainWindow();
    work->show();
    work->setUnsaved(true);
    work->setSubs(std::move(s));
    work->mfile_->setNewVideoFile(video);
    work->mediaChanged();
    work->enableSaveControls();
    work->showInfo();
    work->encodingBar_->showFor(enc, work->mfile_.get(), work->subs_.get());
    work->encodingBar_->show();
    work->tbEncoding_->setChecked(true);
    // The recent entry is the media and the stream: the document's own file
    // does not exist yet (it is named after the media and the language).
    SubFile recent(video, SubFile::EXTENSION_GIVEN);
    recent.setEmbedded(chosen.id, chosen.language);
    AppContext::addRecentFile(recent);
}

void MainWindow::newFromVideo(const QString &video) {
    if (video.isEmpty() || !QFileInfo::exists(video)) return;
    MainWindow *work = isEmptyCanvas() ? this : new MainWindow();
    work->show();
    work->setUnsaved(true);
    auto s = std::make_unique<Subtitles>();
    s->add(std::make_shared<SubEntry>(Time(0.0), Time(5.0), QString(QLatin1String(""))));
    QString base = video;
    const int dot = QFileInfo(video).fileName().lastIndexOf(QLatin1Char('.'));
    if (dot > 0) base = QFileInfo(video).path() + QLatin1Char('/') + QFileInfo(video).fileName().left(dot);
    s->setSubFile(SubFile(base, SubFile::EXTENSION_OMMITED));
    s->setLoadedBytes(QByteArray(""));
    const QString enc = s->getSubFile().getEncoding();
    work->setSubs(std::move(s));
    work->mfile_->setNewVideoFile(video);
    work->mediaChanged();
    work->enableSaveControls();
    work->showInfo();
    work->encodingBar_->showFor(enc, work->mfile_.get(), work->subs_.get());
    work->encodingBar_->show();
    work->tbEncoding_->setChecked(true);
    AppContext::addRecentFile(video);
}

void MainWindow::recentMenuCallback(const SubFile *sfile) {
    if (!sfile) {
        if (!subs_) return;
        auto copy = std::make_unique<Subtitles>(*subs_);
        copy->getSubFile().appendToFilename(QStringLiteral("_clone"));
        auto *w = new MainWindow(std::move(copy));
        w->enableSaveControls();
        w->showInfo();
        AppContext::updateRecents();
        return;
    }
    if (sfile->getEmbeddedStream() >= 0) {
        openEmbedded(sfile->getSaveFile(), sfile->getEmbeddedStream(), sfile->getEmbeddedLanguage());
        return;
    }
    if (AppMediaFile::isVideoFile(sfile->getSaveFile())) {
        newFromVideo(sfile->getSaveFile());
        return;
    }
    loadFileFromHere(*sfile, false);
}

void MainWindow::fileOpen() {
    auto mf = std::make_unique<AppMediaFile>();
    const std::optional<SubFile> result = SubFileDialog::getLoadFile(this, mf.get());
    if (!result) return;
    MainWindow *w = loadFileFromHere(*result, false);
    if (w) {
        w->mfile_ = std::move(mf);
        w->mediaChanged();
    }
}

void MainWindow::fileRevert() {
    if (subs_) loadFileFromHere(subs_->getSubFile(), true);
}

void MainWindow::fileSave() {
    if (!subs_) return;
    if (SystemDependent::isFlatpak() && !SystemDependent::isPortalGranted(subs_->getSubFile().getSaveFile())) {
        fileSaveAs();
        return;
    }
    saveFile(SubFile(subs_->getSubFile()));
}

void MainWindow::fileSaveAs() {
    if (!subs_) return;
    const std::optional<SubFile> result = SubFileDialog::getSaveFile(this, subs_.get(), mfile_.get());
    if (result) saveFile(*result);
}

void MainWindow::fileInformation() {
    if (!subs_) return;
    const SubAttribs oldattr = subs_->getAttribs();
    Subtitles snapshot(*subs_);
    InformationDialog info(this);
    info.exec();
    if (!info.isAccepted()) return;
    subs_->setAttribs(info.getAttribs());
    subs_->updateQuality();
    tableHasChanged(getSelectedSubs());
    if (subs_->getAttribs() != oldattr)
        undo_.addUndo(snapshot, __("Change information"));
}

void MainWindow::fileQuality() {
    QualityDialog dlg(this);
    dlg.exec();
    if (subs_) tableHasChanged(getSelectedSubs());
}

void MainWindow::filePreferences() {
    PreferencesDialog::showPreferencesDialog(this);
}

void MainWindow::fileQuit() {
    if (AppContext::requestQuit(this))
        QApplication::exit(0);
}

// ---- edit ------------------------------------------------------------------------------

void MainWindow::editCopy() {
    QList<SubEntryPtr> &buffer = AppContext::copyBuffer();
    buffer.clear();
    for (const SubEntryPtr &e : getSelectedSubs())
        buffer.append(std::make_shared<SubEntry>(*e));   // table order
}

void MainWindow::editCut() {
    if (!subs_) return;
    QList<SubEntryPtr> &buffer = AppContext::copyBuffer();
    buffer.clear();
    undo_.addUndo(*subs_, __("Cut subtitles"));
    for (const SubEntryPtr &e : getSelectedSubs()) {
        buffer.append(std::make_shared<SubEntry>(*e));
        subs_->remove(e);
    }
    tableHasChanged({});
}

void MainWindow::editPaste() {
    if (!subs_ || AppContext::copyBuffer().isEmpty()) return;
    undo_.addUndo(*subs_, __("Paste subtitles"));
    QList<SubEntryPtr> pasted;
    for (const SubEntryPtr &e : AppContext::copyBuffer()) {
        auto copy = std::make_shared<SubEntry>(*e);
        subs_->addSorted(copy);
        pasted.append(copy);
    }
    subs_->revalidateStyles();
    tableHasChanged(pasted);
}

void MainWindow::editPasteSpecial() {
    if (!subs_ || AppContext::copyBuffer().isEmpty()) return;
    const SubEntryPtr sel = getSelectedRow();
    PasterDialog paster(sel ? sel->getStartTime() : Time(0.0), this);
    if (paster.exec() != QDialog::Accepted) return;
    const int newmark = paster.getMark();
    const double timeoffset = paster.getStartTime().toSeconds();
    double smallest = Time::MAX_TIME;
    for (const SubEntryPtr &e : AppContext::copyBuffer())
        smallest = std::min(smallest, e->getStartTime().toSeconds());
    const double dt = timeoffset - smallest;
    const QList<SubEntryPtr> old = getSelectedSubs();
    undo_.addUndo(*subs_, __("Paste special"));
    for (const SubEntryPtr &e : AppContext::copyBuffer()) {
        auto copy = std::make_shared<SubEntry>(*e);
        if (newmark >= 0) copy->setMark(newmark);
        copy->getStartTime().addTime(dt);
        copy->getFinishTime().addTime(dt);
        subs_->addSorted(copy);
    }
    subs_->revalidateStyles();
    tableHasChanged(old);
}

void MainWindow::editDeleteSelected() {
    if (!subs_) return;
    undo_.addUndo(*subs_, __("Delete subtitles"));
    QList<int> rows = getSelectedRows();
    std::sort(rows.begin(), rows.end(), std::greater<int>());
    for (int r : rows) subs_->remove(r);
    tableHasChanged({});
}

void MainWindow::editDeleteEmptyLines() {
    if (!subs_) return;
    std::unique_ptr<UndoEntry> entry;
    bool changed = false;
    for (int i = subs_->size() - 1; i >= 0; --i) {
        const QString older = subs_->get(i)->getText();
        const QString newer = javaTrim(older);
        if (newer != older || newer.isEmpty()) {
            if (!entry) entry = std::make_unique<UndoEntry>(*subs_, __("Remove empty lines"));
            changed = true;
            if (newer.isEmpty()) subs_->remove(i);
            else subs_->get(i)->setText(newer);
        }
    }
    if (changed) {
        undo_.addUndo(std::move(entry));
        tableHasChanged({});
    } else
        QMessageBox::information(this, __("Remove empty lines"), __("No lines affected"));
}

void MainWindow::editFindReplace() {
    if (!subs_) return;
    auto *dlg = new ReplaceDialog(this, getSelectedRowIdx());
    dlg->setAttribute(Qt::WA_DeleteOnClose);
    dlg->show();
}

void MainWindow::editRegExp() {
    if (!subs_) return;
    ToolRunner::runRegExpReplace(this);
}

void MainWindow::addNewSubtitle(bool isAfter) {
    if (!subs_) return;
    double curdur = NEW_SUB_DURATION, gap = NEW_SUB_GAP;
    const QList<int> sel = getSelectedRows();
    int row;
    if (isAfter)
        row = sel.isEmpty() ? subs_->size() - 1 : sel.last();
    else
        row = sel.isEmpty() ? -1 : sel.first() - 1;
    const double prevtime = row == -1 ? 0 : subs_->get(row)->getFinishTime().toSeconds();
    ++row;
    double nexttime;
    if (row == subs_->size())
        nexttime = (subs_->size() > 0 ? subs_->get(subs_->size() - 1)->getFinishTime().toSeconds() : 0) + 2 * gap + curdur;
    else
        nexttime = subs_->get(row)->getStartTime().toSeconds();
    const double avail = nexttime - prevtime, requested = curdur + 2 * gap;
    if (avail < requested) {
        curdur *= avail / requested;
        gap *= avail / requested;
    }
    const double center = prevtime + (nexttime - prevtime) / 2;
    const double start = center - curdur / 2;
    auto entry = std::make_shared<SubEntry>(Time(start), Time(start + curdur), QString(QLatin1String("")));
    const int where = addSubEntry(entry);
    setSelectedSub(where, true);
}

void MainWindow::splitWith(bool asPrevious) {
    if (!subs_) return;
    const int row = getSelectedRowIdx();
    if (row < 0) return;
    if (asPrevious && row == 0) return;
    if (!asPrevious && row == subs_->size() - 1) return;
    const SubEntryPtr entry = subs_->get(row);
    const QString text = entry->getText();
    const int pos = std::max(0, std::min(subeditor_->caretPosition(), int(text.length())));
    const QString left = javaTrim(text.left(pos));
    const QString right = pos >= text.length() ? QString(QLatin1String("")) : javaTrim(text.mid(pos));
    const SubEntryPtr other = subs_->get(asPrevious ? row - 1 : row + 1);
    const QString space = other->getText().isEmpty() ? QString(QLatin1String("")) : QStringLiteral(" ");
    undo_.addUndo(*subs_, asPrevious ? __("Split with previous subtitle") : __("Split with next subtitle"));
    if (asPrevious) {
        entry->setText(right);
        other->setText(javaTrim(other->getText()) + space + left);
        rowHasChanged(row - 1, false);
        displaySubData();
        setSelectedSub(row - 1, true);
    } else {
        entry->setText(left);
        other->setText(right + space + javaTrim(other->getText()));
        rowHasChanged(row + 1, false);
        displaySubData();
        setSelectedSub(row + 1, true);
        subeditor_->setCaretPosition(0);
    }
    rowHasChanged(row, false);
}

void MainWindow::splitInPlace() {
    if (!subs_) return;
    const int row = getSelectedRowIdx();
    if (row < 0) return;
    const SubEntryPtr entry = subs_->get(row);
    const QString text = entry->getText();
    const int pos = subeditor_->caretPosition();
    if (pos <= 0 || pos >= text.length()) return;
    const QString left = javaTrim(text.left(pos)), right = javaTrim(text.mid(pos));
    const double start = entry->getStartTime().toSeconds();
    const double dur = std::max(0.0, entry->getFinishTime().toSeconds() - start);   // Java getDurationTime clamps
    const double split = start + dur * left.length() / double(left.length() + right.length());
    auto second = std::make_shared<SubEntry>(Time(split), entry->getFinishTime(), right);   // Default style, as the Java
    addSubEntry(second);
    entry->setText(left);
    entry->setFinishTime(Time(split));
    tableHasChanged({entry});
}

void MainWindow::goTo(char cmd) {
    if (!subs_ || subs_->isEmpty()) return;
    int row = getSelectedRowIdx();
    switch (cmd) {
        case 'p': --row; break;
        case 'n': ++row; break;
        case 'u': row -= table_->visibleRowCount(); break;
        case 'd': row += table_->visibleRowCount(); break;
        case 't': row = 0; break;
        case 'b': row = subs_->size() - 1; break;
    }
    row = std::max(0, std::min(row, subs_->size() - 1));
    setSelectedSub(row, true);
}

void MainWindow::goToTime() {
    if (!subs_) return;
    TimeSingleSelectionDialog dlg(Time(3600.0), __("Go to the specified time"), __("Into which time moment do you want to go to"), __("Go to subtitle"), this);
    if (dlg.exec() != QDialog::Accepted) return;
    setSelectedSub(subs_->findSubEntry(dlg.getTime().toSeconds(), true), true);
}

void MainWindow::selectAll() {
    if (!subs_) return;
    ignoreTableSelections_ = true;
    table_->selectAll();
    ignoreTableSelections_ = false;
}

void MainWindow::setMark(int mark) {
    if (!subs_) return;
    const QList<SubEntryPtr> sel = getSelectedSubs();
    undo_.addUndo(*subs_, __("Mark subtitles as {0}", SubEntry::markName(mark)));
    for (const SubEntryPtr &e : sel) e->setMark(mark);
    tableHasChanged(sel);
}

void MainWindow::showTableColumn(int col, bool visible) {
    SubtitleTableModel::setVisibleColumn(col, visible);
    for (MainWindow *w : AppContext::windows()) {
        w->columnActs_[col]->setChecked(SubtitleTableModel::isVisibleColumn(col));
        w->columnPopupActs_[col]->setChecked(SubtitleTableModel::isVisibleColumn(col));
        if (w->subs_) w->tableHasChanged(w->getSelectedSubs());
    }
}

void MainWindow::focusToggle() {
    QWidget *fw = QApplication::focusWidget();
    const bool inTime = fw && (fw->inherits("TimeSpinner") || (fw->parentWidget() && fw->parentWidget()->inherits("TimeSpinner")));
    subeditor_->setFocusOnTimeEditor(fw && !inTime);   // nothing focused → the text (Java)
}

void MainWindow::undoRedo(bool isUndo) {
    if (!subs_) return;
    undo_.applyDoCommand(isUndo, getSelectedRows());
}

void MainWindow::sortAll() {
    if (!subs_) return;
    const QList<SubEntryPtr> sel = getSelectedSubs();
    undo_.addUndo(*subs_, __("Sort"));
    subs_->sort(0, std::numeric_limits<double>::max());
    tableHasChanged(sel);
}

void MainWindow::helpUrl(const QString &url) {
    if (!QDesktopServices::openUrl(QUrl(url)))
        Debug::debug(QStringLiteral("Could not open ") + url);
}

// ---- encoding bar ------------------------------------------------------------------------

void MainWindow::reloadFromBar() {
    if (!subs_) return;
    const QString enc = encodingBar_->encoding();
    if (enc.isNull()) return;
    SubFile &sfile = subs_->getSubFile();
    const bool supportsFps = sfile.getFormat() && sfile.getFormat()->supportsFPS();
    const bool changed = enc != sfile.getEncoding() || (supportsFps && sfile.getFPS() != encodingBar_->fps());
    sfile.setEncoding(enc);
    if (supportsFps) sfile.setFPS(encodingBar_->fps());
    Options::rememberEncoding(enc);
    if (subs_->hasLoadedBytes() && !subs_->getLoadedBytes().isEmpty()) {
        const QString data = FileCommunicator::decodeFrom(subs_->getLoadedBytes(), enc, false);
        if (!data.isNull()) {
            SubFile sf(sfile);
            sf.setFPSForced(supportsFps);   // the bar's FPS, not the one the file declares
            auto fresh = std::make_unique<Subtitles>(sf);
            fresh->populate(sf, data, false);
            fresh->setLoadedBytes(subs_->getLoadedBytes());
            if (!fresh->isEmpty()) {
                // Re-detection may pick another format than the one chosen on
                // the bar; the bar's choice wins (JAVA_BUGS #15).
                const SubFormatPtr chosen = Availabilities::formats().findFromName(encodingBar_->formatName());
                SubFile &fs = fresh->getSubFile();
                if (chosen && (!fs.getFormat() || fs.getFormat()->getName() != chosen->getName())) {
                    fs.setFormat(chosen->newInstance());
                    fs.updateFileByType();
                }
                setSubs(std::move(fresh));
                return;
            }
        }
    }
    if (changed) { setUnsaved(true); showInfo(); }
}

void MainWindow::closeEncodingBar() {
    encodingBar_->hide();
    if (subs_) subs_->releaseLoadedBytes();
    tbEncoding_->setChecked(false);
}

void MainWindow::autoHideEncodingBar() {
    if (subs_ && subs_->hasLoadedBytes()) closeEncodingBar();
}

void MainWindow::showEncodingBar() {
    if (!subs_) return;
    encodingBar_->showFor(subs_->getSubFile().getEncoding(), mfile_.get(), subs_.get());
    encodingBar_->show();
    tbEncoding_->setChecked(true);
}

void MainWindow::toggleEncodingBar() {
    if (!subs_) { tbEncoding_->setChecked(false); return; }
    if (encodingBar_->isVisible()) closeEncodingBar();
    else showEncodingBar();
}

void MainWindow::applyFormatFromBar(const SubFormatPtr &fmt) {
    if (!subs_ || !fmt) return;
    SubFile &sf = subs_->getSubFile();
    if (sf.getFormat() && sf.getFormat()->getName() == fmt->getName()) return;
    sf.setFormat(fmt->newInstance());
    sf.updateFileByType();
    setUnsaved(true);
    showInfo();
}

// ---- preview -----------------------------------------------------------------------------

void MainWindow::enablePreview(bool status) {
    auto visualsUpdate = [this](bool nv) {
        actEnablePreview_->setChecked(nv);
        tbPreview_->setChecked(nv);
        tbPreview_->setIcon(Theme::icon(nv ? QStringLiteral("preview") : QStringLiteral("previewc")));
        tbPreview_->setToolTip((nv ? __("Disable Preview") : __("Enable Preview")) + QStringLiteral("\n") + __("Right mouse click to bring selected row into view"));
        actSnap_->setEnabled(nv);
        actMaxWave_->setEnabled(nv);
        actPlayAudio_->setEnabled(nv);
        tbOrientation_->setEnabled(nv);
    };
    const QPointer<MainWindow> self(this);   // the checks below may run an event loop
    if (status && subs_ && !mfile_->validateMediaFile(subs_.get(), false, this)) {
        if (self) visualsUpdate(false);
        return;
    }
    if (!self) return;
    visualsUpdate(status);
    if (status) {
        preview_->updateMediaFile(mfile_.get());   // starts the analysis of its audio
        preview_->setPreviewEnabled(true);
        mfile_->setSelectorEnabled(false);
        preview_->subsHaveChanged(getSelectedRows());
        preview_->show();
        resetPreviewPanels();
    } else {
        mfile_->setSelectorEnabled(true);
        mfile_->stopPeaks();
        preview_->setPreviewEnabled(false);
        preview_->hide();
    }
    mediaChanged();
}

void MainWindow::setPreviewOrientation(bool horizontal) {
    AutoSaveOptions::setPreviewOrientation(horizontal);
    splitter_->setOrientation(horizontal ? Qt::Vertical : Qt::Horizontal);
    preview_->setOrientation(horizontal);
    resetPreviewPanels();
    tbOrientation_->setIcon(Theme::icon(horizontal ? QStringLiteral("turndown") : QStringLiteral("turnright")));
}

// Java JSplitPane.resetToPreferredSizes: the panes get their preferred sizes
// (scaled to the room there is), once the layout knows them.
void MainWindow::resetPreviewPanels() {
    QTimer::singleShot(0, this, [this]() {
        const bool vertical = splitter_->orientation() == Qt::Vertical;
        QList<int> sizes;
        for (int i = 0; i < splitter_->count(); ++i) {
            const QSize hint = splitter_->widget(i)->sizeHint();
            sizes.append(std::max(1, vertical ? hint.height() : hint.width()));
        }
        splitter_->setSizes(sizes);
    });
}

void MainWindow::setMaxWaveMenu(bool on) { actMaxWave_->setChecked(on); }

void MainWindow::setSnapMenu(bool on) { actSnap_->setChecked(on); }

void MainWindow::showCelebration() {
    if (celebration_) return;
    celebration_ = new CelebrationPanel(basicPanel_);
    setCentral(celebration_);
    celebration_->start();
}

void MainWindow::stopCelebration() {
    if (!celebration_) return;
    celebration_->stop();
    setCentral(splitter_);
    centerStack_->removeWidget(celebration_);
    celebration_->deleteLater();
    celebration_ = nullptr;
}

void MainWindow::setNewVersionCallback(std::function<void(QWidget *)> cb) {
    newsBar_->show();
    newsBar_->setMinimumWidth(newsBar_->sizeHint().width());
    if (!newVersionCb_) newVersionCb_ = std::move(cb);
}

// ---- window lifecycle ---------------------------------------------------------------------

void MainWindow::showEvent(QShowEvent *e) {
    QMainWindow::showEvent(e);
    if (!registered_) {
        registered_ = true;
        AppContext::addWindow(this);
        AutoUpdater::attachWindow(this);
        if (AppContext::windows().size() > 1)
            for (MainWindow *w : AppContext::windows()) w->updateToolsAvailability();
    }
    AppContext::updateRecents();
}

// Drops anywhere on the window (toolbar, editor, …) open the files like the table does.
void MainWindow::dragEnterEvent(QDragEnterEvent *e) {
    if (e->mimeData()->hasUrls()) e->acceptProposedAction();
}

void MainWindow::dropEvent(QDropEvent *e) {
    QStringList paths;
    for (const QUrl &u : e->mimeData()->urls())
        if (u.isLocalFile()) paths.append(u.toLocalFile());
    if (paths.isEmpty()) return;
    e->acceptProposedAction();
    openDroppedFiles(paths);
}

void MainWindow::openDroppedFiles(const QStringList &paths) {
    for (const QString &p : paths) {
        if (!QFileInfo(p).isFile()) continue;   // folders and the like are ignored (Java: isFile)
        if (AppMediaFile::isVideoFile(p)) { newFromVideo(p); continue; }
        loadFileFromHere(SubFile(p, SubFile::EXTENSION_GIVEN), false);
    }
}

// Port of JublerTheme.setComponentDraggable: the toolbar and the editor's
// style strip (and their labels/panels) drag the window; a press that started
// inside the detached editor dialog is ignored.
void MainWindow::makeDraggable(QWidget *w) {
    w->installEventFilter(this);
    for (QWidget *child : w->findChildren<QWidget *>())
        if (qobject_cast<QLabel *>(child) || child->metaObject() == &QWidget::staticMetaObject) child->installEventFilter(this);
}

bool MainWindow::eventFilter(QObject *obj, QEvent *e) {
    auto *w = qobject_cast<QWidget *>(obj);
    if (w && e->type() == QEvent::MouseButtonPress) {
        auto *me = static_cast<QMouseEvent *>(e);
        dragIgnored_ = me->button() != Qt::LeftButton || w->window() != this;
        if (!dragIgnored_) dragOffset_ = me->globalPosition().toPoint() - pos();
    } else if (w && e->type() == QEvent::MouseMove && !dragIgnored_) {
        auto *me = static_cast<QMouseEvent *>(e);
        if (me->buttons() & Qt::LeftButton) {
            if (windowHandle() && windowHandle()->startSystemMove()) dragIgnored_ = true;   // Wayland-friendly
            else move(me->globalPosition().toPoint() - dragOffset_);
        }
    }
    return QMainWindow::eventFilter(obj, e);
}

bool MainWindow::event(QEvent *e) {
    if (e->type() == QEvent::WindowActivate)
        AppContext::setCurrentWindow(this);
    return QMainWindow::event(e);
}

void MainWindow::closeEvent(QCloseEvent *e) {
    e->ignore();
    closeWindow(true, false);
}

void MainWindow::closeWindow(bool unsaveCheck, bool keepApplicationAlive) {
    bool confirmed = false;
    if (isUnsaved() && unsaveCheck && !isEmptyContent()) {
        if (QMessageBox::question(this, __("Quit confirmation"), __("Subtitles are not saved.\nDo you really want to close this window?")) != QMessageBox::Yes)
            return;
        confirmed = true;
    }
    const bool last = AppContext::windows().size() - (AppContext::windows().contains(this) ? 1 : 0) == 0;
    // The quit question comes first: a refused quit must leave the window as it was.
    if (last && !keepApplicationAlive && !AppContext::requestQuit(this, confirmed ? this : nullptr))
        return;
    preview_->setPreviewEnabled(false);
    preview_->release();
    stopCelebration();
    AppContext::removeWindow(this);
    for (MainWindow *w : AppContext::windows())
        if (w->jparent_ == this) w->jparent_ = nullptr;
    if (AppContext::windows().size() == 1) AppContext::windows().first()->updateToolsAvailability();
    AppContext::updateRecents();
    if (AppContext::windows().isEmpty()) {
        if (keepApplicationAlive) {
            AppContext::setWindowPosition(this, true);
            AppContext::jumpWindowPosition(false);
            auto *fresh = new MainWindow();
            fresh->show();
        } else {
            QApplication::exit(0);
        }
    }
    hide();
    deleteLater();
}

void MainWindow::rebuildRecentMenu() {
    recentMenu_->clear();
    if (subs_) {
        recentMenu_->addAction(__("Clone current"), this, [this]() { recentMenuCallback(nullptr); })->setObjectName(IGNORED);
        recentMenu_->addSeparator();
    }
    // The list minus the documents open in any window.
    QList<SubFile> list;
    for (int i = AppContext::recentFiles().size() - 1; i >= 0; --i) {
        const SubFile &sf = AppContext::recentFiles().at(i);
        bool open = false;
        for (MainWindow *w : AppContext::windows())
            if (w->subs_ && w->subs_->getSubFile() == sf) open = true;
        if (!open) list.append(sf);
    }
    if (list.isEmpty()) {
        QAction *a = recentMenu_->addAction(__("-Not any recent items-"));
        a->setObjectName(IGNORED);
        a->setEnabled(false);
        return;
    }
    int counter = 0;
    for (const SubFile &sf : list) {
        ++counter;
        // The subtitles inside a media file: the language (or the stream) tells
        // the entries of one video apart.
        QString label = SystemDependent::displayPath(sf.getSaveFile());
        if (sf.getEmbeddedStream() >= 0)
            label += QStringLiteral(" (%1)").arg(sf.getEmbeddedLanguage().isEmpty() ? QStringLiteral("#%1").arg(sf.getEmbeddedStream())
                                                                                   : sf.getEmbeddedLanguage());
        QAction *a = recentMenu_->addAction(label);
        a->setObjectName(IGNORED);
        if (counter <= 9) a->setShortcut(QKeySequence(Qt::CTRL | (Qt::Key_0 + counter)));
        const SubFile copy = sf;
        connect(a, &QAction::triggered, this, [this, copy]() { recentMenuCallback(&copy); });
    }
}
