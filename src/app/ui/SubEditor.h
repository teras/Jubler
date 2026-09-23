/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QComboBox>
#include <QDialog>
#include <QLabel>
#include <QTextEdit>
#include <QToolButton>
#include <QWidget>

#include "core/subs/SubEntry.h"
#include "core/subs/Subtitles.h"

class OverStylesBar;
class TimeSpinner;
class AnnouncementBanner;
class MetricLabel;

// What the editor needs from its window. Port of the JubFrame calls made by
// `JSubEditor`.
class EditorHost {
public:
    virtual ~EditorHost() = default;
    virtual void keepUndo(const SubEntryPtr &entry) = 0;
    virtual void subTextChanged() = 0;
    virtual void rowHasChanged(int row, bool updateDisplay) = 0;
    virtual void tableHasChanged() = 0;
    virtual int selectedRowIdx() const = 0;
    virtual bool isPlaybackDrivenSelection() const = 0;
    virtual Subtitles *subtitles() const = 0;
    virtual void setToolsLockMenu(bool locked) = 0;
    virtual void addUndo(const Subtitles &subs, const QString &name) = 0;
    virtual void editorAttached(bool attached) = 0;
    virtual void previewRepaint() = 0;
};

// The subtitle editor panel: timing spinners with locks, the styled text
// pane, the override toolbar, the metrics strip and the style controls.
// Port of `JSubEditor` (+ `JSubEditorDialog` when detached).
// Icon + value pair of the metrics strip (lines, line length, cps, …).
class MetricLabel : public QWidget {
    Q_OBJECT
public:
    MetricLabel(const QString &icon, const QString &tip, QWidget *parent = nullptr);
    void setMetric(const QString &text, bool error);
    void clear();   // no icon and no text (before the first metrics)

private:
    QString icon_;
    QLabel *iconL_, *textL_;
};

class SubEditor : public QWidget {
    Q_OBJECT
public:
    explicit SubEditor(EditorHost *host, QWidget *parent = nullptr);

    void setData(const SubEntryPtr &entry);
    SubEntryPtr entry() const { return entry_; }
    // The raw text (toPlainText would turn no-break spaces into spaces).
    QString getSubText() const {
        return text_->document()->toRawText().replace(QChar(0x2029), QLatin1Char('\n')).replace(QChar(0x2028), QLatin1Char('\n'));
    }
    void updateMetrics(const SubEntry &entry);
    void setUnsaved(bool unsaved);
    void setTotal(int n);
    void refreshStyles();
    void showStyle();
    void ignoreSubChanges(bool ignore) { ignoreChanges_ = ignore; }
    bool shouldIgnoreSubChanges() const { return ignoreChanges_; }
    void focusOnText();
    void setFocusOnTimeEditor(bool onTime);
    bool isToolsLocked() const;
    void setToolsLocked(bool locked);
    void setAttached(bool attached);
    bool isAttached() const { return attached_; }
    void setEnabledPanel(bool enabled);
    int caretPosition() const { return text_->textCursor().position(); }
    void setCaretPosition(int pos);
    void removeHelpWanted();
    OverStylesBar *overStyles() const { return overstyle_; }
    QWidget *stylePanel() const { return styleP_; }

private:
    enum Lock { LockStart = 1, LockEnd = 2, LockDuration = 3 };
    void buildUi();
    void lockTimeSpinners(bool enabled);
    void spinnerChanged(int which);
    void onNavigation(int which, int event);
    void changeStyle(StyleType::Id type, const StyleValue &value);
    void onContentsChange(int pos, int removed, int added);
    void passUndoKeys(QWidget *w);
    bool eventFilter(QObject *watched, QEvent *e) override;
    void onCaretChanged();
    void applyEntryFormatting();
    void editStyle();
    void deleteOverrides();
    int lock() const;
    void setLock(int lock);
    TimeSpinner *spinnerFor(int which) const;

    EditorHost *host_;
    SubEntryPtr entry_;
    const SubStyleList *stylesShown_ = nullptr;

    QWidget *timeP_;
    TimeSpinner *start_, *finish_, *dur_;
    QToolButton *lock1_, *lock2_, *lock3_;
    QTextEdit *text_;
    OverStylesBar *overstyle_;
    QWidget *styleP_;
    QToolButton *timeB_, *fontB_, *detachB_, *trashB_, *showStyleB_, *toolsLockB_, *editB_;
    MetricLabel *totalL_, *newlineL_, *lineCharsL_, *cpsL_, *fillL_, *durationL_, *compactL_;
    QLabel *unsavedL_;
    QComboBox *styleListC_;
    AnnouncementBanner *banner_ = nullptr;
    bool panelEnabled_ = false;    // the banner stays usable while the panel is disabled
    bool styleAdvanced_ = false;   // the style editor's Advanced panel, kept between uses
    QDialog *detachedDialog_ = nullptr;
    bool ignoreChanges_ = false;
    bool attached_ = true;
    bool applyingFormat_ = false;
};
