/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/ui/SubEditor.h"

#include <QLineEdit>
#include <QKeyEvent>
#include <QApplication>
#include <QKeySequence>
#include <QFontDatabase>
#include <QGridLayout>
#include <QHBoxLayout>
#include <QMenu>
#include <QMessageBox>
#include <QScrollBar>
#include <QTextBlock>
#include <QTimer>
#include <QVBoxLayout>

#include "app/Theme.h"
#include "app/ui/AnnouncementBanner.h"
#include "app/ui/OverStylesBar.h"
#include "app/ui/StyleEditor.h"
#include "app/ui/TimeSpinner.h"
#include "core/i18n/I18N.h"
#include "core/options/Options.h"
#include "core/os/Debug.h"
#include "core/style/WebSafeFonts.h"

namespace {
const QColor INFOC_E(255, 84, 53);

QString navigationHelp() {
    return QStringLiteral("<html><b>%1</b><ul><li>%2</li><li>%3</li><li>%4</li><li>%5</li><li>%6</li></ul></html>")
        .arg(__("How to navigate with keyboard"),
             __("Change focus from Text area to Editor with {0}+D (default binding)", QKeySequence(Qt::CTRL).toString(QKeySequence::NativeText).remove(QLatin1Char('+'))),
             __("Change focus from one timing to the other with the [ENTER] key"),
             __("Change the currently selected lock with [PAGE-UP]/[PAGE-DOWN] keys"),
             __("Add/substract timing values with [ARROW-UP]/[ARROW-DOWN] keys"),
             __("Change the time resolution with [ARROW-LEFT]/[ARROW-RIGHT] keys"));
}

QToolButton *iconButton(const QString &icon, const QString &tip, bool checkable, QWidget *parent) {
    auto *b = new QToolButton(parent);
    b->setIcon(Theme::icon(icon));
    b->setIconSize(Theme::naturalSize(icon));   // the SVG's own size (24), as the Java
    b->setToolTip(tip);
    b->setCheckable(checkable);
    b->setAutoRaise(true);
    return b;
}

MetricLabel *metricLabel(const QString &icon, const QString &tip, QWidget *parent) {
    return new MetricLabel(icon, tip, parent);
}

void setMetric(MetricLabel *l, const QString &text, bool error) {
    l->setMetric(text, error);
}
}  // namespace

// A QLabel drops its pixmap on setText(), so the icon and the value are two
// labels side by side (16 px lead, 1 px gap, as the Java label had).
MetricLabel::MetricLabel(const QString &icon, const QString &tip, QWidget *parent) : QWidget(parent), icon_(icon) {
    setToolTip(tip);
    auto *lay = new QHBoxLayout(this);
    lay->setContentsMargins(16, 0, 0, 0);
    lay->setSpacing(1);
    iconL_ = new QLabel(this);
    textL_ = new QLabel(this);
    lay->addWidget(iconL_);
    lay->addWidget(textL_);
    setMetric(QString(), false);
}

void MetricLabel::clear() {
    iconL_->clear();
    textL_->clear();
    textL_->hide();
}

void MetricLabel::setMetric(const QString &text, bool error) {
    iconL_->setPixmap(Theme::pixmap(icon_, Theme::naturalSize(icon_).width(), error ? Theme::IconStatus::ERROR : Theme::IconStatus::NORMAL));
    textL_->setText(text);
    textL_->setVisible(!text.isEmpty());
    QPalette p = textL_->palette();
    p.setColor(QPalette::WindowText, error ? INFOC_E : (Theme::isDark() ? QColor(Qt::white) : QColor(Qt::black)));
    textL_->setPalette(p);
}

SubEditor::SubEditor(EditorHost *host, QWidget *parent) : QWidget(parent), host_(host) {
    buildUi();
    setEnabledPanel(false);
}

