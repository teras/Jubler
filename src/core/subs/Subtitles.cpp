/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/subs/Subtitles.h"

#include "core/os/FileCommunicator.h"

#include <QFileInfo>
#include <algorithm>
#include <limits>

#include "core/i18n/I18N.h"

QStringList Subtitles::columnNames() {
    return {__("#"), __("Start"), __("End"), __("Duration"), __("Layer"), __("Style"), __("Cpm"), __("Cps"), __("Subtitle")};
}

Subtitles::Subtitles() : Subtitles(SubFile()) {}

Subtitles::Subtitles(const SubFile &sfile) : subfile_(sfile) {}

Subtitles::Subtitles(const Subtitles &old) {
    *this = old;
}

Subtitles &Subtitles::operator=(const Subtitles &old) {
    if (this == &old)
        return *this;
    styles_ = old.styles_;
    attribs_ = old.attribs_;
    formatData_ = old.formatData_;
    subfile_ = old.subfile_;
    // The raw bytes of a load stay with the original: a copy (undo snapshot,
    // clone, child) is never re-decodable.
    loadedBytes_ = QByteArray();
    sublist_.clear();
    for (const SubEntryPtr &oldentry : old.sublist_) {
        auto newentry = std::make_shared<SubEntry>(*oldentry);
        sublist_.append(newentry);
        if (newentry->getStyle())
            newentry->setStyle(styles_.getStyleByName(oldentry->getStyle()->getName()));
    }
    return *this;
}

namespace {
QString fileExtension(const QString &path) {
    const QString name = QFileInfo(path).fileName();
    const int dot = name.lastIndexOf(QLatin1Char('.'));
    return dot >= 0 ? name.mid(dot + 1) : QString();
}
}  // namespace

namespace {
// What the loaded file said about itself: its frame rate, and whether it
// declared one (every load path).
void commitDetected(SubFile &sfile, const SubFormat &format) {
    if (format.detectedFPS() > 0) sfile.setFPS(format.detectedFPS());
    sfile.setFrameRateHeader(format.hasFrameRateHeader());
    sfile.setFPSForced(false);
}
}  // namespace

std::unique_ptr<Subtitles> Subtitles::loadByFileExtension(SubFile &sfile, const QString &data, bool debug) {
    const QString file = sfile.getSaveFile();
    SubFormatPtr found = Availabilities::formats().findFromExtension(fileExtension(file));
    // Never trust the extension for a catch-all plain-text format: fall
    // through to content-based detection so a structured format is preferred
    // (a SubRip download labelled ".txt" must load as SubRip).
    if (found && found->isLastResort())
        found.reset();
    if (!found)
        return nullptr;
    SubFormatPtr format = found->newInstance();
    format->updateFormat(sfile);
    auto load = format->parse(data, sfile.getFPS(), file, debug);
    if (load) {
        sfile.setFormat(format);
        commitDetected(sfile, *format);
    }
    return load;
}

std::unique_ptr<Subtitles> Subtitles::loadBySelectedHandler(SubFile &sfile, const QString &data, bool debug) {
    SubFormatPtr format = sfile.getFormat();
    if (!format)
        return nullptr;
    format->updateFormat(sfile);
    auto load = format->parse(data, sfile.getFPS(), sfile.getSaveFile(), debug);
    if (load) commitDetected(sfile, *format);
    return load;
}

std::unique_ptr<Subtitles> Subtitles::loadByPattern(SubFile &sfile, const QString &data, bool debug) {
    const QString file = sfile.getSaveFile();
    const AvailSubFormats &formatlist = Availabilities::formats();
    // Two passes so the catch-all plain-text formats stay the very last resort.
    auto load = tryFormatsByPattern(sfile, data, file, debug, formatlist, false);
    if (!load)
        load = tryFormatsByPattern(sfile, data, file, debug, formatlist, true);
    return load;
}

std::unique_ptr<Subtitles> Subtitles::tryFormatsByPattern(SubFile &sfile, const QString &data, const QString &file, bool debug,
                                                          const AvailSubFormats &formatlist, bool lastResort) {
    for (const SubFormatPtr &available : formatlist.getFormats()) {
        if (available->isLastResort() != lastResort)
            continue;
        SubFormatPtr format = available->newInstance();
        format->updateFormat(sfile);
        auto load = format->parse(data, sfile.getFPS(), file, debug);
        if (load && load->size() >= 1) {
            sfile.setFormat(format);
            commitDetected(sfile, *format);
            return load;
        }
    }
    return nullptr;
}

void Subtitles::populate(SubFile &sfile, const QString &raw, bool debug) {
    // Every source of text ends here: a decoded file, a subtitle stream taken
    // out of a video, the output of an external tool, a download, a plugin.
    // Only a file passes through the decoder, so the preparation the parsers
    // count on is made here, where all of them meet.
    const QString data = FileCommunicator::prepareForParsing(raw);
    auto load = loadByFileExtension(sfile, data, debug);
    if (!load)
        load = loadBySelectedHandler(sfile, data, debug);
    if (!load)
        load = loadByPattern(sfile, data, debug);
    if (load) {
        appendSubs(*load, true);
        attribs_ = load->attribs_;
        formatData_ = load->formatData_;
    }
    subfile_ = sfile;  // the format/FPS chosen while loading belong to the document
}

void Subtitles::sort(double mintime, double maxtime) {
    QList<SubEntryPtr> sorted;
    int lastpos = -1;
    for (int i = size() - 1; i >= 0; --i) {
        const SubEntryPtr sub = sublist_.at(i);
        const double time = sub->getStartTime().toSeconds();
        if (time >= mintime && time <= maxtime) {
            lastpos = i;
            sorted.append(sub);
            sublist_.removeAt(i);
        }
    }
    if (lastpos == -1)
        return;
    std::reverse(sorted.begin(), sorted.end());   // document order, so equal starts keep it
    std::stable_sort(sorted.begin(), sorted.end(),
                     [](const SubEntryPtr &a, const SubEntryPtr &b) { return a->compareTo(*b) < 0; });
    for (int i = 0; i < sorted.size(); ++i)
        sublist_.insert(lastpos + i, sorted.at(i));
}

