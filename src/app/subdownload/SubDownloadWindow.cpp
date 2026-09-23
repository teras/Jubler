/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/subdownload/SubDownloadWindow.h"

#include <QCheckBox>
#include <QCloseEvent>
#include <QCollator>
#include <QComboBox>
#include <QDir>
#include <QFileInfo>
#include <QGridLayout>
#include <QHBoxLayout>
#include <QHeaderView>
#include <QKeyEvent>
#include <QLabel>
#include <QLineEdit>
#include <QLocale>
#include <QMessageBox>
#include <QPushButton>
#include <QRegularExpression>
#include <QSaveFile>
#include <QTableWidget>
#include <QUuid>
#include <QVBoxLayout>
#include <QtConcurrent>
#include <algorithm>

#include "app/Theme.h"
#include "app/media/AppMediaFile.h"
#include "app/subdownload/Providers.h"
#include "app/ui/MainWindow.h"
#include "core/i18n/I18N.h"
#include "core/options/Options.h"
#include "core/options/Prefs.h"
#include "core/os/Debug.h"
#include "core/os/FileCommunicator.h"
#include "core/subs/SubFile.h"
#include "core/subs/Subtitles.h"
#include "core/undo/UndoList.h"

using jubler::Candidate;
using jubler::DownloadData;
using jubler::ProviderError;
using jubler::SearchRequest;
using jubler::SubtitleProvider;

// ---- DownloadLanguages --------------------------------------------------------------------------------

namespace DownloadLanguages {
QList<Entry> list() {
    struct L { const char *id; const char *name; };
    static const L langs[] = {{"ar", N__("Arabic")}, {"bg", N__("Bulgarian")}, {"zh", N__("Chinese")}, {"hr", N__("Croatian")}, {"cs", N__("Czech")}, {"da", N__("Danish")}, {"nl", N__("Dutch")}, {"en", N__("English")},
                              {"et", N__("Estonian")}, {"fi", N__("Finnish")}, {"fr", N__("French")}, {"de", N__("German")}, {"el", N__("Greek")}, {"he", N__("Hebrew")}, {"hi", N__("Hindi")}, {"hu", N__("Hungarian")},
                              {"id", N__("Indonesian")}, {"it", N__("Italian")}, {"ja", N__("Japanese")}, {"ko", N__("Korean")}, {"lv", N__("Latvian")}, {"lt", N__("Lithuanian")}, {"no", N__("Norwegian")}, {"pl", N__("Polish")},
                              {"pt", N__("Portuguese")}, {"ro", N__("Romanian")}, {"ru", N__("Russian")}, {"sr", N__("Serbian")}, {"sk", N__("Slovak")}, {"sl", N__("Slovenian")}, {"es", N__("Spanish")}, {"sv", N__("Swedish")},
                              {"th", N__("Thai")}, {"tr", N__("Turkish")}, {"uk", N__("Ukrainian")}, {"vi", N__("Vietnamese")}};
    QList<Entry> out;
    for (const L &l : langs) out.append({QString::fromLatin1(l.id), __(l.name)});
    const QString pref = Options::getLanguage();
    QCollator collator(pref.isEmpty() || pref == QLatin1String("auto") ? QLocale::system() : QLocale(pref));
    collator.setCaseSensitivity(Qt::CaseInsensitive);
    std::sort(out.begin(), out.end(), [&](const Entry &a, const Entry &b) { return collator.compare(a.name, b.name) < 0; });
    out.prepend({QString(QLatin1String("")), __("<Any language>")});
    return out;
}
}  // namespace DownloadLanguages

// ---- SubDownloadWindow ----------------------------------------------------------------------------------