void SubEditor::buildUi() {
    auto *outer = new QVBoxLayout(this);
    outer->setContentsMargins(0, 0, 0, 0);
    outer->setSpacing(2);

    banner_ = new AnnouncementBanner(this);
    outer->addWidget(banner_);
    connect(banner_, &AnnouncementBanner::emptied, this, &SubEditor::removeHelpWanted, Qt::QueuedConnection);

    // Takes the banner's place once it is gone (Java: PAGE_START wins over NORTH).
    overstyle_ = new OverStylesBar(this);
    overstyle_->setStyleChangeListener([this](StyleType::Id t, const StyleValue &v) { changeStyle(t, v); });
    overstyle_->hide();
    outer->addWidget(overstyle_);

    auto *middle = new QWidget(this);
    auto *ml = new QHBoxLayout(middle);
    ml->setContentsMargins(0, 0, 0, 0);
    // Timing panel (WEST): three rows Start/End/Duration with a lock each.
    timeP_ = new QWidget(middle);
    auto *tg = new QGridLayout(timeP_);
    // As the Java's grids (no gaps): the rows touch, the lock sits next to its field.
    tg->setContentsMargins(4, 0, 0, 0);
    tg->setSpacing(0);
    const QString help = navigationHelp();
    auto addRow = [&](int row, const QString &label, const QString &tip, const QString &lockTip, TimeSpinner *&spin, QToolButton *&lock) {
        auto *l = new QLabel(label, timeP_);
        l->setContentsMargins(0, 0, 6, 0);
        spin = new TimeSpinner(timeP_);
        passUndoKeys(spin);
        lock = new QToolButton(timeP_);
        lock->setCheckable(true);
        lock->setIconSize(Theme::naturalSize(QStringLiteral("lock")));
        // A framed square as tall as the field, as the Java's toggle (a taller
        // button made every row taller).
        const int side = std::max(spin->sizeHint().height(), lock->iconSize().height() + 4);
        lock->setFixedSize(side, side);
        if (!Options::isTimestampTooltipsDisabled()) {
            l->setToolTip(help);
            // As the Java: the first line plain, then the bold "How to navigate" part.
            spin->setToolTip(QStringLiteral("<html>%1<br><br>%2").arg(tip, help.mid(6)));
            lock->setToolTip(QStringLiteral("<html>%1<br><br>%2").arg(lockTip, help.mid(6)));
        }   // disabled: no tooltips at all (Java)
        tg->addWidget(l, row, 0);
        tg->addWidget(spin, row, 1);
        tg->addWidget(lock, row, 2);
    };
    addRow(0, __("Start"), __("Start time of the subtitle"), __("Lock the start time of the subtitle"), start_, lock1_);
    addRow(1, __("End"), __("Stop time of the subtitle"), __("Lock the stop time of the subtitle"), finish_, lock2_);
    addRow(2, __("Duration"), __("Duration of the subtitle"), __("Lock the duration of the subtitle"), dur_, lock3_);
    lock1_->setAutoExclusive(true); lock2_->setAutoExclusive(true); lock3_->setAutoExclusive(true);
    lock3_->setChecked(true);
    for (QToolButton *b : {lock1_, lock2_, lock3_})
        connect(b, &QToolButton::clicked, this, [this]() { lockTimeSpinners(true); });
    connect(start_, &TimeSpinner::timeChanged, this, [this](const Time &) { spinnerChanged(1); });
    connect(finish_, &TimeSpinner::timeChanged, this, [this](const Time &) { spinnerChanged(2); });
    connect(dur_, &TimeSpinner::timeChanged, this, [this](const Time &) { spinnerChanged(3); });
    connect(start_, &TimeSpinner::navigation, this, [this](int e) { onNavigation(1, e); });
    connect(finish_, &TimeSpinner::navigation, this, [this](int e) { onNavigation(2, e); });
    connect(dur_, &TimeSpinner::navigation, this, [this](int e) { onNavigation(3, e); });
    ml->addWidget(timeP_);

    // Text pane (CENTER).
    text_ = new QTextEdit(middle);
    text_->setAcceptRichText(false);
    text_->setMinimumSize(22, 70);
    // No height of its own: a QTextEdit asks for ~190 px, which made the editor
    // tall and spread the Start/End/Duration rows apart; the Java's text pane
    // is as tall as the timing column.
    text_->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Ignored);
    // No undo of its own (it would also revert the applied formatting): the
    // document's Edit ▸ Undo/Redo take the keys, as in the Java.
    text_->setUndoRedoEnabled(false);
    passUndoKeys(text_);
    QFont f = Theme::adjustFont(text_->font(), 1);   // the Java's BOLD, size + 1
    f.setBold(true);
    text_->setFont(f);
    text_->setAlignment(Qt::AlignCenter);
    text_->setContextMenuPolicy(Qt::CustomContextMenu);
    connect(text_, &QTextEdit::customContextMenuRequested, this, [this](const QPoint &pos) {
        QMenu menu(this);
        menu.addAction(__("Cut subtitles"), text_, &QTextEdit::cut);
        menu.addAction(__("Copy subtitles"), text_, &QTextEdit::copy);
        menu.addAction(__("Paste subtitles"), text_, &QTextEdit::paste);
        menu.addAction(__("Select all subtitles"), this, [this]() { text_->selectAll(); text_->setFocus(); });
        menu.exec(text_->mapToGlobal(pos));
    });
    connect(text_->document(), &QTextDocument::contentsChange, this, &SubEditor::onContentsChange);
    connect(text_, &QTextEdit::cursorPositionChanged, this, &SubEditor::onCaretChanged);
    connect(text_, &QTextEdit::selectionChanged, this, &SubEditor::onCaretChanged);
    ml->addWidget(text_, 1);
    outer->addWidget(middle, 1);

    // Style panel (SOUTH): tool buttons + metrics + unsaved | style combo + edit.
    styleP_ = new QWidget(this);
    auto *sl = new QHBoxLayout(styleP_);
    sl->setContentsMargins(0, 0, 0, 0);
    timeB_ = iconButton(QStringLiteral("time"), __("Display/hide subtitle timings"), true, styleP_);
    timeB_->setChecked(true);
    connect(timeB_, &QToolButton::toggled, this, [this](bool on) { timeP_->setVisible(on); });
    fontB_ = iconButton(QStringLiteral("font"), __("Display/hide font attributes"), true, styleP_);
    fontB_->setChecked(true);
    connect(fontB_, &QToolButton::toggled, this, [this](bool on) {
        overstyle_->setFontPanelVisible(on);
        if (detachedDialog_) detachedDialog_->adjustSize();
    });
    detachB_ = iconButton(QStringLiteral("detach"), __("Detach subtitle editor panel"), false, styleP_);
    connect(detachB_, &QToolButton::clicked, this, [this]() { setAttached(!attached_); });
    trashB_ = iconButton(QStringLiteral("trash"), __("Delete styles of this subtitle"), false, styleP_);
    connect(trashB_, &QToolButton::clicked, this, &SubEditor::deleteOverrides);
    showStyleB_ = iconButton(QStringLiteral("hidestyle"), __("Display/hide styles for this subtitle"), true, styleP_);
    showStyleB_->setChecked(true);
    connect(showStyleB_, &QToolButton::toggled, this, [this](bool on) {
        showStyleB_->setIcon(Theme::icon(on ? QStringLiteral("showstyle") : QStringLiteral("hidestyle")));
        showStyle();
    });
    showStyleB_->setIcon(Theme::icon(QStringLiteral("showstyle")));
    toolsLockB_ = iconButton(QStringLiteral("opentool"), __("Tools lock.<br>When locked, tools run with default parameters,<br>without prompting for configuration"), true, styleP_);
    connect(toolsLockB_, &QToolButton::toggled, this, [this](bool on) {
        toolsLockB_->setIcon(Theme::icon(on ? QStringLiteral("lockedtool") : QStringLiteral("opentool")));
        host_->setToolsLockMenu(on);
    });
    // Detach | Time Font | Trash ShowStyle ToolsLock (the Java toolbar).
    auto separator = [this, sl]() {
        auto *sep = new QFrame(styleP_);
        sep->setFrameShape(QFrame::VLine);
        sep->setFrameShadow(QFrame::Sunken);
        sl->addWidget(sep);
    };
    sl->addWidget(detachB_);
    separator();
    sl->addWidget(timeB_);
    sl->addWidget(fontB_);
    separator();
    for (QToolButton *b : {trashB_, showStyleB_, toolsLockB_})
        sl->addWidget(b);
    totalL_ = metricLabel(QStringLiteral("lines"), __("Total subtitles"), styleP_);
    newlineL_ = metricLabel(QStringLiteral("newline"), __("Lines per subtitle"), styleP_);
    lineCharsL_ = metricLabel(QStringLiteral("line"), __("Longest characters per line"), styleP_);
    cpsL_ = metricLabel(QStringLiteral("cps"), __("Characters per second"), styleP_);
    fillL_ = metricLabel(QStringLiteral("fill"), __("Fill subtitle percentage"), styleP_);
    durationL_ = metricLabel(QStringLiteral("dur"), __("Duration"), styleP_);
    compactL_ = metricLabel(QStringLiteral("compact"), __("Subtitle could be compacted into fewer lines"), styleP_);
    unsavedL_ = new QLabel(styleP_);
    unsavedL_->setPixmap(Theme::pixmap(QStringLiteral("save"), Theme::naturalSize(QStringLiteral("save")).width()));
    unsavedL_->setEnabled(false);
    for (MetricLabel *l : {totalL_, newlineL_, lineCharsL_, cpsL_, fillL_, durationL_, compactL_})
        sl->addWidget(l);
    for (MetricLabel *l : {newlineL_, lineCharsL_, cpsL_, fillL_})
        l->clear();   // their icons come with the first metrics (Java)
    totalL_->hide();
    durationL_->hide();
    compactL_->hide();
    setMetric(durationL_, QString(), true);
    setMetric(compactL_, QString(), true);
    sl->addStretch(1);
    sl->addWidget(unsavedL_);   // right-aligned, before the style list
    sl->addSpacing(6);
    styleListC_ = new QComboBox(styleP_);
    styleListC_->setToolTip(__("Style list"));
    connect(styleListC_, &QComboBox::activated, this, [this](int idx) {
        if (ignoreChanges_ || idx < 0 || !entry_ || !host_->subtitles()) return;
        host_->keepUndo(entry_);
        entry_->setStyle(host_->subtitles()->getStyleList().get(idx));
        showStyle();
        host_->rowHasChanged(host_->selectedRowIdx(), false);
    });
    sl->addWidget(styleListC_);
    editB_ = iconButton(QStringLiteral("edittheme"), __("Edit current style"), false, styleP_);
    connect(editB_, &QToolButton::clicked, this, &SubEditor::editStyle);
    sl->addWidget(editB_);
    outer->addWidget(styleP_);
}

