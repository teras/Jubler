/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/options/PreferencesDialog.h"

#include <optional>

#include <QPointer>
#include <QApplication>
#include <QAction>
#include <QButtonGroup>
#include <QCheckBox>
#include <QComboBox>
#include <QDialogButtonBox>
#include <QFileDialog>
#include <QFormLayout>
#include <QHBoxLayout>
#include <QHeaderView>
#include <QKeyEvent>
#include <QDir>
#include <QEventLoop>
#include <QLabel>
#include <QLineEdit>
#include <QListWidget>
#include <QMenu>
#include <QMenuBar>
#include <QMessageBox>
#include <QPushButton>
#include <QScreen>
#include <QRadioButton>
#include <QRegularExpression>
#include <QStackedWidget>
#include <QTableWidget>
#include <QToolBar>
#include <QToolButton>
#include <QVBoxLayout>
#include <cmath>

#include "app/AppContext.h"
#include "app/externals/RecipeDialogs.h"
#include "app/subdownload/PluginRegistry.h"
#include "app/update/AutoUpdater.h"
#include <QScrollArea>
#include "core/externals/Recipe.h"
#include <QDesktopServices>
#include <QUrl>
#include <QSet>
#include <QtConcurrent>
#include <QFutureWatcher>
#include <algorithm>
#include "app/Theme.h"
#include "app/ui/MainWindow.h"
#include "core/i18n/I18N.h"
#include "core/options/JavaPrefs.h"
#include "core/options/Options.h"
#include "core/options/Prefs.h"
#include "core/plugins/PluginHost.h"
#include "core/os/Debug.h"
#include "core/os/FileCommunicator.h"
#include "core/os/SystemDependent.h"

QIcon OptionsPage::pageIcon() const {
    return Theme::icon(iconName());
}

namespace {

const QString SHORTCUT_KEY = QStringLiteral("shortcut.keys");
const char *DEFAULT_PROP = "jublerDefaultShortcut";

// ---- Shortcuts page ------------------------------------------------------------------------------

// A row of the shortcuts table: a command (tag) with its default and current
// key, or a separator between top-level menus. Port of `ShortcutsModel`.
struct ShortcutRow {
    QString tag, command;
    QKeySequence deflt, current;
    bool separator = false;
};

class ShortcutsPage : public OptionsPage {
public:
    void setMenuBar(QMenuBar *bar) { bar_ = bar; }
    explicit ShortcutsPage(QMenuBar *bar, QWidget *parent = nullptr) : OptionsPage(parent), bar_(bar) {
        auto *lay = new QVBoxLayout(this);
        table_ = new QTableWidget(0, 2, this);
        table_->setHorizontalHeaderLabels({__("Command"), __("Key")});
        table_->horizontalHeader()->setStretchLastSection(true);
        table_->verticalHeader()->hide();
        table_->setTabKeyNavigation(false);
        table_->setSelectionMode(QAbstractItemView::SingleSelection);
        table_->setSelectionBehavior(QAbstractItemView::SelectRows);
        table_->setEditTriggers(QAbstractItemView::NoEditTriggers);
        table_->setMinimumSize(200, 200);
        table_->installEventFilter(this);
        lay->addWidget(table_, 1);
        auto *buttons = new QHBoxLayout();
        auto *reset = new QPushButton(__("Reset all to defaults"), this);
        auto *clear = new QPushButton(__("Clear current shortcut"), this);
        buttons->addWidget(reset);
        buttons->addStretch(1);
        buttons->addWidget(clear);
        lay->addLayout(buttons);
        connect(reset, &QPushButton::clicked, this, [this]() {
            for (ShortcutRow &r : rows_) r.current = r.deflt;
            refresh();
        });
        connect(clear, &QPushButton::clicked, this, [this]() {
            const int row = table_->currentRow();
            if (row >= 0 && row < rows_.size() && !rows_[row].separator) {
                rows_[row].current = QKeySequence();
                refresh();
            }
        });
    }
    QString pageName() const override { return __("Shortcuts"); }
    QString iconName() const override { return QStringLiteral("shortcut_pref"); }
    QString pageTooltip() const override { return __("Set the menu keyboard shortcuts"); }