namespace {
const QString PROVIDER_KEY = QStringLiteral("subdownload.provider");
const QString LANGUAGE_KEY = QStringLiteral("subdownload.language");
QMap<MainWindow *, SubDownloadWindow *> g_windows;

QString prefillQuery(const MainWindow *owner) {
    const AppMediaFile *m = owner->getMediaFile();
    if (!m || !m->getVideoFile() || !m->getVideoFile()->exists()) return QString();
    QString name = QFileInfo(m->getVideoFile()->getPath()).fileName();
    const int dot = name.lastIndexOf(QLatin1Char('.'));
    if (dot > 0) name = name.left(dot);
    static const QRegularExpression seps(QStringLiteral("[._]+")), ws(QStringLiteral("\\s+"));
    return name.replace(seps, QStringLiteral(" ")).replace(ws, QStringLiteral(" ")).trimmed();
}

QString documentName(const MainWindow *owner) {
    const AppMediaFile *m = owner->getMediaFile();
    if (m && m->getVideoFile() && m->getVideoFile()->exists()) return QFileInfo(m->getVideoFile()->getPath()).fileName();
    if (owner->getSubtitles()) {
        const QString n = QFileInfo(owner->getSubtitles()->getSubFile().getSaveFile()).fileName();
        if (!n.isEmpty()) return n;
    }
    return __("Untitled");
}

// The first number of a Downloads/Rating cell ("8.5/10" → 8.5, "★ 8.5" → 8.5),
// −1 when none (Java numericValue: digits and dots up to the first gap).
double leadingNumber(const QString &s) {
    QString digits;
    for (const QChar c : s) {
        if (c.isDigit() || c == QLatin1Char('.')) digits += c;
        else if (!digits.isEmpty()) break;
    }
    bool ok = false;
    const double v = digits.toDouble(&ok);
    return ok ? v : -1;
}

class NumericItem : public QTableWidgetItem {
public:
    using QTableWidgetItem::QTableWidgetItem;
    bool operator<(const QTableWidgetItem &other) const override { return leadingNumber(text()) < leadingNumber(other.text()); }
};

class TextItem : public QTableWidgetItem {
public:
    using QTableWidgetItem::QTableWidgetItem;
    bool operator<(const QTableWidgetItem &other) const override { return text().compare(other.text(), Qt::CaseInsensitive) < 0; }
};
}  // namespace

void SubDownloadWindow::open(MainWindow *owner) {
    SubDownloadWindow *w = g_windows.value(owner, nullptr);
    if (!w) {
        w = new SubDownloadWindow(owner);
        g_windows.insert(owner, w);
        QObject::connect(owner, &QObject::destroyed, w, [w]() { w->close(); });
    }
    w->show();
    w->raise();
    w->activateWindow();
}

void SubDownloadWindow::keyPressEvent(QKeyEvent *e) {
    if (e->key() == Qt::Key_Escape && e->modifiers() == Qt::NoModifier) {
        e->accept();
        return;
    }
    QDialog::keyPressEvent(e);
}

