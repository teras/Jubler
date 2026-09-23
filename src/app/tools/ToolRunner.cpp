/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/tools/ToolRunner.h"

#include <QAction>
#include <QCheckBox>
#include <QComboBox>
#include <QDialog>
#include <QDialogButtonBox>
#include <QFileInfo>
#include <QGroupBox>
#include <QHBoxLayout>
#include <QLabel>
#include <QLineEdit>
#include <QListWidget>
#include <QMenu>
#include <QMessageBox>
#include <QPushButton>
#include <QRadioButton>
#include <QPainter>
#include <QSlider>
#include <QStyle>
#include <QLocale>
#include <QSet>
#include <QSpinBox>
#include <QVBoxLayout>
#include <map>
#include <memory>

#include "app/AppContext.h"
#include "app/dialogs/EncodingBar.h"
#include "app/dialogs/TimeSelectionDialogs.h"
#include "app/tools/ReplaceList.h"
#include "app/tools/SpellDialogs.h"
#include "app/tools/TranslateDialogs.h"
#include "app/media/AppMediaFile.h"
#include "app/subdownload/Providers.h"
#include "app/subdownload/SubDownloadTool.h"
#include "app/subdownload/SubDownloadWindow.h"
#include "app/ui/MainWindow.h"
#include "app/ui/TimeSpinner.h"
#include "core/i18n/I18N.h"
#include "core/subs/Subtitles.h"
#include "core/tools/SpellTranslateTools.h"
#include "core/undo/UndoList.h"
#include "core/util/JavaCompat.h"

namespace {

// The value labels a Swing slider paints under its major ticks
// (setPaintLabels): a row under the slider, aligned with the handle positions.
class SliderLabels : public QWidget {
public:
    SliderLabels(QSlider *slider, int step, QWidget *parent) : QWidget(parent), slider_(slider), step_(step) {
        setFixedHeight(fontMetrics().height());
    }

protected:
    void paintEvent(QPaintEvent *) override {
        QPainter p(this);
        p.setPen(palette().color(isEnabled() ? QPalette::Active : QPalette::Disabled, QPalette::WindowText));
        const int handle = slider_->style()->pixelMetric(QStyle::PM_SliderLength, nullptr, slider_);
        const int span = slider_->width() - handle;
        const int dx = slider_->mapTo(parentWidget(), QPoint(0, 0)).x() - mapTo(parentWidget(), QPoint(0, 0)).x();
        for (int v = slider_->minimum(); v <= slider_->maximum(); v += step_) {
            const int x = dx + handle / 2 + QStyle::sliderPositionFromValue(slider_->minimum(), slider_->maximum(), v, span);
            const QString t = QString::number(v);
            const int w = fontMetrics().horizontalAdvance(t);
            p.drawText(std::clamp(x - w / 2, 0, std::max(0, width() - w)), fontMetrics().ascent(), t);
        }
    }

private:
    QSlider *slider_;
    int step_;
};

// The slider with its value labels under it.
QWidget *labelledSlider(QSlider *slider, int step, QWidget *parent) {
    auto *box = new QWidget(parent);
    auto *v = new QVBoxLayout(box);
    v->setContentsMargins(0, 0, 0, 0);
    v->setSpacing(0);
    slider->setParent(box);
    v->addWidget(slider);
    v->addWidget(new SliderLabels(slider, step, box));
    return box;
}

// Show `panel` inside a modal OK/Cancel dialog and take it back afterwards
// (the panel is kept alive between runs, so every widget state persists).
bool runDialog(MainWindow *window, const QString &title, QWidget *panel) {
    QDialog dlg(window);
    dlg.setWindowTitle(title);
    auto *lay = new QVBoxLayout(&dlg);
    panel->setParent(&dlg);
    lay->addWidget(panel);
    panel->show();
    auto *buttons = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel, &dlg);
    QObject::connect(buttons, &QDialogButtonBox::accepted, &dlg, &QDialog::accept);
    QObject::connect(buttons, &QDialogButtonBox::rejected, &dlg, &QDialog::reject);
    lay->addWidget(buttons);
    const int r = dlg.exec();
    lay->removeWidget(panel);
    panel->setParent(nullptr);
    return r == QDialog::Accepted;
}

QString windowName(MainWindow *w) {
    return w->getSubtitles() ? QFileInfo(w->getSubtitles()->getSubFile().getStrippedFile()).fileName() : QString();
}

// ---- base ------------------------------------------------------------------------------------

class ToolGui {
public:
    virtual ~ToolGui() = default;
    virtual Tool *tool() const = 0;
    virtual bool isAvailable(MainWindow *) const { return true; }
    virtual void updateData(MainWindow *) {}
    virtual bool execute(MainWindow *window) = 0;
};

// The dialog half of `TimeBaseTool`: the selection area (full model or time
// region), the tool's own panel below it, the tools lock, undo and refresh.
class TimeBaseToolGui : public ToolGui {
public:
    explicit TimeBaseToolGui(std::shared_ptr<TimeBaseTool> tool) : tool_(std::move(tool)) {}
    Tool *tool() const override { return tool_.get(); }
    TimeArea *timeArea() { ensurePanel(); return area_; }

    void updateData(MainWindow *window) override {
        ensurePanel();
        window_ = window;
        tool_->setSubtitles(window->getSubtitles());
        area_->updateData(window->getSubtitles(), window->getSelectedRows());
        if (forceFullRange_) {
            if (auto *full = dynamic_cast<TimeFullSelection *>(area_)) full->forceFullRangeSelection();
            forceFullRange_ = false;
        }
        updateToolData(window);
    }