    void loadPreferences() override {
        rows_.clear();
        const QMap<QString, QString> overrides = PreferencesDialog::loadShortcutOverrides();
        bool first = true;
        if (!bar_) return;
        for (QAction *top : bar_->actions()) {
            QMenu *menu = top->menu();
            if (!menu) continue;
            if (!first) {
                ShortcutRow sep;
                sep.separator = true;
                sep.command = QStringLiteral("---");
                rows_.append(sep);
            }
            first = false;
            walk(menu, QString(), overrides);
        }
        // The Java isValidCodes check: the first duplicate or too short tag.
        QSet<QString> seen;
        for (const ShortcutRow &r : std::as_const(rows_)) {
            if (r.separator) continue;
            QString problem;
            if (seen.contains(r.tag)) problem = r.tag;
            else if (r.tag.length() < 3) problem = QStringLiteral("Tag too short: ") + r.tag;
            seen.insert(r.tag);
            if (!problem.isEmpty()) {
                Debug::debug(QStringLiteral("Error in shortcut entry:") + problem);
                break;
            }
        }
        refresh();
        table_->setCurrentCell(-1, -1);
    }

    void savePreferences() override {
        QMap<QString, QString> overrides;
        for (const ShortcutRow &r : rows_) {
            if (r.separator || r.current == r.deflt) continue;
            overrides.insert(r.tag, r.current.toString(QKeySequence::PortableText));
        }
        PreferencesDialog::saveShortcutOverrides(overrides);
        AppContext::updateAllMenus();
    }

protected:
    bool eventFilter(QObject *obj, QEvent *e) override {
        if (obj == table_ && e->type() == QEvent::KeyPress) {
            auto *ke = static_cast<QKeyEvent *>(e);
            const int key = ke->key();
            if (key == Qt::Key_Control || key == Qt::Key_Shift || key == Qt::Key_Alt || key == Qt::Key_Meta || key == Qt::Key_CapsLock || key == Qt::Key_AltGr || key == Qt::Key_unknown)
                return false;
            // Tab / Shift+Tab move the focus (Java focus traversal keys).
            if ((key == Qt::Key_Tab || key == Qt::Key_Backtab) && !(ke->modifiers() & (Qt::ControlModifier | Qt::AltModifier | Qt::MetaModifier)))
                return false;
            const int row = table_->currentRow();
            if (row < 0 || row >= rows_.size() || rows_[row].separator) return false;
            rows_[row].current = QKeySequence(key | int(ke->modifiers() & (Qt::ControlModifier | Qt::ShiftModifier | Qt::AltModifier | Qt::MetaModifier)));
            refresh();
            table_->selectRow(row);
            return true;
        }
        return OptionsPage::eventFilter(obj, e);
    }

private:
    void walk(QMenu *menu, const QString &prefix, const QMap<QString, QString> &overrides) {
        for (QAction *a : menu->actions()) {
            if (a->isSeparator()) continue;
            if (QMenu *sub = a->menu()) {
                walk(sub, prefix + a->text().remove(QLatin1Char('&')) + QLatin1Char(' '), overrides);
                continue;
            }
            const QString tag = a->objectName();
            if (tag.isEmpty()) {
                Debug::debug(QStringLiteral("Menu item \"%1\" does not provide a valid name").arg(prefix + a->text().remove(QLatin1Char('&'))));
                continue;
            }
            if (tag.startsWith(QLatin1String("ignore"), Qt::CaseInsensitive)) continue;
            ShortcutRow r;
            r.tag = tag;
            r.command = prefix + a->text().remove(QLatin1Char('&'));
            const QVariant d = a->property(DEFAULT_PROP);
            r.deflt = d.isValid() ? QKeySequence::fromString(d.toString(), QKeySequence::PortableText) : a->shortcut();
            r.current = overrides.contains(tag) ? QKeySequence::fromString(overrides.value(tag), QKeySequence::PortableText) : r.deflt;
            rows_.append(r);
        }
    }
    void refresh() {
        table_->setRowCount(rows_.size());
        for (int i = 0; i < rows_.size(); ++i) {
            auto *c = new QTableWidgetItem(rows_[i].command);
            auto *k = new QTableWidgetItem(rows_[i].separator ? QString() : rows_[i].current.toString(QKeySequence::NativeText));
            if (rows_[i].separator) {
                c->setFlags(Qt::NoItemFlags);
                k->setFlags(Qt::NoItemFlags);
            }
            table_->setItem(i, 0, c);
            table_->setItem(i, 1, k);
        }
        table_->resizeColumnToContents(0);
    }
    QPointer<QMenuBar> bar_;
    QTableWidget *table_;
    QList<ShortcutRow> rows_;
};

// ---- plugin pages ---------------------------------------------------------------------------------

// A page a plugin contributed (its widget fills the page).
class PluginPage : public OptionsPage {
public:
    explicit PluginPage(std::shared_ptr<jubler::PreferencesPage> page, QWidget *parent = nullptr) : OptionsPage(parent), page_(std::move(page)) {
        auto *lay = new QVBoxLayout(this);
        lay->setContentsMargins(0, 0, 0, 0);
        QWidget *w = nullptr;
        try {
            w = page_->createWidget(this);
        } catch (...) {
            Debug::debug(QStringLiteral("A plugin Preferences page failed: ") + page_->title());
        }
        if (w) lay->addWidget(w);
    }
    QString pageName() const override { return page_->title(); }
    QString iconName() const override { return QStringLiteral("plugins"); }
    QIcon pageIcon() const override {
        const QIcon i = page_->icon();
        return i.isNull() ? OptionsPage::pageIcon() : i;
    }
    QString pageTooltip() const override { return page_->tooltip(); }
    void loadPreferences() override {
        try { page_->load(); } catch (...) { Debug::debug(QStringLiteral("A plugin Preferences page failed: ") + page_->title()); }
    }
    void savePreferences() override {
        try { page_->save(); } catch (...) { Debug::debug(QStringLiteral("A plugin Preferences page failed: ") + page_->title()); }
    }

private:
    std::shared_ptr<jubler::PreferencesPage> page_;
};

// ---- Preview page ---------------------------------------------------------------------------------

class PreviewPage : public OptionsPage {
public:
    explicit PreviewPage(QWidget *parent = nullptr) : OptionsPage(parent) {
        auto *lay = new QFormLayout(this);
        hardware_ = new QCheckBox(__("Use hardware acceleration for the video preview"), this);
        lay->addRow(hardware_);
    }
    QString pageName() const override { return __("Preview"); }
    QString iconName() const override { return QStringLiteral("waveform"); }
    QString pageTooltip() const override { return __("Configure audio/video preview parameters"); }
    void loadPreferences() override { hardware_->setChecked(Options::isVideoPreviewHardware()); }
    void savePreferences() override { Options::setVideoPreviewHardware(hardware_->isChecked()); }

private:
    QCheckBox *hardware_;
};

// ---- UI options page ------------------------------------------------------------------------------

class UiOptionsPage : public OptionsPage {
public:
    explicit UiOptionsPage(QWidget *parent = nullptr) : OptionsPage(parent) {
        auto *lay = new QVBoxLayout(this);
        lay->setContentsMargins(0, 8, 0, 0);
        if (SystemDependent::shouldSupportChangeScaling()) {
            lay->addWidget(new QLabel(__("Scaling factor"), this));
            scaling_ = new QLineEdit(this);
            lay->addWidget(scaling_);
            lay->addSpacing(16);
        }
        tooltips_ = new QCheckBox(__("Disable timestamp tooltips"), this);
        lay->addWidget(tooltips_);
        lay->addSpacing(16);
        updates_ = new QCheckBox(__("Check for new versions at startup"), this);
        lay->addWidget(updates_);
        lay->addSpacing(16);
        auto *tr = new QHBoxLayout();
        tr->addWidget(new QLabel(__("Theme variation"), this));
        theme_ = new QComboBox(this);
        theme_->addItems({__("Auto"), __("Light"), __("Dark")});
        tr->addWidget(theme_, 1);
        lay->addLayout(tr);
        lay->addSpacing(16);
        auto *lr = new QHBoxLayout();
        lr->addWidget(new QLabel(__("Language"), this));
        language_ = new QComboBox(this);
        struct Lang { const char *code; const char *name; };
        const Lang langs[] = {{"auto", "Automatic"}, {"cs", "Čeština"}, {"de", "Deutsch"}, {"el", "Ελληνικά"}, {"en", "English"}, {"es", "Español"},
                              {"fr", "Français"}, {"it", "Italiano"}, {"nl", "Nederlands"}, {"pt", "Português"}, {"sr", "Српски"}, {"tr", "Türkçe"}};
        for (const Lang &l : langs) {
            const QString code = QString::fromLatin1(l.code);
            const QString name = code == QLatin1String("auto") ? __("Automatic") : QString::fromUtf8(l.name);
            language_->addItem(Theme::icon(QStringLiteral("flag-") + (code == QLatin1String("auto") ? QStringLiteral("global") : code)), name, code);
            if (code == QLatin1String("auto")) language_->insertSeparator(1);
        }
        language_->setIconSize(Theme::naturalSize(QStringLiteral("flag-global")));   // the flags' own 20 × 15
        lr->addWidget(language_, 1);
        lay->addLayout(lr);
        lay->addStretch(1);
    }
    QString pageName() const override { return __("UI Options"); }
    QString iconName() const override { return QStringLiteral("uioptions"); }
    QString pageTooltip() const override { return __("Configure UI related parameters"); }
    void loadPreferences() override {
        oldScaling_ = std::round(Options::getScaling() * 100) / 100.0f;
        if (scaling_) scaling_->setText(Prefs::floatString(oldScaling_));   // "1.0" as the Java
        tooltips_->setChecked(Options::isTimestampTooltipsDisabled());
        updates_->setChecked(AutoUpdater::isEnabled());
        theme_->setCurrentIndex(int(Options::getThemeVariation()));
        const int idx = language_->findData(Options::getLanguage());
        language_->setCurrentIndex(idx >= 0 ? idx : 0);
    }
    void savePreferences() override {
        bool restart = false;
        if (scaling_) {
            bool ok = false;
            const float v = scaling_->text().toFloat(&ok);
            if (ok && std::abs(v - oldScaling_) >= 0.005f) {
                Options::setScaling(v);
                if (std::abs(v - oldScaling_) > 0.1f) restart = true;
            }
        }
        if (tooltips_->isChecked() != Options::isTimestampTooltipsDisabled()) {
            Options::setTimestampTooltipsDisabled(tooltips_->isChecked());
            restart = true;
        }
        AutoUpdater::setEnabled(updates_->isChecked());
        if (ThemeVariation(theme_->currentIndex()) != Options::getThemeVariation()) {
            Options::setThemeVariation(ThemeVariation(theme_->currentIndex()));
            restart = true;
        }
        const QString lang = language_->currentData().toString();
        if (!lang.isEmpty() && lang != Options::getLanguage()) {
            Options::setLanguage(lang);
            restart = true;
        }
        if (restart) QMessageBox::information(this, __("UI Options"), __("New UI elements will be performed after you restart Jubler"));
    }

private:
    QLineEdit *scaling_ = nullptr;
    QCheckBox *tooltips_, *updates_;
    QComboBox *theme_, *language_;
    float oldScaling_ = 1;
};

// ---- Externals page ---------------------------------------------------------------------------------

class ExternalsPage : public OptionsPage {
public:
    explicit ExternalsPage(QWidget *parent = nullptr) : OptionsPage(parent) {
        auto *lay = new QVBoxLayout(this);
        list_ = new QListWidget(this);
        lay->addWidget(list_, 1);
        auto *row = new QHBoxLayout();
        auto tool = [&](const QString &icon, const QString &tip) {
            auto *b = new QToolButton(this);
            b->setIcon(Theme::icon(icon));
            b->setToolTip(tip);
            b->setIconSize(QSize(28, 28));
            row->addWidget(b);
            return b;
        };
        add_ = tool(QStringLiteral("plus"), __("Add"));
        remove_ = tool(QStringLiteral("minus"), __("Remove"));
        globe_ = tool(QStringLiteral("flag-global"), __("Open the recipe's web page"));
        globe_->setIconSize(Theme::naturalSize(QStringLiteral("flag-global"), 28.0 / Theme::naturalSize(QStringLiteral("flag-global")).height()));   // 28 px high
        edit_ = tool(QStringLiteral("edit"), __("Edit…"));
        row->addStretch(1);
        auto text = [&](const QString &label, const QString &tip) {
            auto *b = new QPushButton(label, this);
            b->setToolTip(tip);
            row->addWidget(b);
            return b;
        };
        auto *pin = text(__("Change PIN…"), __("Change the PIN that protects secret values"));
        auto *fetch = text(__("Fetch online…"), __("Download the latest shared recipes"));
        auto *load = text(__("Load…"), QString());
        auto *save = text(__("Save…"), QString());
        lay->addLayout(row);

        connect(list_, &QListWidget::currentRowChanged, this, [this]() { updateButtons(); });
        connect(list_, &QListWidget::itemDoubleClicked, this, [this]() { editSelected(); });
        connect(add_, &QToolButton::clicked, this, [this]() {
            Recipe r(__("New recipe"));
            Recipes::getList().append(r);
            refresh(Recipes::getList().size() - 1);
            RecipeEditorDialog dlg(window(), Recipes::getList().last());
            dlg.exec();
            if (!dlg.accepted()) {
                Recipes::getList().removeLast();
                refresh(-1);
            } else
                refresh(Recipes::getList().size() - 1);
        });
        connect(edit_, &QToolButton::clicked, this, [this]() { editSelected(); });
        connect(remove_, &QToolButton::clicked, this, [this]() {
            const int row = list_->currentRow();
            if (row < 0) return;
            Recipes::getList().removeAt(row);
            refresh(Recipes::getList().isEmpty() ? -1 : std::min(row, int(Recipes::getList().size()) - 1));
        });
        connect(globe_, &QToolButton::clicked, this, [this]() {
            const int row = list_->currentRow();
            if (row >= 0 && row < Recipes::getList().size()) QDesktopServices::openUrl(QUrl(Recipes::getList()[row].url));
        });
        connect(pin, &QPushButton::clicked, this, [this]() {
            if (RecipeSecrets::changePin()) QMessageBox::information(this, __("Change PIN"), __("PIN changed."));
        });
        connect(load, &QPushButton::clicked, this, [this]() {
            const QString f = QFileDialog::getOpenFileName(this, __("Load recipe"), FileCommunicator::getDefaultDirPath(), __("Recipe files (*.json)"));
            if (f.isEmpty()) return;
            QString err;
            const QList<Recipe> loaded = Recipes::loadFromFile(f, &err);
            if (loaded.isEmpty()) {
                // A readable file with no recipes adds nothing, silently (Java).
                if (!err.isEmpty()) QMessageBox::critical(this, __("Load recipe"), __("Could not load recipe: {0}", err));
                return;
            }
            appendRecipes(loaded, __("Load recipe"));
        });
        connect(save, &QPushButton::clicked, this, [this]() {
            const int row = list_->currentRow();
            if (row < 0) {
                QMessageBox::information(this, __("Save recipe"), __("Select a recipe to save."));
                return;
            }
            const Recipe &r = Recipes::getList()[row];
            static const QRegularExpression bad(QStringLiteral("[^a-zA-Z0-9_-]+"));
            QString name = QString(r.name).replace(bad, QStringLiteral("_"));
            QString f = QFileDialog::getSaveFileName(this, __("Save recipe"), FileCommunicator::getDefaultDirPath() + QLatin1Char('/') + name + QStringLiteral(".json"), __("Recipe files (*.json)"));
            if (f.isEmpty()) return;
            if (!f.endsWith(QLatin1String(".json"), Qt::CaseInsensitive)) f += QStringLiteral(".json");
            const QString err = Recipes::saveToFile(r, f);
            if (!err.isNull()) QMessageBox::critical(this, __("Save recipe"), __("Could not save recipe: {0}", err));
        });
        connect(fetch, &QPushButton::clicked, this, [this]() { fetchOnline(); });
    }
    QString pageName() const override { return __("Externals"); }
    QString iconName() const override { return QStringLiteral("externals"); }
    QString pageTooltip() const override { return __("Configure external tools"); }
    void loadPreferences() override {
        Recipes::load();
        refresh(-1);
    }
    void savePreferences() override {
        Recipes::save();
        AppContext::updateExternals();
    }

private:
    void refresh(int select) {
        list_->clear();
        for (const Recipe &r : Recipes::getList()) {
            const bool ok = RecipeResolver::isAvailable(r);
            auto *item = new QListWidgetItem(ok ? r.name : r.name + QStringLiteral("   ⚠"), list_);
            if (!ok) item->setForeground(Qt::red);
        }
        if (select >= 0 && select < list_->count()) list_->setCurrentRow(select);
        updateButtons();
    }
    void updateButtons() {
        const int row = list_->currentRow();
        const bool sel = row >= 0 && row < Recipes::getList().size();
        remove_->setEnabled(sel);
        edit_->setEnabled(sel);
        globe_->setEnabled(sel && !Recipes::getList()[row].url.trimmed().isEmpty());
    }
    void editSelected() {
        const int row = list_->currentRow();
        if (row < 0 || row >= Recipes::getList().size()) return;
        RecipeEditorDialog dlg(window(), Recipes::getList()[row]);
        dlg.exec();
        refresh(row);
    }
    void fetchOnline() {
        const auto cached = RecipeCatalog::cached();
        QList<Recipe> chosen;
        if (cached && !cached->isEmpty()) {
            // Show the cached list at once and refresh it in place.
            CatalogChooser chooser(window(), *cached);
            auto *watcher = new QFutureWatcher<std::optional<QList<Recipe>>>(&chooser);
            connect(watcher, &QFutureWatcher<std::optional<QList<Recipe>>>::finished, &chooser, [watcher, &chooser]() {
                const auto fresh = watcher->result();
                if (fresh && !fresh->isEmpty()) chooser.setRecipes(*fresh);   // an empty answer keeps the cached list
            });
            watcher->setFuture(QtConcurrent::run([]() { return RecipeCatalog::fetch(); }));
            chosen = chooser.choose();
        } else {
            QApplication::setOverrideCursor(Qt::WaitCursor);
            QFutureWatcher<std::optional<QList<Recipe>>> watcher;
            QEventLoop loop;
            connect(&watcher, &QFutureWatcher<std::optional<QList<Recipe>>>::finished, &loop, &QEventLoop::quit);
            watcher.setFuture(QtConcurrent::run([]() { return RecipeCatalog::fetch(); }));
            loop.exec();
            QApplication::restoreOverrideCursor();
            const auto fresh = watcher.result();
            if (!fresh || fresh->isEmpty()) {
                QMessageBox::information(this, __("Fetch recipes"), __("No shared recipes available (offline?)."));
                return;
            }
            CatalogChooser chooser(window(), *fresh);
            chosen = chooser.choose();
        }
        if (chosen.isEmpty()) return;
        appendRecipes(chosen, __("Fetch recipes"));
    }
    // Append the recipes whose name is not taken yet; report the skipped ones.
    void appendRecipes(const QList<Recipe> &recipes, const QString &title) {
        QList<Recipe> &list = Recipes::getList();
        QStringList skipped;
        int added = 0;
        for (const Recipe &r : recipes) {
            const bool exists = std::any_of(list.cbegin(), list.cend(), [&](const Recipe &o) { return o.name == r.name; });
            if (exists) {
                skipped.append(r.name);
                continue;
            }
            list.append(r);
            ++added;
        }
        if (added) refresh(list.size() - 1);
        if (!skipped.isEmpty())
            QMessageBox::information(this, title, __("These recipes already exist and were not added:") + QStringLiteral("\n  • ") + skipped.join(QStringLiteral("\n  • ")));
    }
    QListWidget *list_;
    QToolButton *add_, *remove_, *globe_, *edit_;
};

// ---- Plugins page ------------------------------------------------------------------------------------

class PluginsPage : public OptionsPage {
public:
    explicit PluginsPage(QWidget *parent = nullptr) : OptionsPage(parent) {
        auto *lay = new QVBoxLayout(this);
        auto *intro = new QLabel(__("Enable only plugins you trust. Changes apply after you restart Jubler."), this);
        intro->setWordWrap(true);
        intro->setContentsMargins(10, 10, 10, 12);
        lay->addWidget(intro);
        auto *scroll = new QScrollArea(this);
        scroll->setWidgetResizable(true);
        scroll->setHorizontalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
        scroll->setMaximumHeight(260);
        auto *inner = new QWidget(scroll);
        auto *il = new QVBoxLayout(inner);
        for (const auto &p : PluginRegistry::plugins()) {
            auto *box = new QCheckBox(p.name, inner);
            box->setProperty("pluginKey", p.key);
            il->addWidget(box);
            if (!p.description.trimmed().isEmpty()) {
                auto *d = new QLabel(p.description, inner);
                d->setWordWrap(true);
                d->setContentsMargins(26, 0, 0, 0);
                d->setEnabled(false);
                il->addWidget(d);
            }
            boxes_.append(box);
        }
        if (boxes_.isEmpty()) {
            auto *none = new QLabel(__("No plugins are installed. Put a plugin in the plugins folder and restart Jubler."), inner);
            none->setWordWrap(true);
            none->setEnabled(false);
            il->addWidget(none);
        }
        il->addStretch(1);
        scroll->setWidget(inner);
        lay->addWidget(scroll);
        auto *openDir = new QPushButton(__("Open plugins folder"), this);
        connect(openDir, &QPushButton::clicked, this, []() {
            QDir().mkpath(PluginRegistry::directory());
            QDesktopServices::openUrl(QUrl::fromLocalFile(PluginRegistry::directory()));
        });
        lay->addWidget(openDir, 0, Qt::AlignLeft);
        lay->addStretch(1);
    }
    QString pageName() const override { return __("Plugins"); }
    QString iconName() const override { return QStringLiteral("plugins"); }
    QString pageTooltip() const override { return __("Enable or disable installed plugins"); }
    void loadPreferences() override {
        for (QCheckBox *b : boxes_) b->setChecked(PluginRegistry::isEnabled(b->property("pluginKey").toString()));
    }
    void savePreferences() override {
        QSet<QString> keys;
        for (QCheckBox *b : boxes_)
            if (b->isChecked()) keys.insert(b->property("pluginKey").toString());
        if (keys != PluginRegistry::enabledKeys()) {
            PluginRegistry::setEnabledKeys(keys);
            QMessageBox::information(this, __("Plugins"), __("Please exit Jubler and restart it to apply the changes."));
        }
    }

private:
    QList<QCheckBox *> boxes_;
};

// ---- the dialog ----------------------------------------------------------------------------------

class Dialog : public QDialog {
public:
    explicit Dialog(QMenuBar *bar) : QDialog(nullptr) {
        setWindowTitle(__("Jubler Preferences"));
        setModal(true);
        auto *lay = new QVBoxLayout(this);
        strip_ = new QToolBar(this);
        strip_->setMovable(false);
        strip_->setToolButtonStyle(Qt::ToolButtonTextUnderIcon);
        strip_->setIconSize(QSize(48, 48));
        lay->addWidget(strip_);
        stack_ = new QStackedWidget(this);
        stack_->setContentsMargins(8, 12, 8, 4);
        lay->addWidget(stack_, 1);
        group_ = new QButtonGroup(this);
        addPage(shortcuts_ = new ShortcutsPage(bar));
        if (!SystemDependent::isFlatpak()) addPage(new ExternalsPage());
        addPage(new PreviewPage());
        addPage(new PluginsPage());
        addPage(new UiOptionsPage());
        for (const auto &p : PluginHost::preferencesPages()) addPage(new PluginPage(p));

        auto *buttons = new QHBoxLayout();
        auto *exportB = new QPushButton(__("Export"), this);
        auto *importB = new QPushButton(__("Import"), this);
        auto *javaB = new QPushButton(__("Import from Java Jubler"), this);
        javaB->setToolTip(__("Copy the preferences of the older, Java based, Jubler"));
        auto *resetB = new QPushButton(__("Reset"), this);
        buttons->addWidget(exportB);
        buttons->addWidget(importB);
        buttons->addWidget(javaB);
        buttons->addWidget(resetB);
        buttons->addStretch(1);
        auto *cancel = new QPushButton(__("Cancel"), this);
        cancel->setToolTip(__("Cancel changes and revert to previous values"));
        auto *accept = new QPushButton(__("Accept"), this);
        accept->setToolTip(__("Accept and save preferences"));
        accept->setDefault(true);
        buttons->addWidget(cancel);
        buttons->addWidget(accept);
        lay->addLayout(buttons);
        connect(cancel, &QPushButton::clicked, this, &QDialog::reject);
        connect(accept, &QPushButton::clicked, this, &QDialog::accept);
        connect(exportB, &QPushButton::clicked, this, [this]() {
            const QString f = QFileDialog::getSaveFileName(this, __("Export preferences"), FileCommunicator::getDefaultDirPath(), __("Preference files") + QStringLiteral(" (*.ini)"));
            if (f.isEmpty()) return;
            const QString err = Prefs::exportPrefs(f.endsWith(QLatin1String(".ini"), Qt::CaseInsensitive) ? f : f + QStringLiteral(".ini"));
            if (err.isEmpty()) QMessageBox::information(this, __("Success"), __("Preferences exported successfully."));
            else QMessageBox::critical(this, __("Error"), err);
        });
        const auto imported = [this]() {
            Options::load();
            loadAll();
            AppContext::updateAllMenus();   // the store changed already; Cancel puts the snapshot back
            AppContext::updateExternals();
            QMessageBox::information(this, __("Success"), __("Preferences imported successfully.\nConsider exiting Jubler and restarting it to apply the new preferences."));
        };
        connect(importB, &QPushButton::clicked, this, [this, imported]() {
            const QString f = QFileDialog::getOpenFileName(this, __("Import preferences"), FileCommunicator::getDefaultDirPath(), __("Preference files") + QStringLiteral(" (*.ini *.xml)"));
            if (f.isEmpty()) return;
            keepSnapshot();
            const QString err = Prefs::importPrefs(f);
            if (err.isEmpty()) imported();
            else QMessageBox::critical(this, __("Error"), err);
        });
        connect(javaB, &QPushButton::clicked, this, [this, imported]() {
            const QMap<QString, QString> java = JavaPrefs::readNativeStore();
            if (java.isEmpty()) {
                QMessageBox::information(this, __("Import from Java Jubler"), __("No preferences of the Java Jubler were found."));
                return;
            }
            if (QMessageBox::question(this, __("Import from Java Jubler"),
                                      __("This will replace all current preferences with the ones of the Java Jubler.\n\nAre you sure you want to continue?"))
                != QMessageBox::Yes)
                return;
            keepSnapshot();
            JavaPrefs::apply(java);
            imported();
        });
        connect(resetB, &QPushButton::clicked, this, [this]() {
            if (QMessageBox::warning(this, __("Reset Preferences"),
                                     __("This will delete all preferences and reset them to defaults.\nYou will need to exit Jubler for the changes to take full effect.\n\nAre you sure you want to continue?"),
                                     QMessageBox::Yes | QMessageBox::No) != QMessageBox::Yes)
                return;
            keepSnapshot();
            const QString err = Prefs::resetPrefs();
            if (err.isEmpty()) {
                Options::load();
                loadAll();
                AppContext::updateAllMenus();
                AppContext::updateExternals();
                QMessageBox::information(this, __("Reset Preferences"), __("All preferences have been reset to defaults.\nPlease exit Jubler and restart it to apply the changes."));
            } else
                QMessageBox::critical(this, __("Error"), err);
        });
    }