// Undo/Redo keys pressed in a text or time field go to the window's Edit
// actions (the fields would otherwise claim them).
namespace {
class UndoKeysFilter : public QObject {
public:
    using QObject::QObject;
    bool eventFilter(QObject *, QEvent *e) override {
        if (e->type() != QEvent::ShortcutOverride) return false;
        auto *ke = static_cast<QKeyEvent *>(e);
        const QKeyCombination k = ke->keyCombination();
        if (ke->matches(QKeySequence::Undo) || ke->matches(QKeySequence::Redo) || k == QKeyCombination(Qt::ControlModifier, Qt::Key_Y)) {
            e->ignore();
            return true;
        }
        return false;
    }
};
}  // namespace

void SubEditor::passUndoKeys(QWidget *w) {
    static UndoKeysFilter *filter = new UndoKeysFilter(qApp);
    w->installEventFilter(filter);
    for (QLineEdit *le : w->findChildren<QLineEdit *>()) le->installEventFilter(filter);
}

bool SubEditor::eventFilter(QObject *watched, QEvent *e) {
    if (watched == detachedDialog_ && e->type() == QEvent::KeyPress && static_cast<QKeyEvent *>(e)->key() == Qt::Key_Escape)
        return true;
    return QWidget::eventFilter(watched, e);
}