    bool execute(MainWindow *window) override {
        ensurePanel();
        Subtitles *subs = window->getSubtitles();
        if (!subs) return false;
        const bool locked = window->isToolLocked();
        if (!locked && !runDialog(window, tool_->getToolTitle(), panel_)) return false;
        QList<SubEntryPtr> list = locked ? window->getSelectedSubs() : area_->getAffectedSubs();
        if (list.isEmpty()) return false;
        if (!storeSelections(window)) return false;
        const QList<SubEntryPtr> selected = window->getSelectedSubs();
        // The snapshot is taken before, but recorded only when the tool did
        // something (a failed start leaves the undo list alone).
        auto snapshot = std::make_unique<UndoEntry>(*subs, tool_->getToolTitle());
        // A successful run that left the document as it was (a zero shift,
        // times already rounded) records no undo step either.
        const auto documentState = [subs]() {
            QList<QVariant> state;
            for (const SubEntryPtr &e : subs->entries())
                state << QVariant::fromValue(quintptr(e.get())) << e->getText() << e->getStartTime().getMillis()
                      << e->getFinishTime().getMillis() << QVariant::fromValue(quintptr(e->getStyle().get())) << e->getMark();
            return state;
        };
        const QList<QVariant> documentBefore = documentState();
        // A failed run leaves no trace: the marks and the (Fixer) order set
        // before the tool ran are put back.
        const QList<SubEntryPtr> order = subs->entries();
        QList<int> marks;
        for (const SubEntryPtr &e : list) marks.append(e->getMark());
        area_->updateSubsMark(list);   // whenever the checkbox is ticked, also on locked runs
        const QList<SubEntryPtr> marked = list;
        // What a failing tool may already have changed (a translation that
        // stopped half way): then the undo step is kept, as the Java did.
        struct State { QString text; double start, finish; };
        QList<State> before;
        for (const SubEntryPtr &e : list) before.append({e->getText(), e->getStartTime().toSeconds(), e->getFinishTime().toSeconds()});
        const auto changed = [&marked, &before]() {
            for (int i = 0; i < marked.size(); ++i)
                if (marked[i]->getText() != before[i].text || marked[i]->getStartTime().toSeconds() != before[i].start ||
                    marked[i]->getFinishTime().toSeconds() != before[i].finish)
                    return true;
            return false;
        };
        beforeAffect(window, list);
        const bool ok = tool_->affect(list);
        if (ok) {
            if (documentState() != documentBefore) window->getUndoList()->addUndo(std::move(snapshot));
            afterAffect(window);
        } else if (changed()) {
            window->getUndoList()->addUndo(std::move(snapshot));
        } else {
            for (int i = 0; i < marked.size(); ++i) marked[i]->setMark(marks[i]);
            if (subs->entries() != order) subs->setSublist(order);
        }
        window->tableHasChanged(selected);
        return ok;
    }

protected:
    virtual QWidget *constructToolVisuals() { return nullptr; }
    // Read the widgets into the tool; false aborts (message already shown).
    virtual bool storeSelections(MainWindow *) { return true; }
    virtual void updateToolData(MainWindow *) {}
    virtual void beforeAffect(MainWindow *, QList<SubEntryPtr> &) {}
    virtual void afterAffect(MainWindow *) {}
    void requestFullRange() { forceFullRange_ = true; }

    std::shared_ptr<TimeBaseTool> tool_;
    MainWindow *window_ = nullptr;

private:
    void ensurePanel() {
        if (panel_) return;
        panel_ = new QWidget();
        auto *lay = new QVBoxLayout(panel_);
        lay->setContentsMargins(0, 0, 0, 0);
        area_ = tool_->isFreeform() ? static_cast<TimeArea *>(new TimeFullSelection(panel_)) : new TimeRegion(panel_);
        lay->addWidget(area_);
        if (QWidget *visuals = constructToolVisuals()) {
            visuals->setParent(panel_);
            lay->addWidget(visuals);
        }
    }
    QWidget *panel_ = nullptr;
    TimeArea *area_ = nullptr;
    bool forceFullRange_ = false;
};

// ---- Shift time -------------------------------------------------------------------------------

class ShiftTimeGui : public TimeBaseToolGui {
public:
    explicit ShiftTimeGui(std::shared_ptr<ShiftTime> t) : TimeBaseToolGui(t), shift_(std::move(t)) {}
    bool setValues(const TimeSync &first, const TimeSync &second) {
        if (!shift_->setValues(first, second)) return false;
        timeArea();   // builds the widgets
        const double d = shift_->shift();
        sign_->setCurrentIndex(d < 0 ? 1 : 0);
        spinner_->setTimeValue(Time(std::abs(d)));
        requestFullRange();
        return true;
    }

protected:
    QWidget *constructToolVisuals() override {
        auto *box = new QGroupBox(__("Shift Subtitles"));
        auto *lay = new QHBoxLayout(box);
        sign_ = new QComboBox(box);
        sign_->addItems({QStringLiteral(" + "), QStringLiteral(" - ")});
        sign_->setToolTip(__("Either increase or decrease the time"));
        spinner_ = new TimeSpinner(box);
        spinner_->setTimeValue(Time(1.0));
        spinner_->setToolTip(__("The amount of time in order to shift the subtitles"));
        lay->addWidget(sign_);
        lay->addWidget(spinner_, 1);
        return box;
    }
    bool storeSelections(MainWindow *) override {
        const double v = spinner_->seconds();
        shift_->setShift(sign_->currentIndex() == 1 ? -v : v);
        return true;
    }

private:
    std::shared_ptr<ShiftTime> shift_;
    QComboBox *sign_ = nullptr;
    TimeSpinner *spinner_ = nullptr;
};

// ---- Recode -----------------------------------------------------------------------------------