SubDownloadWindow::SubDownloadWindow(MainWindow *owner) : QDialog(owner), owner_(owner) {
    setModal(false);
    setWindowFlags(Qt::Window);   // an independent window, as the Java JFrame
    setAttribute(Qt::WA_DeleteOnClose);
    setWindowTitle(__("Download subtitles") + QStringLiteral(" — ") + documentName(owner));
    resize(1180, 640);
    SubtitleProviders::registerBuiltin();
    providers_ = SubtitleProviders::all();

    auto *lay = new QVBoxLayout(this);
    auto *form = new QGridLayout();
    form->addWidget(new QLabel(__("Provider"), this), 0, 0);
    providerC_ = new QComboBox(this);
    for (const auto &p : providers_) providerC_->addItem(p->getName());
    form->addWidget(providerC_, 0, 1);
    configureB_ = new QPushButton(__("Configure…"), this);
    form->addWidget(configureB_, 0, 2);
    form->addWidget(new QLabel(__("Language"), this), 1, 0);
    languageC_ = new QComboBox(this);
    for (const auto &l : DownloadLanguages::list()) languageC_->addItem(l.name, l.id);
    form->addWidget(languageC_, 1, 1, 1, 2);
    hashC_ = new QCheckBox(__("Match by video hash"), this);
    form->addWidget(hashC_, 2, 1, 1, 2);
    form->addWidget(new QLabel(__("Search"), this), 3, 0);
    query_ = new QLineEdit(prefillQuery(owner), this);
    form->addWidget(query_, 3, 1);
    searchB_ = new QPushButton(__("Search"), this);
    searchB_->setDefault(true);
    form->addWidget(searchB_, 3, 2);
    form->setColumnStretch(1, 1);
    lay->addLayout(form);

    table_ = new QTableWidget(0, 4, this);
    table_->setSelectionMode(QAbstractItemView::SingleSelection);
    table_->setSelectionBehavior(QAbstractItemView::SelectRows);
    table_->setEditTriggers(QAbstractItemView::NoEditTriggers);
    table_->setSortingEnabled(true);
    table_->verticalHeader()->hide();
    auto *release = new QTableWidgetItem(__("Release"));
    auto *language = new QTableWidgetItem(Theme::icon(QStringLiteral("flag-global")), QString());
    language->setToolTip(__("Language"));
    auto *downloads = new QTableWidgetItem(Theme::icon(QStringLiteral("download")), QString());
    downloads->setToolTip(__("Downloads"));
    auto *rating = new QTableWidgetItem(Theme::icon(QStringLiteral("star")), QString());
    rating->setToolTip(__("Rating"));
    table_->setHorizontalHeaderItem(0, release);
    table_->setHorizontalHeaderItem(1, language);
    table_->setHorizontalHeaderItem(2, downloads);
    table_->setHorizontalHeaderItem(3, rating);
    table_->horizontalHeader()->setSectionResizeMode(0, QHeaderView::Stretch);
    table_->setColumnWidth(1, 90);
    table_->setColumnWidth(2, 78);
    table_->setColumnWidth(3, 78);
    table_->horizontalHeader()->setSortIndicatorShown(true);
    table_->horizontalHeader()->setSortIndicator(-1, Qt::AscendingOrder);
    lay->addWidget(table_, 1);

    auto *bottom = new QHBoxLayout();
    status_ = new QLabel(this);
    status_->setMinimumWidth(0);
    status_->setSizePolicy(QSizePolicy::Ignored, QSizePolicy::Preferred);
    bottom->addWidget(status_, 1);
    status_->installEventFilter(this);
    downloadB_ = new QPushButton(QString(__("Download & preview")).replace(QLatin1Char('&'), QLatin1String("&&")), this);
    downloadB_->setEnabled(false);
    bottom->addWidget(downloadB_);
    lay->addLayout(bottom);

    // Remembered provider and language.
    const QString wantP = Prefs::getString(PROVIDER_KEY, QString());
    int pi = 0;
    for (int i = 0; i < providers_.size(); ++i)
        if (providers_[i]->getName() == wantP) pi = i;
    providerC_->setCurrentIndex(pi);
    const QString wantL = Prefs::getString(LANGUAGE_KEY, QStringLiteral("en"));
    int li = languageC_->findData(wantL);
    if (li < 0) li = languageC_->findData(QStringLiteral("en"));
    languageC_->setCurrentIndex(std::max(0, li));

    connect(providerC_, &QComboBox::currentIndexChanged, this, [this]() {
        if (auto p = provider()) Prefs::set(PROVIDER_KEY, p->getName());
        onProviderChanged();
    });
    connect(languageC_, &QComboBox::currentIndexChanged, this, [this]() { Prefs::set(LANGUAGE_KEY, languageC_->currentData().toString()); });
    connect(configureB_, &QPushButton::clicked, this, [this]() { if (auto p = provider()) p->configure(this); });
    connect(hashC_, &QCheckBox::clicked, this, [this](bool on) {
        userHashChoice_ = on;
        updateHashControls();
    });
    connect(searchB_, &QPushButton::clicked, this, &SubDownloadWindow::search);
    connect(query_, &QLineEdit::returnPressed, this, &SubDownloadWindow::search);
    connect(downloadB_, &QPushButton::clicked, this, &SubDownloadWindow::download);
    connect(table_, &QTableWidget::itemSelectionChanged, this, [this]() { downloadB_->setEnabled(!downloading_ && table_->currentRow() >= 0); });
    onProviderChanged();
}

SubDownloadWindow::~SubDownloadWindow() {
    g_windows.remove(owner_);
    if (auto s = searching_.lock()) s->cancelSearch();
}

// The provider a result came from (the combo may show another one by now).
std::shared_ptr<SubtitleProvider> SubDownloadWindow::providerNamed(const QString &name) const {
    for (const auto &p : providers_)
        if (p->getName() == name) return p;
    return nullptr;
}

std::shared_ptr<SubtitleProvider> SubDownloadWindow::provider() const {
    const int i = providerC_->currentIndex();
    return i >= 0 && i < providers_.size() ? providers_[i] : nullptr;
}

void SubDownloadWindow::onProviderChanged() {
    auto p = provider();
    configureB_->setVisible(p && p->needsConfiguration());
    updateHashControls();
}

void SubDownloadWindow::updateHashControls() {
    auto p = provider();
    const bool hashable = p && p->hashSupport() == SubtitleProvider::HashSupport::HASH_OPTIONAL;
    const AppMediaFile *m = owner_->getMediaFile();
    const bool video = m && m->getVideoFile() && m->getVideoFile()->exists();
    if (!hashable) {
        hashC_->setEnabled(false);
        hashC_->setChecked(false);
    } else {
        hashC_->setEnabled(video);
        hashC_->setChecked(video && userHashChoice_);
    }
    query_->setEnabled(!hashC_->isChecked());
}

bool SubDownloadWindow::event(QEvent *e) {
    if (e->type() == QEvent::WindowActivate) updateHashControls();
    return QDialog::event(e);
}