    void addPage(OptionsPage *page) {
        pages_.append(page);
        stack_->addWidget(page);
        auto *b = new QToolButton(strip_);
        b->setText(page->pageName());
        b->setIcon(page->pageIcon());
        b->setToolTip(page->pageTooltip());
        b->setCheckable(true);
        b->setToolButtonStyle(Qt::ToolButtonTextUnderIcon);
        b->setIconSize(strip_->iconSize());   // 48, the pages' SVG size (added with addWidget: the strip's size does not apply)
        b->setMinimumWidth(70);
        group_->addButton(b, pages_.size() - 1);
        strip_->addWidget(b);
        connect(b, &QToolButton::clicked, this, [this, page]() { stack_->setCurrentWidget(page); });
    }

    void loadAll() {
        for (OptionsPage *p : pages_) p->loadPreferences();
    }

    // Import and Reset write the store at once: the store as it was when the
    // dialog opened is kept, so that Cancel can put it back.
    void keepSnapshot() {
        if (!snapshot_) snapshot_ = Prefs::snapshot();
    }

    // The shortcuts page lists the menus of the window the dialog is opened
    // from (the first window may be gone since).
    void setMenuBar(QMenuBar *bar) { shortcuts_->setMenuBar(bar); }

    void run(QWidget *owner) {
        loadAll();
        if (QAbstractButton *first = group_->button(0)) first->click();
        // Packed to the largest page and centred on the screen (Java setLocationRelativeTo(null)).
        setFixedSize(sizeHint());
        if (const QScreen *scr = owner->window()->screen()) move(scr->availableGeometry().center() - rect().center());
        snapshot_.reset();
        if (exec() == QDialog::Accepted)
            for (OptionsPage *p : pages_) p->savePreferences();
        else {
            if (snapshot_) {
                Prefs::restore(*snapshot_);
                Options::load();
                AppContext::updateAllMenus();
                AppContext::updateExternals();
            }
            loadAll();
        }
        snapshot_.reset();
    }

private:
    ShortcutsPage *shortcuts_ = nullptr;
    QToolBar *strip_;
    QStackedWidget *stack_;
    QButtonGroup *group_;
    QList<OptionsPage *> pages_;
    std::optional<QMap<QString, QVariant>> snapshot_;
};

Dialog *g_dialog = nullptr;

}  // namespace