class RecodeTimeGui : public TimeBaseToolGui {
public:
    explicit RecodeTimeGui(std::shared_ptr<RecodeTime> t) : TimeBaseToolGui(t), recode_(std::move(t)) {}
    bool setValues(const TimeSync &first, const TimeSync &second) {
        double c = 0, f = 1;
        if (!recode_->setValues(first, second, c, f)) return false;
        timeArea();
        // Full precision, as Java's Double.toString (6 digits put subtitles tens of ms off).
        const auto javaDouble = [](double v) {
            QString t = QString::number(v, 'g', QLocale::FloatingPointShortest);
            if (!t.contains(QLatin1Char('.')) && !t.contains(QLatin1Char('e'))) t += QStringLiteral(".0");
            return t;
        };
        factor_->setText(javaDouble(f));
        center_->setText(javaDouble(c));
        custom_->setChecked(true);
        toggleMode(false);
        requestFullRange();
        return true;
    }

protected:
    QWidget *constructToolVisuals() override {
        auto *w = new QWidget();
        auto *lay = new QVBoxLayout(w);
        lay->setContentsMargins(0, 0, 0, 0);
        auto_ = new QRadioButton(__("Automatically compute based on FPS"), w);
        auto_->setToolTip(__("Use the following FPS in order to automatically compute the desired recoding"));
        auto_->setChecked(true);
        lay->addWidget(auto_);
        auto *fpsRow = new QHBoxLayout();
        from_ = new RateChooser(w);
        to_ = new RateChooser(w);
        fpsRow->addWidget(from_);
        arrowL_ = new QLabel(QStringLiteral(" -> "), w);
        fpsRow->addWidget(arrowL_);
        fpsRow->addWidget(to_);
        lay->addLayout(fpsRow);
        custom_ = new QRadioButton(__("Custom"), w);
        custom_->setToolTip(__("Use a custom factor in order to perform the recoding"));
        lay->addWidget(custom_);
        auto *fr = new QHBoxLayout();
        factorL_ = new QLabel(__("Recoding factor"), w);
        factor_ = new QLineEdit(QStringLiteral("1.0"), w);
        factor_->setToolTip(__("The value of the custom factor which will do the recoding"));
        fr->addWidget(factorL_);
        fr->addWidget(factor_, 1);
        lay->addLayout(fr);
        auto *cr = new QHBoxLayout();
        centerL_ = new QLabel(__("Central time"), w);
        center_ = new QLineEdit(QStringLiteral("0.0"), w);
        center_->setToolTip(__("The central time point which the recoding occurs. Usually left to 0 to apply evenly to the whole file."));
        cr->addWidget(centerL_);
        cr->addWidget(center_, 1);
        lay->addLayout(cr);
        QObject::connect(auto_, &QRadioButton::toggled, w, [this](bool on) { toggleMode(on); });
        toggleMode(true);
        return w;
    }
    void updateToolData(MainWindow *window) override {
        from_->setDataFiles(window->getMediaFile(), window->getSubtitles());
        to_->setDataFiles(window->getMediaFile(), window->getSubtitles());
    }
    bool storeSelections(MainWindow *) override {
        bool ok = false;
        double center = center_->text().toDouble(&ok);
        if (!ok) center = 0;
        double factor = 1;
        if (auto_->isChecked())
            factor = from_->fps() / to_->fps();
        else {
            factor = factor_->text().toDouble(&ok);
            if (!ok) factor = 1;
        }
        recode_->setCustom(center, factor);
        return true;
    }

private:
    void toggleMode(bool automatic) {
        from_->setChooserEnabled(automatic);
        to_->setChooserEnabled(automatic);
        arrowL_->setEnabled(automatic);
        for (QWidget *w : {static_cast<QWidget *>(factorL_), static_cast<QWidget *>(factor_), static_cast<QWidget *>(centerL_), static_cast<QWidget *>(center_)}) w->setEnabled(!automatic);
    }
    std::shared_ptr<RecodeTime> recode_;
    QRadioButton *auto_ = nullptr, *custom_ = nullptr;
    RateChooser *from_ = nullptr, *to_ = nullptr;
    QLabel *factorL_ = nullptr, *centerL_ = nullptr, *arrowL_ = nullptr;
    QLineEdit *factor_ = nullptr, *center_ = nullptr;
};

// ---- Synchronize ------------------------------------------------------------------------------

class SynchronizeGui : public TimeBaseToolGui {
public:
    explicit SynchronizeGui(std::shared_ptr<Synchronize> t) : TimeBaseToolGui(t), sync_(std::move(t)) {}

protected:
    QWidget *constructToolVisuals() override {
        auto *w = new QWidget();
        auto *lay = new QVBoxLayout(w);
        lay->setContentsMargins(0, 0, 0, 0);
        lay->addWidget(new QLabel(__("Synchronize data from the following subtitles"), w));
        model_ = new QComboBox(w);
        model_->setToolTip(__("The subtitles file to use as a model"));
        lay->addWidget(model_);
        auto *row = new QHBoxLayout();
        row->addWidget(new QLabel(__("Model subtitles offset"), w));
        offset_ = new QSpinBox(w);
        offset_->setRange(-100000, 100000);
        offset_->setToolTip(__("Relative offset of the model subtitles. It is based on index, not time."));
        row->addWidget(offset_, 1);
        lay->addLayout(row);
        time_ = new QCheckBox(__("Import timestamp"), w);
        time_->setToolTip(__("Use the timestamp of the subtitles as the time model for current subtitles"));
        time_->setChecked(true);
        text_ = new QCheckBox(__("Import text"), w);
        text_->setToolTip(__("Copy the text of the other subtitles into this file"));
        lay->addWidget(time_);
        lay->addWidget(text_);
        return w;
    }
    void updateToolData(MainWindow *window) override {
        model_->clear();
        windows_ = AppContext::windows();
        int select = 0;
        for (int i = 0; i < windows_.size(); ++i) {
            MainWindow *w = windows_[i];
            model_->addItem(windowName(w) + (w == window ? __("  -current-") : QString()));
            if (w == lastModel_) select = i;
            if (!lastModel_ && w == window) select = i;
        }
        if (!windows_.contains(lastModel_)) {
            select = std::max(0, int(windows_.indexOf(window)));
        }
        model_->setCurrentIndex(select);
    }
    bool storeSelections(MainWindow *window) override {
        const int idx = model_->currentIndex();
        MainWindow *m = idx >= 0 && idx < windows_.size() ? windows_[idx] : window;
        lastModel_ = m;
        sync_->setModel(m->getSubtitles());
        sync_->setTarget(window->getSubtitles());
        sync_->setCopy(time_->isChecked(), text_->isChecked());
        sync_->setOffset(offset_->value());
        return true;
    }

private:
    std::shared_ptr<Synchronize> sync_;
    QComboBox *model_ = nullptr;
    QSpinBox *offset_ = nullptr;
    QCheckBox *time_ = nullptr, *text_ = nullptr;
    QList<MainWindow *> windows_;
    MainWindow *lastModel_ = nullptr;
};