void SubDownloadWindow::closeEvent(QCloseEvent *e) {
    if (auto s = searching_.lock()) s->cancelSearch();
    ++searchSerial_;
    QDialog::closeEvent(e);
}

bool SubDownloadWindow::eventFilter(QObject *watched, QEvent *e) {
    if (watched == status_ && e->type() == QEvent::Resize) elideStatus();
    return QDialog::eventFilter(watched, e);
}

void SubDownloadWindow::elideStatus() {
    status_->setText(status_->fontMetrics().elidedText(statusText_, Qt::ElideRight, status_->width()));
}

void SubDownloadWindow::setStatus(const QString &text, bool error) {
    statusText_ = text;
    elideStatus();
    status_->setToolTip(text);
    status_->setStyleSheet(error ? QStringLiteral("color: rgb(176,48,48)") : QString());
    if (error) Debug::debug(QStringLiteral("Subtitle download: ") + text);
}

void SubDownloadWindow::reportFailure(const ProviderError &error) { setStatus(error.message.isEmpty() ? __("Operation failed.") : error.message, true); }

namespace {
// A provider call on the worker thread. Providers return their failures;
// an exception is still caught here, as a guard against a faulty one.
template <typename T, typename F>
jubler::ProviderResult<T> callProvider(F &&call) {
    try {
        return call();
    } catch (const std::exception &e) {
        Debug::debug(e);
        return ProviderError{ProviderError::Kind::NETWORK, QString::fromUtf8(e.what())};
    } catch (...) {
        return ProviderError{ProviderError::Kind::NETWORK, QString()};
    }
}
}  // namespace

void SubDownloadWindow::search() {
    auto p = provider();
    if (!p) return;
    const AppMediaFile *m = owner_->getMediaFile();
    const QString video = m && m->getVideoFile() && m->getVideoFile()->exists() ? m->getVideoFile()->getPath() : QString();
    SearchRequest req;
    req.useHash = hashC_->isChecked();
    req.videoPath = video;
    req.languageCode = languageC_->currentData().toString();
    static const QRegularExpression controls(QStringLiteral("[\\x00-\\x1f\\x7f]")), ws(QStringLiteral("\\s+"));
    req.query = QString(query_->text()).replace(controls, QStringLiteral(" ")).replace(ws, QStringLiteral(" ")).trimmed();
    if (req.useHash && video.isEmpty()) {
        setStatus(__("Open a video first to match by hash."), true);
        return;
    }
    if (!req.useHash && req.query.isEmpty()) {
        setStatus(__("Enter something to search for."), true);
        return;
    }
    const QString notReady = p->ensureReady(this);
    if (!notReady.isNull()) {
        setStatus(notReady, true);
        return;
    }
    if (auto s = searching_.lock()) s->cancelSearch();
    searching_ = p;
    const int serial = ++searchSerial_;
    searchB_->setEnabled(false);
    setStatus(__("Searching…"), false);
    auto result = std::make_shared<jubler::ProviderResult<QList<Candidate>>>();
    auto *watcher = new QFutureWatcher<void>(this);
    const QString name = p->getName();
    connect(watcher, &QFutureWatcher<void>::finished, this, [this, watcher, serial, result, name]() {
        watcher->deleteLater();
        if (serial != searchSerial_) return;   // superseded
        searchB_->setEnabled(true);
        if (result->error) {
            reportFailure(result->error);
            return;
        }
        QList<Candidate> &results = result->value;
        for (Candidate &c : results)
            if (c.provider.isEmpty()) c.provider = name;   // results remember their provider
        showResults(results);
        setStatus(results.isEmpty() ? __("No subtitles found.") : __("{0} results.", results.size()), false);
    });
    watcher->setFuture(QtConcurrent::run([p, req, result]() { *result = callProvider<QList<Candidate>>([&] { return p->search(req); }); }));
}

void SubDownloadWindow::showResults(const QList<Candidate> &results) {
    results_ = results;
    table_->setSortingEnabled(false);
    table_->setRowCount(results.size());
    for (int i = 0; i < results.size(); ++i) {
        const Candidate &c = results[i];
        auto *r = new TextItem(c.releaseName);
        r->setData(Qt::UserRole, i);
        table_->setItem(i, 0, r);
        table_->setItem(i, 1, new TextItem(c.language));
        auto *d = new NumericItem(c.downloads);
        d->setTextAlignment(Qt::AlignCenter);
        table_->setItem(i, 2, d);
        auto *rt = new NumericItem(c.rating);
        rt->setTextAlignment(Qt::AlignCenter);
        table_->setItem(i, 3, rt);
    }
    table_->setSortingEnabled(true);
    downloadB_->setEnabled(false);
}