// ---- data -------------------------------------------------------------------------

void SubEditor::setData(const SubEntryPtr &entry) {
    entry_ = entry;
    ignoreChanges_ = true;
    text_->setPlainText(entry->getText());
    text_->moveCursor(QTextCursor::End);   // the Java caret follows the inserted text
    start_->setTimeValue(entry->getStartTime());
    finish_->setTimeValue(entry->getFinishTime());
    dur_->setTimeValue(Time(entry->getFinishTime().toSeconds() - entry->getStartTime().toSeconds()));
    if (host_->subtitles() && stylesShown_ != &host_->subtitles()->getStyleList())
        refreshStyles();
    if (!panelEnabled_)
        setEnabledPanel(true);
    showStyle();
    overstyle_->updateVisualData(*entry, 0, 0);
    ignoreChanges_ = false;
}

void SubEditor::refreshStyles() {
    ignoreChanges_ = true;
    styleListC_->clear();
    if (host_->subtitles()) {
        stylesShown_ = &host_->subtitles()->getStyleList();
        for (const SubStylePtr &s : stylesShown_->all())
            styleListC_->addItem(s->getName());
    }
    styleListC_->setEnabled(panelEnabled_ && styleListC_->count() > 1);
    ignoreChanges_ = false;
}

void SubEditor::setEnabledPanel(bool enabled) {
    panelEnabled_ = enabled;
    for (QWidget *child : findChildren<QWidget *>(Qt::FindDirectChildrenOnly))
        if (child != banner_) child->setEnabled(enabled);
    for (QWidget *w : {static_cast<QWidget *>(timeB_), static_cast<QWidget *>(fontB_), static_cast<QWidget *>(detachB_),
                       static_cast<QWidget *>(trashB_), static_cast<QWidget *>(showStyleB_), static_cast<QWidget *>(toolsLockB_),
                       static_cast<QWidget *>(lock1_), static_cast<QWidget *>(lock2_), static_cast<QWidget *>(lock3_),
                       static_cast<QWidget *>(text_), static_cast<QWidget *>(editB_)})
        w->setEnabled(enabled);
    styleListC_->setEnabled(enabled && styleListC_->count() > 1);
    lockTimeSpinners(enabled);
    if (!enabled || !entry_) {
        text_->setPalette(QApplication::palette());
    } else
        showStyle();
}