// ---- Fixer -------------------------------------------------------------------------------------

class FixerGui : public TimeBaseToolGui {
public:
    explicit FixerGui(std::shared_ptr<Fixer> t) : TimeBaseToolGui(t), fixer_(std::move(t)) {}

protected:
    QWidget *constructToolVisuals() override {
        auto *w = new QWidget();
        auto *lay = new QVBoxLayout(w);
        lay->setContentsMargins(0, 0, 0, 0);
        sort_ = new QCheckBox(__("Sort first  (strongly recommended)"), w);
        sort_->setChecked(true);
        prevent_ = new QCheckBox(__("Prevent overlapping"), w);
        prevent_->setChecked(true);
        model_ = new QComboBox(w);
        model_->addItems({__("Evenly distribute subtitles"), __("Equally divide overriding duration"), __("Shift subtitles")});
        model_->setToolTip(__("Model how to solve overriding subtitles"));
        gapCheck_ = new QCheckBox(__("Leave gap between subtitles (in milliseconds)"), w);
        gap_ = new QLineEdit(QStringLiteral("100"), w);
        gap_->setEnabled(false);
        lay->addWidget(sort_);
        lay->addWidget(prevent_);
        auto *mr = new QHBoxLayout();
        mr->setContentsMargins(20, 0, 0, 0);
        mr->addWidget(model_);
        lay->addLayout(mr);
        auto *gr = new QHBoxLayout();
        gr->setContentsMargins(20, 0, 0, 0);
        gr->addWidget(gapCheck_);
        gr->addWidget(gap_);
        lay->addLayout(gr);
        auto *durations = new QHBoxLayout();
        auto *minBox = new QGroupBox(__("Minimum subtitle duration"), w);
        min_ = new DurationChooser(true, minBox);
        (new QVBoxLayout(minBox))->addWidget(min_);
        auto *maxBox = new QGroupBox(__("Maximum subtitle duration"), w);
        max_ = new DurationChooser(false, maxBox);
        (new QVBoxLayout(maxBox))->addWidget(max_);
        durations->addWidget(minBox);
        durations->addWidget(maxBox);
        lay->addLayout(durations);
        QObject::connect(prevent_, &QCheckBox::toggled, w, [this](bool on) {
            model_->setEnabled(on);
            gapCheck_->setEnabled(on);
            gap_->setEnabled(on && gapCheck_->isChecked());
        });
        QObject::connect(gapCheck_, &QCheckBox::toggled, w, [this](bool on) { gap_->setEnabled(on && prevent_->isChecked()); });
        return w;
    }
    bool storeSelections(MainWindow *) override {
        double gap = 0;
        if (gapCheck_->isChecked()) {
            bool ok = false;
            gap = gap_->text().toDouble(&ok) / 1000;
            if (!ok) gap = 0;
        }
        fixer_->setSortFirst(sort_->isChecked());
        fixer_->setFix(prevent_->isChecked(), Fixer::PushModel(model_->currentIndex()), gap);
        fixer_->setDurations(min_->getAbsTime(), min_->getCPS(), max_->getAbsTime(), max_->getCPS());
        return true;
    }
    void beforeAffect(MainWindow *window, QList<SubEntryPtr> &list) override {
        if (!sort_->isChecked()) return;
        auto *region = dynamic_cast<TimeRegion *>(timeArea());
        if (!region) return;
        window->getSubtitles()->sort(region->getStartTime(), region->getFinishTime());
        // The same entries (the locked selection or the region's), now in the
        // document's sorted order.
        const QSet<const SubEntry *> affected = [&list]() {
            QSet<const SubEntry *> out;
            for (const SubEntryPtr &e : list) out.insert(e.get());
            return out;
        }();
        QList<SubEntryPtr> ordered;
        for (const SubEntryPtr &e : window->getSubtitles()->entries())
            if (affected.contains(e.get())) ordered.append(e);
        list = ordered;
    }
    void afterAffect(MainWindow *window) override {
        if (!sort_->isChecked()) return;
        if (auto *region = dynamic_cast<TimeRegion *>(timeArea()))
            window->getSubtitles()->sort(region->getStartTime(), region->getFinishTime());
    }

private:
    std::shared_ptr<Fixer> fixer_;
    QCheckBox *sort_ = nullptr, *prevent_ = nullptr, *gapCheck_ = nullptr;
    QComboBox *model_ = nullptr;
    QLineEdit *gap_ = nullptr;
    DurationChooser *min_ = nullptr, *max_ = nullptr;
};

// ---- Rounder -----------------------------------------------------------------------------------

class RounderGui : public TimeBaseToolGui {
public:
    explicit RounderGui(std::shared_ptr<Rounder> t) : TimeBaseToolGui(t), rounder_(std::move(t)) {}

protected:
    QWidget *constructToolVisuals() override {
        auto *w = new QWidget();
        auto *lay = new QHBoxLayout(w);
        lay->setContentsMargins(0, 8, 0, 0);
        lay->addWidget(new QLabel(__("Number of decimals      "), w));
        slider_ = new QSlider(Qt::Horizontal, w);
        slider_->setRange(0, 3);   // decision 04 #23: the GUI allows 3 decimals like the CLI
        slider_->setValue(2);
        slider_->setTickPosition(QSlider::TicksBelow);
        slider_->setTickInterval(1);
        slider_->setToolTip(__("Decimal digits"));
        lay->addWidget(labelledSlider(slider_, 1, w), 1);
        return w;
    }
    bool storeSelections(MainWindow *) override {
        rounder_->setDecimals(slider_->value());
        return true;
    }

private:
    std::shared_ptr<Rounder> rounder_;
    QSlider *slider_ = nullptr;
};

