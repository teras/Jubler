/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QByteArray>
#include <QList>
#include <QMap>
#include <QString>
#include <memory>

#include "core/style/SubStyle.h"
#include "core/subs/SubAttribs.h"
#include "core/subs/SubEntry.h"
#include "core/subs/SubFile.h"
#include "core/subs/SubMetrics.h"

using SubEntryPtr = std::shared_ptr<SubEntry>;

// The document: the ordered entry list, the style table, the metadata and the
// file description. Port of the Java `Subtitles` minus its Swing table-model
// half (see SubtitleTableModel in the app layer). Entries are shared objects
// so identity-based operations (indexOf, remove, insert-after) work as in
// Java; copying a document deep-copies entries and styles and re-links each
// copied entry to the copied style of the same name.
class Subtitles {
public:
    // Column names of the subtitle table (Java COLNAME), translated.
    static QStringList columnNames();
    static constexpr int COLUMN_COUNT = 9;

    Subtitles();
    explicit Subtitles(const SubFile &sfile);
    Subtitles(const Subtitles &old);
    Subtitles &operator=(const Subtitles &old);

    // Detect the format of `data` (by extension, then by the file's chosen
    // format, then by content) and load it, appending the entries and
    // adopting the metadata. `sfile` receives the format that succeeded.
    // `data` may come from anywhere (a decoded file, a subtitle stream of a
    // video, a tool, a download): it is prepared for the parsers here.
    void populate(SubFile &sfile, const QString &data, bool debug);

    // Sort chronologically the entries whose start lies in [mintime, maxtime]
    // seconds, keeping them in the slot of the earliest such entry.
    void sort(double mintime, double maxtime);
    void insertSubs(const SubEntryPtr &location, const Subtitles &newsubs);
    // Append s1, then s2 shifted so that it starts `dt` seconds after the end
    // of s1; returns the first entry of the appended s2 (for selection).
    SubEntryPtr joinSubs(const Subtitles &s1, const Subtitles &s2, double dt);
    int addSorted(const SubEntryPtr &sub);
    void add(const SubEntryPtr &sub);
    void insert(int i, const SubEntryPtr &sub) { sublist_.insert(i, sub); }
    void remove(int i) { sublist_.removeAt(i); }
    void remove(const SubEntryPtr &sub) { sublist_.removeOne(sub); }
    SubEntryPtr elementAt(int i) const { return sublist_.at(i); }
    SubEntryPtr get(int i) const { return sublist_.at(i); }
    bool isEmpty() const { return sublist_.isEmpty(); }
    int size() const { return sublist_.size(); }
    const QList<SubEntryPtr> &entries() const { return sublist_; }
    void setSublist(const QList<SubEntryPtr> &l) { sublist_ = l; }
    TotalSubMetrics getTotalMetrics() const;
    int indexOf(const SubEntryPtr &e) const { return sublist_.indexOf(e); }
    // Entry containing `time` (seconds); with fuzzyMatch the nearest start
    // otherwise; -1 if none.
    int findSubEntry(double time, bool fuzzyMatch) const;

    SubStyleList &getStyleList() { return styles_; }
    const SubStyleList &getStyleList() const { return styles_; }
    // Point entries whose style is missing from the table to Default.
    void revalidateStyles();
    const SubAttribs &getAttribs() const { return attribs_; }
    void setAttribs(const SubAttribs &a) { attribs_ = a; }
    // Data of the loaded file that only its format family uses (the SSA/ASS
    // header keys, canvas size, extra sections and comment events), kept so
    // that a save in that family writes it back.
    const QMap<QString, QString> &getFormatData() const { return formatData_; }
    void setFormatData(const QMap<QString, QString> &d) { formatData_ = d; }
    void updateQuality();
    SubFile &getSubFile() { return subfile_; }
    const SubFile &getSubFile() const { return subfile_; }
    void setSubFile(const SubFile &s) { subfile_ = s; }
    void setLoadedBytes(const QByteArray &b) { loadedBytes_ = b; }
    const QByteArray &getLoadedBytes() const { return loadedBytes_; }
    bool hasLoadedBytes() const { return !loadedBytes_.isNull(); }
    void releaseLoadedBytes() { loadedBytes_ = QByteArray(); }
    // Replace the entry at `row` (style reset to Default). False if out of range.
    bool replace(const SubEntryPtr &sub, int row);
    bool isTextType() const { return true; }
    // Move rows [start, end] so that they begin at `to`.
    void moveRow(int start, int end, int to);

    // Max finish time in seconds.
    double getMaxTime() const;
    // Append the entries and merge the styles of `newsubs` (loading / joining).
    void appendSubs(const Subtitles &newsubs, bool priorityToNewStyle);

private:
    std::unique_ptr<Subtitles> loadByFileExtension(SubFile &sfile, const QString &data, bool debug);
    std::unique_ptr<Subtitles> loadBySelectedHandler(SubFile &sfile, const QString &data, bool debug);
    std::unique_ptr<Subtitles> loadByPattern(SubFile &sfile, const QString &data, bool debug);
    std::unique_ptr<Subtitles> tryFormatsByPattern(SubFile &sfile, const QString &data, const QString &file, bool debug,
                                                   const AvailSubFormats &formatlist, bool lastResort);

    SubAttribs attribs_;
    QMap<QString, QString> formatData_;
    QList<SubEntryPtr> sublist_;
    SubStyleList styles_;
    SubFile subfile_;
    QByteArray loadedBytes_;
};