int SubEditor::lock() const {
    if (lock1_->isChecked()) return LockStart;
    if (lock2_->isChecked()) return LockEnd;
    return LockDuration;
}

void SubEditor::setLock(int lock) {
    (lock == LockStart ? lock1_ : lock == LockEnd ? lock2_ : lock3_)->setChecked(true);
}

void SubEditor::lockTimeSpinners(bool enabled) {
    start_->setEnabled(enabled);
    finish_->setEnabled(enabled);
    dur_->setEnabled(enabled);
    switch (lock()) {
        case LockStart: start_->setEnabled(false); break;
        case LockEnd: finish_->setEnabled(false); break;
        default: dur_->setEnabled(false);
    }
    for (QToolButton *b : {lock1_, lock2_, lock3_})
        b->setIcon(Theme::icon(b->isChecked() ? QStringLiteral("lock") : QStringLiteral("unlock")));
}

TimeSpinner *SubEditor::spinnerFor(int which) const {
    return which == 1 ? start_ : which == 2 ? finish_ : dur_;
}

void SubEditor::spinnerChanged(int which) {
    if (ignoreChanges_ || !entry_ || host_->selectedRowIdx() < 0)
        return;
    host_->keepUndo(entry_);
    double start = start_->seconds(), finish = finish_->seconds(), dur = dur_->seconds();
    switch (lock()) {
        case LockStart:
            if (which == 2) {
                if (finish < start) finish = start;
                dur = finish - start;
            } else
                finish = start + dur;
            break;
        case LockEnd:
            if (which == 1) {
                if (start > finish) start = finish;
                dur = finish - start;
            } else {
                if (dur > finish) dur = finish;
                start = finish - dur;
            }
            break;
        default:
            if (which == 1) finish = start + dur;
            else start = finish - dur;
    }
    ignoreChanges_ = true;
    start_->setTimeValue(Time(start));
    finish_->setTimeValue(Time(finish));
    dur_->setTimeValue(Time(dur));
    ignoreChanges_ = false;
    entry_->setStartTime(Time(start));
    entry_->setFinishTime(Time(finish));
    host_->rowHasChanged(host_->selectedRowIdx(), true);
}

void SubEditor::onNavigation(int which, int event) {
    const int cur = lock();
    if (event == TimeSpinner::PREVIOUS_LOCK) {
        const int next = cur == LockStart ? LockDuration : cur == LockEnd ? LockStart : LockEnd;
        setLock(next);
        if ((next == LockDuration && which == 3)) finish_->setFocus();
        else if (next == LockStart && which == 1) dur_->setFocus();
        else if (next == LockEnd && which == 2) start_->setFocus();
        lockTimeSpinners(true);
    } else if (event == TimeSpinner::NEXT_LOCK) {
        const int next = cur == LockStart ? LockEnd : cur == LockEnd ? LockDuration : LockStart;
        setLock(next);
        if (next == LockEnd && which == 2) dur_->setFocus();
        else if (next == LockDuration && which == 3) start_->setFocus();
        else if (next == LockStart && which == 1) finish_->setFocus();
        lockTimeSpinners(true);
    } else {  // NEXT_TIME_SPINNER: toggle between the two unlocked spinners
        switch (cur) {
            case LockStart: (which == 2 ? dur_ : finish_)->setFocus(); break;
            case LockEnd: (which == 1 ? dur_ : start_)->setFocus(); break;
            default: (which == 1 ? finish_ : start_)->setFocus();
        }
    }
}