// ---- Marker ------------------------------------------------------------------------------------

class MarkerGui : public TimeBaseToolGui {
public:
    explicit MarkerGui(std::shared_ptr<Marker> t) : TimeBaseToolGui(t), marker_(std::move(t)) {}

protected:
    QWidget *constructToolVisuals() override {
        auto *w = new QWidget();
        w->setToolTip(__("Select the color to use in order to mark the area"));
        auto *lay = new QHBoxLayout(w);
        lay->setContentsMargins(0, 0, 0, 0);
        lay->addWidget(new QLabel(__("Color to use  "), w));
        color_ = new QComboBox(w);
        for (int i = 0; i < SubEntry::MARK_COUNT; ++i) color_->addItem(SubEntry::markName(i));
        color_->setToolTip(__("Select the mark color from the drop down list"));
        lay->addWidget(color_, 1);
        return w;
    }
    bool storeSelections(MainWindow *) override {
        marker_->setMark(color_->currentIndex());
        return true;
    }

private:
    std::shared_ptr<Marker> marker_;
    QComboBox *color_ = nullptr;
};

// ---- Delete selection ---------------------------------------------------------------------------

class DelSelectionGui : public TimeBaseToolGui {
public:
    using TimeBaseToolGui::TimeBaseToolGui;
    bool execute(MainWindow *window) override {
        const int lastrow = window->getSelectedRowIdx();
        if (!TimeBaseToolGui::execute(window)) return false;
        window->setSelectedSub(std::max(0, lastrow), true);
        return true;
    }
};

// ---- Styler -------------------------------------------------------------------------------------

class StylerGui : public TimeBaseToolGui {
public:
    explicit StylerGui(std::shared_ptr<Styler> t) : TimeBaseToolGui(t), styler_(std::move(t)) {}

protected:
    QWidget *constructToolVisuals() override {
        auto *w = new QWidget();
        auto *lay = new QHBoxLayout(w);
        lay->setContentsMargins(0, 0, 0, 0);
        lay->addWidget(new QLabel(__("Style to use  "), w));
        style_ = new QComboBox(w);
        style_->setToolTip(__("Select the desired style from the drop down list"));
        lay->addWidget(style_, 1);
        return w;
    }
    void updateToolData(MainWindow *window) override {
        const int prev = style_->currentIndex();
        style_->clear();
        if (Subtitles *subs = window->getSubtitles())
            for (const SubStylePtr &s : subs->getStyleList().all()) style_->addItem(s->getName());
        style_->setCurrentIndex(prev >= 0 && prev < style_->count() ? prev : 0);
    }
    bool storeSelections(MainWindow *window) override {
        Subtitles *subs = window->getSubtitles();
        const int idx = style_->currentIndex();
        if (!subs || idx < 0 || idx >= subs->getStyleList().size()) return false;
        styler_->setStyle(subs->getStyleList().get(idx));
        return true;
    }

private:
    std::shared_ptr<Styler> styler_;
    QComboBox *style_ = nullptr;
};

// ---- Regular expression replace -------------------------------------------------------------------

class RegExpReplaceGui : public TimeBaseToolGui {
public:
    explicit RegExpReplaceGui(std::shared_ptr<RegExpReplace> t) : TimeBaseToolGui(t), regexp_(std::move(t)) {}

protected:
    QWidget *constructToolVisuals() override {
        auto *box = new QGroupBox(__("Regular expressions to be executed"));
        auto *lay = new QHBoxLayout(box);
        list_ = new QListWidget(box);
        list_->setToolTip(__("List of replacements to be done"));
        list_->setSelectionMode(QAbstractItemView::NoSelection);
        list_->setMinimumSize(259, 80);
        lay->addWidget(list_, 1);
        auto *edit = new QPushButton(__("Edit"), box);
        lay->addWidget(edit, 0, Qt::AlignTop);
        model_ = new ReplaceModel(box);
        QObject::connect(edit, &QPushButton::clicked, box, [this, box]() {
            QDialog dlg(box->window());
            dlg.setWindowTitle(__("Edit regular expression replace list"));
            auto *dl = new QVBoxLayout(&dlg);
            dl->addWidget(new ReplaceListWidget(model_, &dlg), 1);
            auto *buttons = new QDialogButtonBox(&dlg);
            QPushButton *use = buttons->addButton(__("Use"), QDialogButtonBox::AcceptRole);
            QPushButton *cancel = buttons->addButton(__("Cancel"), QDialogButtonBox::RejectRole);
            QPushButton *reset = buttons->addButton(__("Reset"), QDialogButtonBox::ResetRole);
            cancel->setDefault(true);
            QObject::connect(use, &QPushButton::clicked, &dlg, &QDialog::accept);
            QObject::connect(cancel, &QPushButton::clicked, &dlg, &QDialog::reject);
            QObject::connect(reset, &QPushButton::clicked, &dlg, [this, &dlg]() { model_->reset(); model_->saveOptions(); dlg.done(2); });
            dl->addWidget(buttons);
            const int r = dlg.exec();
            if (r == QDialog::Accepted) model_->saveOptions();
            else if (r == QDialog::Rejected) model_->loadOptions();
            refreshList();
        });
        refreshList();
        return box;
    }
    bool storeSelections(MainWindow *window) override {
        QList<QPair<QString, QString>> rules;
        for (const ReplaceEntry &e : model_->entries())
            if (e.usable) rules.append({e.getPattern(), e.getReplacement()});
        QString error;
        if (!regexp_->setRules(rules, &error)) {
            QMessageBox::critical(window, tool_->getToolTitle(), error);
            return false;
        }
        return true;
    }

private:
    void refreshList() {
        list_->clear();
        for (const ReplaceEntry &e : model_->entries()) {
            const QString t = e.getTransformation();
            if (!t.isNull()) list_->addItem(t);
        }
    }
    std::shared_ptr<RegExpReplace> regexp_;
    QListWidget *list_ = nullptr;
    ReplaceModel *model_ = nullptr;
};