namespace PreferencesDialog {

void showPreferencesDialog(MainWindow *window) {
    if (!g_dialog) g_dialog = new Dialog(window->menus());
    g_dialog->setMenuBar(window->menus());
    g_dialog->run(window);
}

QMap<QString, QString> loadShortcutOverrides() {
    QMap<QString, QString> out;
    // A value ends at the ")" before the next "TAG=(" or the end, so that keys
    // such as "Ctrl+)" survive.
    static const QRegularExpression re(QStringLiteral("(\\w{3,})=\\((.*?)\\)(?=,\\w{3,}=\\(|$)"));
    auto it = re.globalMatch(Prefs::getString(SHORTCUT_KEY, QString()));
    while (it.hasNext()) {
        const auto m = it.next();
        out.insert(m.captured(1), m.captured(2));
    }
    return out;
}

void saveShortcutOverrides(const QMap<QString, QString> &overrides) {
    QStringList parts;
    for (auto it = overrides.constBegin(); it != overrides.constEnd(); ++it)
        parts.append(it.key() + QStringLiteral("=(") + it.value() + QLatin1Char(')'));
    Prefs::set(SHORTCUT_KEY, parts.join(QLatin1Char(',')));
}

void applyMenuShortcuts(const QMap<QString, QAction *> &actions) {
    const QMap<QString, QString> overrides = loadShortcutOverrides();
    for (auto it = actions.constBegin(); it != actions.constEnd(); ++it) {
        QAction *a = it.value();
        if (!a->property(DEFAULT_PROP).isValid()) a->setProperty(DEFAULT_PROP, a->shortcut().toString(QKeySequence::PortableText));
        const QKeySequence seq = QKeySequence::fromString(overrides.contains(it.key()) ? overrides.value(it.key()) : a->property(DEFAULT_PROP).toString(),
                                                          QKeySequence::PortableText);
        // Return also answers to the keypad Enter (Java VK_ENTER is both keys).
        QList<QKeySequence> keys{seq};
        if (seq.count() == 1 && seq[0].key() == Qt::Key_Return) keys.append(QKeySequence(QKeyCombination(seq[0].keyboardModifiers(), Qt::Key_Enter)));
        a->setShortcuts(keys);
    }
}

}  // namespace PreferencesDialog