void SubEditor::setFocusOnTimeEditor(bool onTime) {
    if (onTime)
        (lock() == LockStart ? finish_ : start_)->setFocus();
    else
        text_->setFocus();
}

void SubEditor::focusOnText() {
    QWidget *fw = QApplication::focusWidget();
    if (fw && (fw == start_ || fw == finish_ || fw == dur_ || (fw->parentWidget() && (fw->parentWidget() == start_ || fw->parentWidget() == finish_ || fw->parentWidget() == dur_))))
        return;  // editing a time: do not interrupt
    text_->setFocus();
}

bool SubEditor::isToolsLocked() const { return toolsLockB_->isChecked(); }
void SubEditor::setToolsLocked(bool locked) { toolsLockB_->setChecked(locked); }

void SubEditor::setCaretPosition(int pos) {
    QTextCursor c = text_->textCursor();
    c.setPosition(std::max(0, std::min(pos, int(text_->toPlainText().length()))));
    text_->setTextCursor(c);
}

// ---- text edits ---------------------------------------------------------------------

void SubEditor::onContentsChange(int pos, int removed, int added) {
    if (ignoreChanges_ || applyingFormat_ || !entry_)
        return;
    if (removed == added && getSubText() == entry_->getText())
        return;   // a formatting-only change: the text (and its overrides) stay
    host_->subTextChanged();
    if (removed > 0) entry_->removeText(pos, removed);
    if (added > 0) entry_->insertText(pos, added);
    QTimer::singleShot(0, this, [this]() { showStyle(); host_->previewRepaint(); });
}

void SubEditor::onCaretChanged() {
    if (!entry_ || ignoreChanges_) return;
    const QTextCursor c = text_->textCursor();
    const int start = std::min(c.anchor(), c.position()), end = std::max(c.anchor(), c.position());
    overstyle_->updateVisualData(*entry_, start, end);
}

void SubEditor::changeStyle(StyleType::Id type, const StyleValue &value) {
    if (!entry_) return;
    host_->subTextChanged();
    const QTextCursor c = text_->textCursor();
    const int start = std::min(c.anchor(), c.position()), end = std::max(c.anchor(), c.position());
    entry_->setOverStyle(type, value, start, end);
    QTimer::singleShot(0, this, [this]() { showStyle(); host_->previewRepaint(); text_->setFocus(); });
}

void SubEditor::showStyle() {
    if (!entry_ || !host_->subtitles()) return;
    const SubStyleList &styles = host_->subtitles()->getStyleList();
    int idx = entry_->getStyle() ? styles.indexOf(entry_->getStyle()) : -1;
    if (idx < 0) idx = entry_->getStyle() ? styles.findStyleIndex(entry_->getStyle()->getName()) : 0;
    ignoreChanges_ = true;
    styleListC_->setCurrentIndex(idx);
    ignoreChanges_ = false;
    applyEntryFormatting();
}