// ---- Spell check --------------------------------------------------------------------------------

class SpellerGui : public TimeBaseToolGui {
public:
    explicit SpellerGui(std::shared_ptr<Speller> t) : TimeBaseToolGui(t), speller_(std::move(t)) {}

protected:
    QWidget *constructToolVisuals() override {
        bar_ = new SpellerBar();
        return bar_;
    }
    void updateToolData(MainWindow *) override { bar_->refresh(); }
    bool storeSelections(MainWindow *window) override {
        speller_->setRunner([window](SpellChecker &checker, QList<SubEntryPtr> &list) {
            // The dialog keeps the checker alive through a non-owning pointer.
            std::shared_ptr<SpellChecker> keep(&checker, [](SpellChecker *) {});
            SpellDialog dlg(window, keep, list);
            return dlg.run();
        });
        return true;
    }

private:
    std::shared_ptr<Speller> speller_;
    SpellerBar *bar_ = nullptr;
};

// ---- Translate ----------------------------------------------------------------------------------

class TranslateGui : public TimeBaseToolGui {
public:
    explicit TranslateGui(std::shared_ptr<Translate> t) : TimeBaseToolGui(t), translate_(std::move(t)) {}

protected:
    QWidget *constructToolVisuals() override {
        panel_ = new TranslatePanel();
        return panel_;
    }
    bool storeSelections(MainWindow *window) override {
        translate_->setSelection(panel_->translator(), panel_->from(), panel_->to());
        translate_->setRunner([window](Translator &t, QList<SubEntryPtr> &list, const Language &from, const Language &to) {
            return TranslateUi::run(window, t, list, from, to);
        });
        return true;
    }

private:
    std::shared_ptr<Translate> translate_;
    TranslatePanel *panel_ = nullptr;
};

// ---- Split file --------------------------------------------------------------------------------

class SubSplitGui : public ToolGui {
public:
    explicit SubSplitGui(std::shared_ptr<SubSplit> t) : tool_(std::move(t)) {}
    Tool *tool() const override { return tool_.get(); }
    bool execute(MainWindow *window) override {
        Subtitles *subs = window->getSubtitles();
        if (!subs || subs->isEmpty()) return false;
        QDialog dlg(window);
        dlg.setWindowTitle(tool_->getToolTitle());
        auto *lay = new QVBoxLayout(&dlg);
        lay->addWidget(new QLabel(__("Splitting time"), &dlg));
        auto *spinner = new TimeSpinner(&dlg);
        spinner->setToolTip(__("Use the following time (inclusive) in order to create the new splitted subtitles"));
        lay->addWidget(spinner);
        lay->addSpacing(16);
        lay->addWidget(new QLabel(__("Split at given subtitle line (in percent)"), &dlg));
        auto *slider = new QSlider(Qt::Horizontal, &dlg);
        slider->setRange(0, 100);
        slider->setTickInterval(10);
        slider->setTickPosition(QSlider::TicksBelow);
        slider->setToolTip(__("Percentage of subtitles to divide"));
        lay->addWidget(labelledSlider(slider, 10, &dlg));
        const int row = std::max(0, window->getSelectedRowIdx());
        slider->setValue(100 * row / subs->size());
        spinner->setTimeValue(subs->get(row)->getStartTime());
        QObject::connect(slider, &QSlider::valueChanged, &dlg, [spinner, subs](int v) {
            const int idx = std::clamp(v * (subs->size() - 1) / 100, 0, subs->size() - 1);
            spinner->setTimeValue(subs->get(idx)->getStartTime());
        });
        auto *buttons = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel, &dlg);
        QObject::connect(buttons, &QDialogButtonBox::accepted, &dlg, &QDialog::accept);
        QObject::connect(buttons, &QDialogButtonBox::rejected, &dlg, &QDialog::reject);
        lay->addWidget(buttons);
        if (dlg.exec() != QDialog::Accepted) return false;

        window->getUndoList()->addUndo(*subs, __("Split subtitles"));
        auto first = std::make_unique<Subtitles>();
        auto second = std::make_unique<Subtitles>();
        SubSplit::split(*subs, spinner->seconds(), *first, *second);
        window->setSubs(std::move(first));
        window->getUndoList()->invalidateSaveMark();
        window->enableWindowControls(false);
        window->showInfo();
        auto *other = new MainWindow(std::move(second));
        other->getUndoList()->invalidateSaveMark();
        other->enableWindowControls(true);
        other->showInfo();
        AppContext::updateRecents();
        return true;
    }

private:
    std::shared_ptr<SubSplit> tool_;
};

// ---- Join files --------------------------------------------------------------------------------