void SubDownloadWindow::download() {
    const int row = table_->currentRow();
    if (row < 0 || !table_->item(row, 0)) {
        setStatus(__("Select a subtitle first."), true);
        return;
    }
    if (downloading_) return;
    const Candidate c = results_[table_->item(row, 0)->data(Qt::UserRole).toInt()];
    auto p = providerNamed(c.provider);   // the one that produced the result
    if (!p) {
        setStatus(__("Select a subtitle first."), true);
        return;
    }
    const QString cacheKey = c.provider + QLatin1Char('\n') + c.handle;
    if (cache_.contains(cacheKey)) {
        if (applyDownloaded(c, cache_.value(cacheKey))) setStatus(__("Applied from cache."), false);
        return;
    }
    const QString notReady = p->ensureReady(this);
    if (!notReady.isNull()) {
        setStatus(notReady, true);
        return;
    }
    downloading_ = true;
    downloadB_->setEnabled(false);
    setStatus(__("Downloading…"), false);
    auto result = std::make_shared<jubler::ProviderResult<DownloadData>>();
    auto *watcher = new QFutureWatcher<void>(this);
    connect(watcher, &QFutureWatcher<void>::finished, this, [this, watcher, c, result, cacheKey]() {
        watcher->deleteLater();
        downloading_ = false;
        downloadB_->setEnabled(table_->currentRow() >= 0);
        if (result->error) {
            reportFailure(result->error);
            return;
        }
        if (applyDownloaded(c, result->value)) {
            cache_.insert(cacheKey, result->value);
            setStatus(__("Downloaded and applied."), false);
        }
    });
    watcher->setFuture(QtConcurrent::run([p, c, result]() { *result = callProvider<DownloadData>([&] { return p->download(c); }); }));
}

// Turn the payload into a temporary file and apply it as an undoable
// replacement of the owner document. Port of `DownloadApply`.
bool SubDownloadWindow::applyDownloaded(const Candidate &c, const DownloadData &data) {
    const QString dir = QDir::tempPath() + QStringLiteral("/jubler-subdownload");
    QDir().mkpath(dir);
    QString ext = QStringLiteral(".srt");
    static const QRegularExpression extRe(QStringLiteral("\\.[a-z0-9]+$"));
    const auto m = extRe.match(c.fileHint.toLower());
    if (m.hasMatch() && m.captured(0).length() <= 6) ext = m.captured(0);
    const QString path = dir + QStringLiteral("/sub") + QUuid::createUuid().toString(QUuid::WithoutBraces).left(8) + ext;
    {
        QSaveFile f(path);
        if (!f.open(QIODevice::WriteOnly) || f.write(data.bytes) < 0 || !f.commit()) {
            setStatus(__("Could not read the downloaded subtitle."), true);
            return false;
        }
    }
    SubFile sf(path, SubFile::EXTENSION_GIVEN);
    const QString text = FileCommunicator::detectAndDecode(sf, data.bytes, false);
    if (text.isNull()) {
        Debug::debug(QStringLiteral("Subtitle download: undecodable payload, first bytes ") + QString::fromLatin1(data.bytes.left(16).toHex(' ')));
        setStatus(__("Could not read the downloaded subtitle."), true);
        return false;
    }
    auto result = std::make_unique<Subtitles>(sf);
    result->populate(sf, text, false);
    if (result->isEmpty()) {
        setStatus(__("The download was not recognized as a subtitle."), true);
        return false;
    }
    result->setLoadedBytes(data.bytes);
    Debug::debug(QStringLiteral("Subtitle download: %1 hint=%2 file=%3 type=%4 format=%5 encoding=%6 size=%7 entries=%8")
                     .arg(c.provider, c.fileHint, QFileInfo(path).fileName(), data.contentType, sf.getFormat() ? sf.getFormat()->getName() : QString(), sf.getEncoding())
                     .arg(data.bytes.size())
                     .arg(result->size()));
    // The owner's save location with the download's encoding and format.
    Subtitles *current = owner_->getSubtitles();
    if (current) {
        SubFile target(current->getSubFile());
        target.setEncoding(sf.getEncoding());
        target.setFormat(sf.getFormat());
        target.updateFileByType();
        result->setSubFile(target);
        owner_->getUndoList()->addUndo(*current, __("Download: {0}", c.releaseName));
    }
    owner_->getUndoList()->invalidateSaveMark();
    owner_->setSubs(std::move(result));
    owner_->enableWindowControls(false);
    owner_->showEncodingBar();
    owner_->showInfo();
    QFile::remove(path);
    return true;
}