void Subtitles::appendSubs(const Subtitles &newsubs, bool priorityToNewStyle) {
    // The appended entries keep pointing at the styles of `newsubs`; they are
    // re-linked below to this document's styles of the same name.
    sublist_.append(newsubs.sublist_);
    if (priorityToNewStyle)
        styles_.get(0)->setValues(*newsubs.styles_.get(0));
    for (int i = 1; i < newsubs.styles_.size(); ++i) {
        const SubStylePtr newstyle = newsubs.styles_.get(i);
        const int existing = styles_.indexOfName(newstyle->getName());
        if (existing < 0)
            styles_.add(newstyle);
        else if (existing == 0 && priorityToNewStyle)
            styles_.get(0)->setValues(*newstyle);   // a non-default style literally named like Default merges into it
        // Otherwise a same-named style already exists: keep the existing one.
        // (The Java code clobbered the Default style here, see JAVA_BUGS #6.)
    }
    styles_.get(0)->setDefault(true);
    revalidateStyles();
}

void Subtitles::insertSubs(const SubEntryPtr &location, const Subtitles &newsubs) {
    const int idx = sublist_.indexOf(location);
    if (idx < 0)
        appendSubs(newsubs, false);
    else {
        for (int i = 0; i < newsubs.sublist_.size(); ++i)
            sublist_.insert(idx + 1 + i, newsubs.sublist_.at(i));
        revalidateStyles();
    }
}

SubEntryPtr Subtitles::joinSubs(const Subtitles &s1, const Subtitles &s2, double dt) {
    appendSubs(s1, false);
    const double maxtime = s1.getMaxTime() + dt;
    SubEntryPtr selected;
    for (int i = 0; i < s2.size(); ++i) {
        auto newentry = std::make_shared<SubEntry>(*s2.elementAt(i));
        newentry->getStartTime().addTime(maxtime);
        newentry->getFinishTime().addTime(maxtime);
        add(newentry);
        if (!selected)
            selected = newentry;
    }
    revalidateStyles();  // the copies point at s2's style objects; re-link by name
    return selected;
}

double Subtitles::getMaxTime() const {
    double max = 0;
    for (const SubEntryPtr &e : sublist_) {
        const double cur = e->getFinishTime().toSeconds();
        if (cur > max)
            max = cur;
    }
    return max;
}

int Subtitles::addSorted(const SubEntryPtr &sub) {
    const double time = sub->getStartTime().toSeconds();
    int pos = 0;
    while (sublist_.size() > pos && sublist_.at(pos)->getStartTime().toSeconds() < time)
        ++pos;
    sublist_.insert(pos, sub);
    if (!sub->getStyle())
        sub->setStyle(styles_.get(0));
    return pos;
}

void Subtitles::add(const SubEntryPtr &sub) {
    sublist_.append(sub);
    if (!sub->getStyle())
        sub->setStyle(styles_.get(0));
}

TotalSubMetrics Subtitles::getTotalMetrics() const {
    TotalSubMetrics max;
    for (const SubEntryPtr &entry : sublist_) {
        max.updateToMaxValues(entry->getMetrics());
        max.updateDuration(float(entry->getDurationTime().toSeconds()));
    }
    return max;
}

int Subtitles::findSubEntry(double time, bool fuzzyMatch) const {
    int fuzzyresult = -1;
    double fuzzyDiff = std::numeric_limits<double>::max();
    for (int i = 0; i < sublist_.size(); ++i) {
        const SubEntryPtr &entry = sublist_.at(i);
        if (entry->isInTime(time))
            return i;
        if (fuzzyMatch) {
            const double cdiff = std::abs(time - entry->getStartTime().toSeconds());
            if (cdiff > 0 && cdiff < fuzzyDiff) {
                fuzzyDiff = cdiff;
                fuzzyresult = i;
            }
        }
    }
    return fuzzyresult;
}

void Subtitles::revalidateStyles() {
    for (const SubEntryPtr &entry : sublist_) {
        const SubStylePtr style = entry->getStyle();
        if (!style || !styles_.contains(style)) {
            // Re-link by name when a same-named style exists (entries coming
            // from another document), else Default.
            entry->setStyle(style ? styles_.getStyleByName(style->getName()) : styles_.get(0));
        }
    }
}

void Subtitles::updateQuality() {
    for (const SubEntryPtr &entry : sublist_)
        entry->updateQuality();
}

bool Subtitles::replace(const SubEntryPtr &sub, int row) {
    sub->setStyle(styles_.get(0));
    if (row >= 0 && row < sublist_.size()) {
        sublist_[row] = sub;
        return true;
    }
    return false;
}

void Subtitles::moveRow(int start, int end, int to) {
    const int shift = to - start;
    int first, last;
    if (shift < 0) {
        first = to;
        last = end;
    } else {
        first = start;
        last = to + end - start;
    }
    // Rotate [first, last] by `shift` (the Java gcd-based in-place rotation).
    if (last >= sublist_.size() || first < 0 || first > last)
        return;
    QList<SubEntryPtr> block = sublist_.mid(first, last - first + 1);
    const int n = block.size();
    const int r = ((n - shift) % n + n) % n;
    std::rotate(block.begin(), block.begin() + r, block.end());
    for (int i = 0; i < n; ++i)
        sublist_[first + i] = block.at(i);
}