class SubJoinGui : public ToolGui {
public:
    explicit SubJoinGui(std::shared_ptr<SubJoin> t) : tool_(std::move(t)) {}
    Tool *tool() const override { return tool_.get(); }
    bool isAvailable(MainWindow *) const override { return AppContext::windows().size() > 1; }
    bool execute(MainWindow *window) override {
        Subtitles *subs = window->getSubtitles();
        if (!subs) return false;
        QList<MainWindow *> others;
        for (MainWindow *w : AppContext::windows())
            if (w != window && w->getSubtitles()) others.append(w);
        if (others.isEmpty()) return false;
        ensurePanel();
        combo_->clear();
        for (MainWindow *w : others) combo_->addItem(windowName(w));
        if (!runDialog(window, tool_->getToolTitle(), panel_)) return false;
        MainWindow *other = others[std::max(0, combo_->currentIndex())];
        window->getUndoList()->addUndo(*subs, __("Join subtitles"));
        auto joined = std::make_unique<Subtitles>(subs->getSubFile());
        SubEntryPtr firstAppended = append_->isChecked() ? joined->joinSubs(*subs, *other->getSubtitles(), gap_->seconds())
                                                         : joined->joinSubs(*other->getSubtitles(), *subs, gap_->seconds());
        window->setSubs(std::move(joined));
        window->tableHasChanged(QList<SubEntryPtr>{firstAppended});
        other->closeWindow(false, true);
        return true;
    }

private:
    // The panel persists between runs (the Prepend/Append choice and the gap
    // are remembered within the session).
    void ensurePanel() {
        if (panel_) return;
        panel_ = new QWidget();
        auto *lay = new QVBoxLayout(panel_);
        lay->setContentsMargins(0, 0, 0, 0);
        auto *box = new QGroupBox(__("Use the following subtitles"), panel_);
        auto *bl = new QVBoxLayout(box);
        combo_ = new QComboBox(box);
        combo_->setToolTip(__("Subtitles file to use"));
        prepend_ = new QRadioButton(__("Prepend subtitles"), box);
        prepend_->setToolTip(__("Put subtitles in the beginning of the current subtitles"));
        append_ = new QRadioButton(__("Append Subtitles"), box);
        append_->setToolTip(__("Put subtitles at the end of the current subtitles"));
        append_->setChecked(true);
        bl->addWidget(combo_);
        bl->addWidget(prepend_);
        bl->addWidget(append_);
        lay->addWidget(box);
        auto *gapBox = new QGroupBox(__("Leave gap"), panel_);
        auto *gl = new QVBoxLayout(gapBox);
        gap_ = new TimeSpinner(gapBox);
        gap_->setToolTip(__("Use the selected amount of time as space between the two subtitles"));
        gl->addWidget(gap_);
        lay->addWidget(gapBox);
    }
    std::shared_ptr<SubJoin> tool_;
    QWidget *panel_ = nullptr;
    QComboBox *combo_ = nullptr;
    QRadioButton *prepend_ = nullptr, *append_ = nullptr;
    TimeSpinner *gap_ = nullptr;
};

// ---- Reparent ----------------------------------------------------------------------------------

class ReparentGui : public ToolGui {
public:
    explicit ReparentGui(std::shared_ptr<Reparent> t) : tool_(std::move(t)) {}
    Tool *tool() const override { return tool_.get(); }
    bool isAvailable(MainWindow *) const override { return AppContext::windows().size() > 1; }
    bool execute(MainWindow *window) override {
        QDialog dlg(window);
        dlg.setWindowTitle(tool_->getToolTitle());
        auto *lay = new QVBoxLayout(&dlg);
        lay->addWidget(new QLabel(__("Provide the desired parent for this subtitles file"), &dlg));
        auto *combo = new QComboBox(&dlg);
        combo->setToolTip(__("The new parent subtitles file"));
        combo->addItem(__("-No parent available-"));
        QList<MainWindow *> others;
        for (MainWindow *w : AppContext::windows()) {
            if (w == window) continue;
            others.append(w);
            combo->addItem(windowName(w));
            if (w == window->jparent()) combo->setCurrentIndex(others.size());
        }
        lay->addWidget(combo);
        auto *buttons = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel, &dlg);
        QObject::connect(buttons, &QDialogButtonBox::accepted, &dlg, &QDialog::accept);
        QObject::connect(buttons, &QDialogButtonBox::rejected, &dlg, &QDialog::reject);
        lay->addWidget(buttons);
        if (dlg.exec() != QDialog::Accepted) return false;
        const int idx = combo->currentIndex();
        if (idx <= 0) {
            window->setJParent(nullptr);
            return false;
        }
        MainWindow *chosen = others[idx - 1];
        for (MainWindow *p = chosen; p; p = p->jparent())
            if (p == window) {
                QMessageBox::critical(window, __("Reparent error"), __("Cyclic dependency while setting new parent.\nParenting will be cancelled"));
                return false;
            }
        window->setJParent(chosen);
        return true;
    }

private:
    std::shared_ptr<Reparent> tool_;
};

// ---- Download subtitles ----------------------------------------------------------------------

class SubDownloadGui : public ToolGui {
public:
    explicit SubDownloadGui(std::shared_ptr<SubDownloadTool> t) : tool_(std::move(t)) {}
    Tool *tool() const override { return tool_.get(); }
    bool isAvailable(MainWindow *w) const override {
        const AppMediaFile *m = w->getMediaFile();
        return m && m->getVideoFile() && m->getVideoFile()->exists();
    }
    bool execute(MainWindow *window) override {
        if (!isAvailable(window)) {
            QMessageBox::information(window, tool_->getToolTitle(), __("Attach a video to this document first, then search for its subtitles."));
            return false;
        }
        SubDownloadWindow::open(window);
        return true;
    }

private:
    std::shared_ptr<SubDownloadTool> tool_;
};

// ---- registry ----------------------------------------------------------------------------------

struct Registry {
    QList<std::shared_ptr<ToolGui>> guis;
    std::shared_ptr<ShiftTimeGui> shifter;
    std::shared_ptr<RecodeTimeGui> recoder;
    std::shared_ptr<RegExpReplaceGui> regexp;
};