void SubEditor::applyEntryFormatting() {
    applyingFormat_ = true;
    const bool wasIgnoring = ignoreChanges_;
    ignoreChanges_ = true;
    const QTextCursor saved = text_->textCursor();
    QTextCursor c(text_->document());
    c.select(QTextCursor::Document);
    QTextCharFormat plain;
    c.setCharFormat(plain);
    const SubStylePtr style = entry_->getStyle() ? entry_->getStyle() : host_->subtitles()->getStyleList().get(0);
    QPalette pal = QApplication::palette();
    if (showStyleB_->isChecked() && style) {
        pal.setColor(QPalette::Base, style->color(StyleType::SHADOW).color());
        pal.setColor(QPalette::Text, style->color(StyleType::PRIMARY).color());
        text_->setPalette(pal);
        const int len = entry_->getText().length();
        // The runs count characters of the entry's text, the formatting goes on
        // the document: the two must agree. When they do not, the document is
        // the one that can be addressed, so the runs are clamped to it — a
        // subtitle keeps its styling either way — and the disagreement is
        // reported, because it means the editor is showing something other
        // than what it holds. (It did once: QTextDocument folds a CRLF into a
        // single paragraph separator, and CRs were reaching the text.)
        const int docLen = text_->document()->characterCount() - 1;
        if (docLen != len)
            Debug::debug(QStringLiteral("Subtitle editor: the entry holds %1 characters, the document shows %2; the styling follows the document").arg(len).arg(docLen));
        auto applyRuns = [&](StyleType::Id id, auto fmtSetter) {
            const StyleValue basic = style->get(id);
            auto apply = [&](int from, int length, const StyleValue &v) {
                const int start = std::min(from, docLen);
                const int end = std::min(from + length, docLen);
                if (length <= 0 || end <= start) return;
                QTextCursor rc(text_->document());
                rc.setPosition(start);
                rc.setPosition(end, QTextCursor::KeepAnchor);
                QTextCharFormat f;
                fmtSetter(f, v);
                rc.mergeCharFormat(f);
            };
            if (const Styleover *over = entry_->getStyleover(id))
                over->forEachRun(basic, len, apply);
            else
                apply(0, len, basic);
        };
        applyRuns(StyleType::FONTNAME, [](QTextCharFormat &f, const StyleValue &v) { f.setFontFamilies({WebSafeFonts::renderFamily(std::get<QString>(v))}); });
        applyRuns(StyleType::FONTSIZE, [](QTextCharFormat &f, const StyleValue &v) { f.setProperty(QTextFormat::FontPixelSize, std::max(1, std::get<int>(v))); });   // Swing sizes are pixels
        applyRuns(StyleType::BOLD, [](QTextCharFormat &f, const StyleValue &v) { f.setFontWeight(std::get<bool>(v) ? QFont::Bold : QFont::Normal); });
        applyRuns(StyleType::ITALIC, [](QTextCharFormat &f, const StyleValue &v) { f.setFontItalic(std::get<bool>(v)); });
        applyRuns(StyleType::UNDERLINE, [](QTextCharFormat &f, const StyleValue &v) { f.setFontUnderline(std::get<bool>(v)); });
        applyRuns(StyleType::STRIKETHROUGH, [](QTextCharFormat &f, const StyleValue &v) { f.setFontStrikeOut(std::get<bool>(v)); });
        applyRuns(StyleType::PRIMARY, [](QTextCharFormat &f, const StyleValue &v) { f.setForeground(std::get<AlphaColor>(v).color()); });
        applyRuns(StyleType::SHADOW, [&](QTextCharFormat &f, const StyleValue &v) {
            if (std::get<AlphaColor>(v) != style->color(StyleType::SHADOW)) f.setBackground(std::get<AlphaColor>(v).color());
        });
        // Alignment of the whole paragraph from the direction in effect.
        Direction d = style->direction();
        if (const auto v = entry_->overValue(StyleType::DIRECTION, 0, len); v && std::holds_alternative<Direction>(*v)) d = std::get<Direction>(*v);
        Qt::Alignment al = Qt::AlignCenter;
        if (d == Direction::TOPLEFT || d == Direction::LEFT || d == Direction::BOTTOMLEFT) al = Qt::AlignLeft;
        else if (d == Direction::TOPRIGHT || d == Direction::RIGHT || d == Direction::BOTTOMRIGHT) al = Qt::AlignRight;
        QTextCursor pc(text_->document());
        pc.select(QTextCursor::Document);
        QTextBlockFormat bf;
        bf.setAlignment(al);
        pc.mergeBlockFormat(bf);
    } else {
        pal.setColor(QPalette::Base, Qt::white);
        pal.setColor(QPalette::Text, Qt::black);
        text_->setPalette(pal);
        QTextCharFormat f;
        f.setFontFamilies({WebSafeFonts::renderFamily(QStringLiteral("Arial"))});
        f.setProperty(QTextFormat::FontPixelSize, 24);
        f.setFontWeight(QFont::Normal);   // plain, as the Java (the pane's own font is bold)
        f.setForeground(Qt::black);
        c.mergeCharFormat(f);
        QTextCursor pc(text_->document());
        pc.select(QTextCursor::Document);
        QTextBlockFormat bf;
        bf.setAlignment(Qt::AlignCenter);
        pc.mergeBlockFormat(bf);
    }
    text_->setTextCursor(saved);
    ignoreChanges_ = wasIgnoring;
    applyingFormat_ = false;
}

// ---- metrics ------------------------------------------------------------------------