Registry &registry() {
    static Registry r;
    static bool built = false;
    if (built) return r;
    built = true;
    ToolRegistry &reg = ToolRegistry::instance();
    if (reg.tools().isEmpty()) registerBuiltinTools(reg);
    SubtitleProviders::registerBuiltin();
    if (!SubtitleProviders::all().isEmpty() && !reg.find<SubDownloadTool>()) reg.add(std::make_shared<SubDownloadTool>());
    for (const auto &t : reg.tools()) {
        std::shared_ptr<ToolGui> gui;
        if (auto p = std::dynamic_pointer_cast<ShiftTime>(t)) { r.shifter = std::make_shared<ShiftTimeGui>(p); gui = r.shifter; }
        else if (auto p = std::dynamic_pointer_cast<RecodeTime>(t)) { r.recoder = std::make_shared<RecodeTimeGui>(p); gui = r.recoder; }
        else if (auto p = std::dynamic_pointer_cast<Synchronize>(t)) gui = std::make_shared<SynchronizeGui>(p);
        else if (auto p = std::dynamic_pointer_cast<Fixer>(t)) gui = std::make_shared<FixerGui>(p);
        else if (auto p = std::dynamic_pointer_cast<Rounder>(t)) gui = std::make_shared<RounderGui>(p);
        else if (auto p = std::dynamic_pointer_cast<Marker>(t)) gui = std::make_shared<MarkerGui>(p);
        else if (auto p = std::dynamic_pointer_cast<DelSelection>(t)) gui = std::make_shared<DelSelectionGui>(p);
        else if (auto p = std::dynamic_pointer_cast<Styler>(t)) gui = std::make_shared<StylerGui>(p);
        else if (auto p = std::dynamic_pointer_cast<Speller>(t)) gui = std::make_shared<SpellerGui>(p);
        else if (auto p = std::dynamic_pointer_cast<Translate>(t)) gui = std::make_shared<TranslateGui>(p);
        else if (auto p = std::dynamic_pointer_cast<JoinEntries>(t)) gui = std::make_shared<TimeBaseToolGui>(p);
        else if (auto p = std::dynamic_pointer_cast<SplitEntries>(t)) gui = std::make_shared<TimeBaseToolGui>(p);
        else if (auto p = std::dynamic_pointer_cast<RegExpReplace>(t)) { r.regexp = std::make_shared<RegExpReplaceGui>(p); gui = r.regexp; }
        else if (auto p = std::dynamic_pointer_cast<SubSplit>(t)) gui = std::make_shared<SubSplitGui>(p);
        else if (auto p = std::dynamic_pointer_cast<SubJoin>(t)) gui = std::make_shared<SubJoinGui>(p);
        else if (auto p = std::dynamic_pointer_cast<Reparent>(t)) gui = std::make_shared<ReparentGui>(p);
        else if (auto p = std::dynamic_pointer_cast<SubDownloadTool>(t)) gui = std::make_shared<SubDownloadGui>(p);
        if (gui) r.guis.append(gui);
    }
    return r;
}

void run(MainWindow *window, const std::shared_ptr<ToolGui> &gui) {
    gui->updateData(window);
    gui->execute(window);
}

}  // namespace

namespace ToolRunner {

void registerMenus(MainWindow *window, QMenu *tools, QMenu *deleteMenu, QMenu *markMenu, QMenu *styleMenu) {
    Registry &r = registry();
    // The Tools menu: FILETOOL | TIMETOOL | CONTENTTOOL, then the existing
    // Preview/Externals sub-menus.
    const QList<QAction *> existing = tools->actions();
    QAction *before = existing.isEmpty() ? nullptr : existing.first();
    auto add = [&](QMenu *menu, QAction *insertBefore, const std::shared_ptr<ToolGui> &gui) {
        const ToolMenu &m = *gui->tool()->menu();
        auto *a = new QAction(m.text, menu);
        a->setObjectName(m.name);
        if (m.key != 0) a->setShortcut(QKeySequence(m.key | m.modifiers));
        a->setEnabled(false);
        QObject::connect(a, &QAction::triggered, window, [window, gui]() { run(window, gui); });
        if (insertBefore) menu->insertAction(insertBefore, a); else menu->addAction(a);
        return a;
    };
    for (ToolLocation loc : {ToolLocation::FILETOOL, ToolLocation::TIMETOOL, ToolLocation::CONTENTTOOL}) {
        bool any = false;
        for (const auto &gui : r.guis) {
            if (!gui->tool()->menu() || gui->tool()->menu()->location != loc) continue;
            add(tools, before, gui);
            any = true;
        }
        if (any) {
            auto *sep = new QAction(tools);
            sep->setSeparator(true);
            if (before) tools->insertAction(before, sep); else tools->addAction(sep);
        }
    }
    for (const auto &gui : r.guis) {
        if (!gui->tool()->menu()) continue;
        switch (gui->tool()->menu()->location) {
            case ToolLocation::DELETE: add(deleteMenu, nullptr, gui); break;
            case ToolLocation::MARK: add(markMenu, nullptr, gui); break;
            case ToolLocation::STYLE: add(styleMenu, nullptr, gui); break;
            default: break;
        }
    }
    updateToolsAvailability(window, tools);
}

void updateToolsAvailability(MainWindow *window, QMenu *tools) {
    Registry &r = registry();
    const bool hasDoc = window->getSubtitles() != nullptr;
    for (const auto &gui : r.guis) {
        if (!gui->tool()->menu()) continue;
        // The Delete/Mark/Style items live in the Edit sub-menus, so search the window.
        if (auto *a = window->findChild<QAction *>(gui->tool()->menu()->name))
            a->setEnabled(hasDoc && gui->isAvailable(window));
    }
}

void runRegExpReplace(MainWindow *window) {
    Registry &r = registry();
    if (r.regexp) run(window, r.regexp);
}

bool runSync(MainWindow *window, const TimeSync &first, const TimeSync &second) {
    Registry &r = registry();
    std::shared_ptr<ToolGui> gui;
    if (first.isEqualDiff(second)) {
        if (!r.shifter || !r.shifter->setValues(first, second)) return false;
        gui = r.shifter;
    } else {
        if (!r.recoder || !r.recoder->setValues(first, second)) return false;
        gui = r.recoder;
    }
    run(window, gui);
    return true;
}

bool runTool(MainWindow *window, const QString &menuName) {
    for (const auto &gui : registry().guis)
        if (gui->tool()->menu() && gui->tool()->menu()->name == menuName) {
            run(window, gui);
            return true;
        }
    return false;
}

}  // namespace ToolRunner