void SubEditor::updateMetrics(const SubEntry &entry) {
    const SubMetrics m = entry.getMetrics();
    setMetric(newlineL_, QString::number(m.lines), m.lines > Options::getMaxLines());
    setMetric(lineCharsL_, QString::number(m.linelength), m.linelength > Options::getMaxLineLength());
    const QString cps = std::isinf(m.cps) ? QStringLiteral("∞") : QString::number(int(m.cps * 10) / 10.0, 'f', 1);
    setMetric(cpsL_, cps, m.cps > Options::getMaxCPS());
    setMetric(fillL_, QString::number(m.fillpercent) + QLatin1Char('%'), m.fillpercent < Options::getFillPercent());
    totalL_->setVisible(true);
    const double dur = entry.getFinishTime().differenceInSecs(entry.getStartTime());
    if (dur > Options::getMaxDuration()) { durationL_->setToolTip(__("Duration time is too big")); durationL_->show(); }
    else if (dur < Options::getMinDuration()) { durationL_->setToolTip(__("Duration time is too small")); durationL_->show(); }
    else durationL_->hide();
    compactL_->setVisible(Options::isCompactSubs() && m.length < (m.lines - 1) * Options::getMaxLineLength());
    const_cast<SubEntry &>(entry).updateQuality(m);
}

void SubEditor::setTotal(int n) {
    setMetric(totalL_, QString::number(n), false);
}

void SubEditor::setUnsaved(bool unsaved) {
    unsavedL_->setEnabled(unsaved);
    unsavedL_->setToolTip(unsaved ? __("Subtitles need to be saved") : QString());
}

void SubEditor::removeHelpWanted() {
    if (banner_) {
        banner_->hide();
        banner_->deleteLater();
        banner_ = nullptr;
        overstyle_->show();
    }
}

// ---- styles ---------------------------------------------------------------------------

void SubEditor::deleteOverrides() {
    if (!entry_ || !host_->subtitles()) return;
    if (QMessageBox::question(this, __("Delete current subtitle style"), __("Are you sure you want to delete the override styles of this subtitle?")) != QMessageBox::Yes)
        return;
    Subtitles snapshot(*host_->subtitles());
    entry_->resetOverStyle();
    showStyle();
    host_->addUndo(snapshot, __("Cleanup style"));
    host_->previewRepaint();
}

void SubEditor::editStyle() {
    if (!entry_ || !host_->subtitles()) return;
    Subtitles *subs = host_->subtitles();
    SubStylePtr style = entry_->getStyle() ? entry_->getStyle() : subs->getStyleList().get(0);
    const SubStyle backup(*style);
    Subtitles snapshot(*subs);
    StyleEditor dlg(subs->getStyleList(), this);
    dlg.setAdvancedShown(styleAdvanced_);
    const StyleEditor::Result res = dlg.editStyle(style);
    styleAdvanced_ = dlg.advancedShown();
    if (res.cancelled) {
        style->setValues(backup);
        style->setNameRaw(backup.getName());
        return;
    }
    SubStylePtr current = res.style;
    if (current != style) {
        style->setValues(backup);
        style->setNameRaw(backup.getName());
    }
    entry_->setStyle(current);
    if (res.deleted) {
        subs->getStyleList().remove(current);
        subs->revalidateStyles();
    }
    refreshStyles();
    showStyle();
    host_->addUndo(snapshot, __("Edit style"));
    host_->tableHasChanged();
}

// ---- attach / detach ------------------------------------------------------------------

void SubEditor::setAttached(bool attached) {
    if (attached == attached_) return;
    attached_ = attached;
    detachB_->setIcon(Theme::icon(attached ? QStringLiteral("detach") : QStringLiteral("attach")));
    if (attached) {
        if (detachedDialog_) {
            setParent(nullptr);
            detachedDialog_->hide();
            detachedDialog_->deleteLater();
            detachedDialog_ = nullptr;
        }
        host_->editorAttached(true);
    } else {
        host_->editorAttached(false);
        detachedDialog_ = new QDialog(window(), Qt::Tool);
        detachedDialog_->setWindowTitle(__("Subtitle editor"));
        auto *l = new QVBoxLayout(detachedDialog_);
        l->setContentsMargins(4, 4, 4, 4);
        setParent(detachedDialog_);
        l->addWidget(this);
        show();
        connect(detachedDialog_, &QDialog::rejected, this, [this]() { setAttached(true); });
        detachedDialog_->installEventFilter(this);   // Escape does not re-attach (Java: only the close button)
        detachedDialog_->show();
    }
}
