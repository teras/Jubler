/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/tools/CoreTools.h"

#include <QFileInfo>
#include <cmath>

#include "core/i18n/I18N.h"
#include "core/os/Debug.h"
#include "core/os/FileCommunicator.h"
#include "core/tools/ToolHelp.h"
#include "core/util/JavaCompat.h"

// ---- ShiftTime ----------------------------------------------------------------

ShiftTime::ShiftTime() : OneByOneTool(true, ToolMenu{__("Shift time"), QStringLiteral("TSH"), ToolLocation::TIMETOOL}) {}
QString ShiftTime::getToolTitle() const { return __("Shift time by absolute value"); }
QString ShiftTime::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::shift); }

bool ShiftTime::setValues(const TimeSync &first, const TimeSync &) {
    if (std::abs(first.timediff) < 0.001)
        return false;
    shift_ = first.timediff;
    return true;
}

void ShiftTime::affect(SubEntry &sub) {
    sub.getStartTime().addTime(shift_);
    sub.getFinishTime().addTime(shift_);
}

QString ShiftTime::applyToolSpecificArguments(const QMap<QString, QString> &args) {
    const double found = parseDoubleParameter(args, QStringLiteral("delta"));
    if (std::isnan(found))
        return QStringLiteral("Delta is missing");
    shift_ = found;
    return QString();
}

// ---- RecodeTime ---------------------------------------------------------------

RecodeTime::RecodeTime() : OneByOneTool(true, ToolMenu{__("Recode"), QStringLiteral("TCO"), ToolLocation::TIMETOOL}) {}
QString RecodeTime::getToolTitle() const { return __("Recode time"); }
QString RecodeTime::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::recode); }

bool RecodeTime::setValues(const TimeSync &first, const TimeSync &second, double &centerOut, double &factorOut) {
    const TimeSync &t1 = first.smallerThan(second) ? first : second;
    const TimeSync &t2 = first.smallerThan(second) ? second : first;
    const double c = (t2.timediff * t1.timepos - t1.timediff * t2.timepos) / (t2.timediff - t1.timediff);
    if (std::isinf(c) || std::isnan(c))
        return false;
    const double f = (t1.timepos - t2.timepos + t1.timediff - t2.timediff) / (t1.timepos - t2.timepos);
    if (std::isinf(f) || std::isnan(f))
        return false;
    centerOut = c;
    factorOut = f;
    return true;
}

void RecodeTime::affect(SubEntry &sub) {
    sub.getStartTime().recodeTime(center_, factor_);
    sub.getFinishTime().recodeTime(center_, factor_);
}

QString RecodeTime::applyToolSpecificArguments(const QMap<QString, QString> &args) {
    const double fromfps = parseDoubleParameter(args, QStringLiteral("fromfps"));
    const double tofps = parseDoubleParameter(args, QStringLiteral("tofps"));
    if (std::isnan(fromfps) || std::isnan(tofps)) {
        center_ = parseDoubleParameter(args, QStringLiteral("center"));
        factor_ = parseDoubleParameter(args, QStringLiteral("factor"));
        if (std::isnan(center_) || std::isnan(factor_))
            return QStringLiteral("Invalid recode parameters");
    } else {
        if (tofps == 0)
            return QStringLiteral("Invalid recode parameters");
        center_ = 0;
        factor_ = fromfps / tofps;
    }
    return QString();
}

// ---- Synchronize --------------------------------------------------------------

Synchronize::Synchronize() : OneByOneTool(true, ToolMenu{__("Synchronize"), QStringLiteral("TSY"), ToolLocation::FILETOOL}) {}
QString Synchronize::getToolTitle() const { return __("Synchronize"); }
QString Synchronize::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::sync); }

bool Synchronize::affect(QList<SubEntryPtr> &list) {
    if (offset_ < 0)  // walk backwards so earlier copies do not disturb later ones
        std::reverse(list.begin(), list.end());
    return OneByOneTool::affect(list);
}

void Synchronize::affect(SubEntry &sub) {
    if (!model_ || !target_)
        return;
    int idx = -1;
    for (int i = 0; i < target_->size(); ++i)
        if (target_->get(i).get() == &sub) { idx = i; break; }
    const int modid = idx + offset_;
    if (idx < 0 || modid < 0 || modid >= model_->size())
        return;
    const SubEntryPtr from = model_->elementAt(modid);
    if (copytime_) {
        sub.setStartTime(from->getStartTime());
        sub.setFinishTime(from->getFinishTime());
    }
    if (copytext_)
        sub.setText(from->getText());
}

QString Synchronize::applyToolSpecificArguments(const QMap<QString, QString> &args) {
    if (!args.contains(QStringLiteral("sourcesub")))
        return QStringLiteral("Missing source subtitle file");
    model_ = CommandLineContext::getSubtitles(args.value(QStringLiteral("sourcesub")));
    if (!model_)
        return QStringLiteral("Unable to locate sourcesub ") + args.value(QStringLiteral("sourcesub"));
    target_ = CommandLineContext::getSubtitles(QString());
    // Defaults follow the dialog: timing on, text off.
    // An unreadable value is false (Java Boolean.TRUE.equals); only a missing one defaults to true.
    copytime_ = args.contains(QStringLiteral("timestamp")) ? parseBooleanParameter(args, QStringLiteral("timestamp")).value_or(false) : true;
    copytext_ = parseBooleanParameter(args, QStringLiteral("text")).value_or(false);
    offset_ = 0;
    try { offset_ = parseIntParameter(args, QStringLiteral("offset")); } catch (const FilterException &) {}
    return QString();
}

// ---- Fixer --------------------------------------------------------------------

Fixer::Fixer() : OneByOneTool(false, ToolMenu{__("Time fix"), QStringLiteral("TFI"), ToolLocation::TIMETOOL}) {}
QString Fixer::getToolTitle() const { return __("Fix time inconsistencies"); }
QString Fixer::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::fix); }

bool Fixer::affect(QList<SubEntryPtr> &list) {
    pushTime_ = 0;
    return OneByOneTool::affect(list);
}

void Fixer::affect(SubEntry &sub) {
    sub.getStartTime().addTime(pushTime_);
    sub.getFinishTime().addTime(pushTime_);
    const double curstart = sub.getStartTime().toSeconds();
    double curdur = sub.getFinishTime().toSeconds() - curstart;
    // Characters counted as the quality check counts them for its CPS rule.
    const int charcount = sub.getMetrics().length;
    double mindur = -1, maxdur = -1;
    if (minAbs_ >= 0) mindur = minAbs_;
    else if (fastCps_ > 0) mindur = charcount / fastCps_;
    if (maxAbs_ >= 0) maxdur = maxAbs_;
    else if (slowCps_ > 0) maxdur = charcount / slowCps_;
    if (mindur < 0 && maxdur < 0)
        mindur = maxdur = curdur;
    else if (mindur < 0)
        mindur = curdur > maxdur ? maxdur : curdur;
    else if (maxdur < 0)
        maxdur = curdur < mindur ? mindur : curdur;
    if (curdur > maxdur) curdur = maxdur;
    if (curdur < mindur) curdur = mindur;
    if (!fix_) {
        sub.getFinishTime().setTime(curstart + curdur);
        return;
    }
    const SubEntryPtr next = getNextEntry();
    const SubEntryPtr prev = getPreviousEntry();
    const double lowerlimit = prev ? prev->getFinishTime().toSeconds() : 0;
    const double upperlimit = next ? next->getStartTime().toSeconds() : Time::MAX_TIME;
    switch (pushmodel_) {
        case SHIFT: {
            sub.getFinishTime().setTime(curstart + curdur);
            const double dt = curstart + curdur + gap_ - upperlimit;
            if (dt > 0)
                pushTime_ += dt;
            break;
        }
        case DIVIDE:
            if (prev) {
                const double timesplit = (lowerlimit - curstart + gap_) / 2.0;
                if (timesplit > 0) {
                    sub.getStartTime().setTime(curstart + timesplit);
                    prev->getFinishTime().setTime(lowerlimit - timesplit);
                }
            }
            break;
        default: {
            const double avail = upperlimit - lowerlimit;
            if (curdur + 2 * gap_ <= avail) {
                const double fulldur = curdur + 2 * gap_;
                const double center = curstart + curdur / 2;
                double newstart = center - fulldur / 2;
                const double newfinish = center + fulldur / 2;
                double dt = 0;
                if (newfinish > upperlimit) dt = upperlimit - newfinish;
                else if (newstart < lowerlimit) dt = lowerlimit - newstart;
                newstart += dt + gap_;
                sub.getStartTime().setTime(newstart);
                sub.getFinishTime().setTime(newstart + curdur);
            } else if (mindur >= avail) {
                sub.getStartTime().setTime(lowerlimit);
                sub.getFinishTime().setTime(lowerlimit + avail);
            } else {
                const double dcur = curdur - mindur;
                const double factor = (avail - mindur) / (dcur + 2 * gap_);
                const double newdur = dcur * factor + mindur;
                double newbegin = lowerlimit;
                if (prev)
                    newbegin += gap_ * factor;  // gap only between subtitles (not before the first)
                sub.getStartTime().setTime(newbegin);
                sub.getFinishTime().setTime(newbegin + newdur);
            }
        }
    }
}

QString Fixer::applyToolSpecificArguments(const QMap<QString, QString> &args) {
    auto opt = [&](const QString &k) { const double v = parseDoubleParameter(args, k); return std::isnan(v) ? -1 : v; };
    minAbs_ = opt(QStringLiteral("mintime"));
    maxAbs_ = opt(QStringLiteral("maxtime"));
    // maxcps (reading no faster than) bounds the duration from below, mincps
    // (no slower than) from above; an absolute time on the same side wins.
    fastCps_ = opt(QStringLiteral("maxcps"));
    slowCps_ = opt(QStringLiteral("mincps"));
    if (minAbs_ >= 0) fastCps_ = -1;
    if (maxAbs_ >= 0) slowCps_ = -1;
    if (args.contains(QStringLiteral("overlap"))) {
        const QString overlap = args.value(QStringLiteral("overlap"));
        if (overlap == QLatin1String("distribute")) pushmodel_ = DISTRIBUTE;
        else if (overlap == QLatin1String("divide")) pushmodel_ = DIVIDE;
        else if (overlap == QLatin1String("shift")) pushmodel_ = SHIFT;
        else return QStringLiteral("Invalid overlap value: ") + overlap;
        gap_ = parseDoubleParameter(args, QStringLiteral("gap"));
        if (std::isnan(gap_)) gap_ = 0;
        fix_ = true;
    } else {
        fix_ = false;
        gap_ = 0;
    }
    return QString();
}

// ---- Rounder ------------------------------------------------------------------

Rounder::Rounder() : OneByOneTool(true, ToolMenu{__("Round time"), QStringLiteral("TRO"), ToolLocation::TIMETOOL}) {}
QString Rounder::getToolTitle() const { return __("Round timing"); }
QString Rounder::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::round); }

void Rounder::setDecimals(int decimals) {
    precise_ = int(std::pow(10, std::max(0, std::min(3, decimals))));
}

void Rounder::roundTime(Time &t, int precise) {
    const double r = jc::roundJavaL(t.toSeconds() * precise);
    t.setTime(r / precise);
}

void Rounder::affect(SubEntry &sub) {
    roundTime(sub.getStartTime(), precise_);
    roundTime(sub.getFinishTime(), precise_);
}

QString Rounder::applyToolSpecificArguments(const QMap<QString, QString> &args) {
    const int decimals = parseIntParameter(args, QStringLiteral("decimals"));
    if (decimals < 0) return QStringLiteral("Invalid decimals parameter, must be >= 0");
    if (decimals > 3) return QStringLiteral("Invalid decimals parameter, must be <= 3");
    precise_ = int(std::pow(10, decimals));
    return QString();
}

// ---- Marker -------------------------------------------------------------------

Marker::Marker() : OneByOneTool(true, ToolMenu{__("By Selection"), QStringLiteral("EMS"), ToolLocation::MARK}) {}
QString Marker::getToolTitle() const { return __("Mark region"); }
QString Marker::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::mark); }

QString Marker::applyToolSpecificArguments(const QMap<QString, QString> &args) {
    mark_ = parseMarkParam(args, QStringLiteral("mark"));
    if (mark_ < 0)
        return QStringLiteral("Invalid mark parameter: ") + args.value(QStringLiteral("mark"));
    return QString();
}

// ---- DelSelection -------------------------------------------------------------

DelSelection::DelSelection() : OneByOneTool(true, ToolMenu{__("By selection"), QStringLiteral("EDS"), ToolLocation::DELETE}) {}
QString DelSelection::getToolTitle() const { return __("Delete selection"); }
QString DelSelection::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::delete_); }

void DelSelection::affect(SubEntry &sub) {
    if (!subtitles_) return;
    for (int i = 0; i < subtitles_->size(); ++i)
        if (subtitles_->get(i).get() == &sub) { subtitles_->remove(i); return; }
}

// ---- Styler -------------------------------------------------------------------

Styler::Styler() : OneByOneTool(true, ToolMenu{__("By selection"), QStringLiteral("ESS"), ToolLocation::STYLE}) {}
QString Styler::getToolTitle() const { return __("Set region style"); }
QString Styler::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::style); }

QString Styler::applyToolSpecificArguments(const QMap<QString, QString> &args) {
    style_ = parseStyleParam(args, CommandLineContext::getSubtitles(QString()), QStringLiteral("style"));
    if (!style_)
        return QStringLiteral("Unable to find style named ") + args.value(QStringLiteral("style"));
    return QString();
}

// ---- JoinEntries --------------------------------------------------------------

JoinEntries::JoinEntries() : TimeBaseTool(true, ToolMenu{__("Join entries"), QStringLiteral("TJE"), ToolLocation::CONTENTTOOL, Qt::Key_Equal, Qt::ControlModifier}) {}
QString JoinEntries::getToolTitle() const { return __("Join entries"); }
QString JoinEntries::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::jointext); }

bool JoinEntries::affect(QList<SubEntryPtr> &list) {
    if (list.isEmpty() || !subtitles_)
        return true;
    const SubEntryPtr first = list.first();
    first->setFinishTime(list.last()->getFinishTime());
    QString text = first->getText();
    for (int i = 1; i < list.size(); ++i) {
        text += QLatin1Char('\n') + list.at(i)->getText();
        subtitles_->remove(list.at(i));
    }
    first->setText(text);
    return true;
}

// ---- SplitEntries -------------------------------------------------------------

SplitEntries::SplitEntries() : OneByOneTool(true, ToolMenu{__("Split entries"), QStringLiteral("TSE"), ToolLocation::CONTENTTOOL, Qt::Key_Minus, Qt::ControlModifier}) {}
QString SplitEntries::getToolTitle() const { return __("Split entries"); }
QString SplitEntries::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::splittext); }

void SplitEntries::affect(SubEntry &sub) {
    if (!subtitles_) return;
    const QStringList tokens = sub.getText().split(QLatin1Char('\n'), Qt::SkipEmptyParts);
    if (tokens.isEmpty())
        return;  // nothing to split; one line with breaks ("abc\n") is trimmed and shortened, as the Java
    const double delta = (sub.getFinishTime().toSeconds() - sub.getStartTime().toSeconds()) / sub.getText().length();
    Subtitles newsubs;
    double from = sub.getStartTime().toSeconds();
    for (const QString &t : tokens) {
        const double upto = from + delta * t.length();
        auto e = std::make_shared<SubEntry>(from, upto, t);
        e->setStyle(sub.getStyle());
        newsubs.add(e);
        from = upto + 0.001;
    }
    sub.setStartTime(newsubs.elementAt(0)->getStartTime());
    sub.setFinishTime(newsubs.elementAt(0)->getFinishTime());
    sub.setText(newsubs.elementAt(0)->getText());
    newsubs.remove(0);
    SubEntryPtr self;
    for (const SubEntryPtr &e : subtitles_->entries())
        if (e.get() == &sub) { self = e; break; }
    subtitles_->insertSubs(self, newsubs);
}

// ---- RegExpReplace ------------------------------------------------------------

RegExpReplace::RegExpReplace() : OneByOneTool(false, std::nullopt) {}
QString RegExpReplace::getToolTitle() const { return __("Regular Expression replace"); }
QString RegExpReplace::getCommandLineHelp() const {
    return QStringLiteral("Perform regular expression-based find and replace operations on subtitle text (format: regex:pattern=…:replace=…)");
}

bool RegExpReplace::setRules(const QList<QPair<QString, QString>> &rules, QString *error) {
    patterns_.clear();
    replacements_.clear();
    for (const auto &r : rules) {
        QRegularExpression re(r.first);
        if (!re.isValid()) {
            if (error) *error = re.errorString();
            patterns_.clear();
            replacements_.clear();
            return false;
        }
        const QString refError = checkReplacement(re, r.second);
        if (!refError.isNull()) {
            if (error) *error = refError;
            patterns_.clear();
            replacements_.clear();
            return false;
        }
        patterns_.append(re);
        replacements_.append(r.second);
    }
    return true;
}

// The references Java's Matcher refuses (an exception there): a trailing
// "\\" or "$", "$" not followed by a group, a group that does not exist.
QString RegExpReplace::checkReplacement(const QRegularExpression &re, const QString &replacement) {
    const int groups = re.captureCount();
    const QStringList names = re.namedCaptureGroups();
    for (int i = 0; i < replacement.length(); ++i) {
        const QChar c = replacement[i];
        if (c == QLatin1Char('\\')) {
            if (i + 1 >= replacement.length()) return __("character to be escaped is missing");
            ++i;
        } else if (c == QLatin1Char('$')) {
            if (i + 1 >= replacement.length()) return __("Illegal group reference: group index is missing");
            const QChar n = replacement[i + 1];
            if (n == QLatin1Char('{')) {
                const int close = replacement.indexOf(QLatin1Char('}'), i + 2);
                const QString name = close < 0 ? QString() : replacement.mid(i + 2, close - i - 2);
                static const QRegularExpression valid(QStringLiteral("^[A-Za-z][A-Za-z0-9]*$"));
                if (close < 0 || !valid.match(name).hasMatch()) return __("named capturing group is missing trailing '}'");
                if (!names.contains(name)) return __("No group with name {0}", QStringLiteral("{%1}").arg(name));
                i = close;
            } else if (n >= QLatin1Char('0') && n <= QLatin1Char('9')) {
                if (n.digitValue() > groups) return __("No group {0}", n.digitValue());
                ++i;
            } else
                return __("Illegal group reference");
        }
    }
    return QString();
}

// Java Matcher.replaceAll semantics: "$n"/"${name}" insert a group, "\\x"
// inserts x literally; an unknown group inserts nothing.
QString RegExpReplace::replaceAll(const QString &text, const QRegularExpression &re, const QString &replacement) {
    QString out;
    int last = 0;
    auto it = re.globalMatch(text);
    while (it.hasNext()) {
        const auto m = it.next();
        out += text.mid(last, m.capturedStart() - last);
        for (int i = 0; i < replacement.length(); ++i) {
            const QChar c = replacement[i];
            if (c == QLatin1Char('\\') && i + 1 < replacement.length()) {
                out += replacement[++i];
            } else if (c == QLatin1Char('$') && i + 1 < replacement.length()) {
                if (replacement[i + 1] == QLatin1Char('{')) {
                    const int close = replacement.indexOf(QLatin1Char('}'), i + 2);
                    if (close > i + 2) {
                        out += m.captured(replacement.mid(i + 2, close - i - 2));
                        i = close;
                        continue;
                    }
                    out += c;
                } else if (replacement[i + 1].isDigit()) {
                    // The longest group number the pattern has (Java rule); an
                    // unmatched group inserts nothing.
                    const int groups = re.captureCount();
                    int j = i + 1, group = replacement[j].digitValue();
                    while (j + 1 < replacement.length() && replacement[j + 1].isDigit() && group * 10 + replacement[j + 1].digitValue() <= groups) {
                        ++j;
                        group = group * 10 + replacement[j].digitValue();
                    }
                    if (group <= groups) out += m.captured(group);
                    i = j;
                } else
                    out += c;
            } else
                out += c;
        }
        last = m.capturedEnd();
        if (m.capturedLength() == 0 && last < text.length()) {
            out += text[last];
            ++last;
        }
    }
    out += text.mid(last);
    return out;
}

void RegExpReplace::affect(SubEntry &sub) {
    QString res = sub.getText();
    for (int i = 0; i < patterns_.size(); ++i)
        res = replaceAll(res, patterns_[i], replacements_[i]);
    sub.setText(res);
}

QString RegExpReplace::applyToolSpecificArguments(const QMap<QString, QString> &args) {
    if (!args.contains(QStringLiteral("pattern"))) return QStringLiteral("Missing pattern");
    if (!args.contains(QStringLiteral("replace"))) return QStringLiteral("Missing replacement");
    QString err;
    if (!setRules({{args.value(QStringLiteral("pattern")), args.value(QStringLiteral("replace"))}}, &err))
        return QStringLiteral("Invalid pattern: ") + err;
    return QString();
}

// ---- SubSplit -----------------------------------------------------------------

SubSplit::SubSplit() : Tool(ToolMenu{__("Split file"), QStringLiteral("TSP"), ToolLocation::FILETOOL}) {}
QString SubSplit::getToolTitle() const { return __("Split subtitles in two"); }
QString SubSplit::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::split); }

void SubSplit::split(Subtitles &source, double at, Subtitles &first, Subtitles &second) {
    first = Subtitles(source.getSubFile());
    first.getSubFile().appendToFilename(QStringLiteral("_1"));
    first.getStyleList() = source.getStyleList();
    first.setAttribs(source.getAttribs());
    first.setFormatData(source.getFormatData());
    second = Subtitles(source.getSubFile());
    second.getSubFile().appendToFilename(QStringLiteral("_2"));
    second.getStyleList() = source.getStyleList();
    second.setAttribs(source.getAttribs());
    second.setFormatData(source.getFormatData());
    for (const SubEntryPtr &csub : source.entries()) {
        if (csub->getStartTime().toSeconds() < at)
            first.add(csub);
        else {
            csub->getStartTime().addTime(-at);
            csub->getFinishTime().addTime(-at);
            second.add(csub);
        }
    }
    first.revalidateStyles();
    second.revalidateStyles();
}

QString SubSplit::executeParams(const QMap<QString, QString> &params, bool) {
    double at;
    try { at = parseDoubleParameter(params, QStringLiteral("at")); } catch (const FilterException &e) { return e.message(); }
    if (std::isnan(at))
        return QStringLiteral("Invalid at parameter: ") + params.value(QStringLiteral("at"));
    Subtitles *source = CommandLineContext::getSubtitles(QString());
    if (!source)
        return QStringLiteral("No default subtitle file loaded");
    Subtitles first, second;
    const SubFile keep = source->getSubFile();
    split(*source, at, first, second);
    *source = first;
    source->getSubFile() = keep;   // the CLI keeps editing the same document, no "_1" rename
    if (params.contains(QStringLiteral("target"))) {
        SubFile sf(params.value(QStringLiteral("target")), SubFile::EXTENSION_GIVEN);
        sf.setEncoding(source->getSubFile().getEncoding());
        sf.setFPS(source->getSubFile().getFPS());
        const QString ext = QFileInfo(sf.getSaveFile()).suffix();
        if (SubFormatPtr f = Availabilities::formats().findFromExtension(ext))
            sf.setFormat(f);
        else
            sf.setFormat(source->getSubFile().getFormat());
        const QString err = FileCommunicator::save(second, sf, nullptr);
        if (!err.isNull())
            return err;
    }
    return QString();
}

// ---- SubJoin ------------------------------------------------------------------

SubJoin::SubJoin() : Tool(ToolMenu{__("Join files"), QStringLiteral("TJO"), ToolLocation::FILETOOL}) {}
QString SubJoin::getToolTitle() const { return __("Join two subtitles"); }
QString SubJoin::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::join); }

QString SubJoin::executeParams(const QMap<QString, QString> &params, bool) {
    Subtitles *current = CommandLineContext::getSubtitles(QString());
    if (!current)
        return QStringLiteral("No default subtitle file loaded");
    Subtitles *other = CommandLineContext::getSubtitles(params.value(QStringLiteral("append")));
    if (!other || !params.contains(QStringLiteral("append")))
        return QStringLiteral("Unable to locate other subtitle file");
    double dt;
    try { dt = parseDoubleParameter(params, QStringLiteral("gap")); } catch (const FilterException &e) { return e.message(); }
    if (std::isnan(dt)) dt = 0;
    const double offset = dt + current->getMaxTime();
    for (const SubEntryPtr &it : other->entries()) {
        auto se = std::make_shared<SubEntry>(*it);
        se->getStartTime().addTime(offset);
        se->getFinishTime().addTime(offset);
        current->add(se);
    }
    current->revalidateStyles();
    return QString();
}

// ---- Reparent -----------------------------------------------------------------

Reparent::Reparent() : Tool(ToolMenu{__("Reparent"), QStringLiteral("TPA"), ToolLocation::FILETOOL}) {}
QString Reparent::getToolTitle() const { return __("Reparent subtitles file"); }

// ---- AddSubtitle --------------------------------------------------------------

QString AddSubtitle::getToolTitle() const { return __("Add subtitle"); }
QString AddSubtitle::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::add); }

QString AddSubtitle::executeParams(const QMap<QString, QString> &params, bool debug) {
    Subtitles *subs = CommandLineContext::getSubtitles(QString());
    if (!subs)
        return QStringLiteral("No subtitles loaded");
    try {
        const double start = parseDoubleParameter(params, QStringLiteral("start"));
        if (std::isnan(start)) return QStringLiteral("Missing start time parameter");
        const double end = parseDoubleParameter(params, QStringLiteral("end"));
        if (std::isnan(end)) return QStringLiteral("Missing end time parameter");
        const QString text = params.value(QStringLiteral("text"));
        if (text.trimmed().isEmpty()) return QStringLiteral("Missing text parameter");
        if (start >= end) return QStringLiteral("Start time must be less than end time");
        if (start < 0) return QStringLiteral("Start time cannot be negative");
        SubStylePtr style;
        if (!params.value(QStringLiteral("style")).trimmed().isEmpty()) {
            style = parseStyleParam(params, subs, QStringLiteral("style"));
            if (!style)
                return QStringLiteral("Style '%1' not found").arg(params.value(QStringLiteral("style")));
        } else if (subs->getStyleList().size())
            style = subs->getStyleList().get(0);
        const int mark = parseMarkParam(params, QStringLiteral("mark"));
        QString processed = text;
        processed.replace(QLatin1String("\\n"), QLatin1String("\n")).replace(QLatin1String("\\t"), QLatin1String("\t")).replace(QLatin1String("\\\\"), QLatin1String("\\"));
        auto entry = std::make_shared<SubEntry>(start, end, processed);
        if (style) entry->setStyle(style);
        if (mark >= 0) entry->setMark(mark);
        subs->addSorted(entry);
        if (debug)
            Debug::debug(QStringLiteral("Added subtitle: %1s-%2s: %3").arg(start).arg(end).arg(processed));
        return QString();
    } catch (const FilterException &e) {
        return e.message();
    }
}

// ---- SortSubtitles ------------------------------------------------------------

QString SortSubtitles::getToolTitle() const { return __("Sort subtitles"); }
QString SortSubtitles::getCommandLineHelp() const { return QString::fromUtf8(ToolHelp::sort); }

QString SortSubtitles::executeParams(const QMap<QString, QString> &params, bool debug) {
    Subtitles *subs = CommandLineContext::getSubtitles(QString());
    if (!subs)
        return QStringLiteral("No subtitles loaded");
    try {
        double startTime = 0.0, endTime = std::numeric_limits<double>::max();
        const double ps = parseDoubleParameter(params, QStringLiteral("start"));
        if (!std::isnan(ps)) startTime = ps;
        const double pe = parseDoubleParameter(params, QStringLiteral("end"));
        if (!std::isnan(pe)) endTime = pe;
        if (startTime < 0) return QStringLiteral("Start time cannot be negative");
        if (endTime <= startTime) return QStringLiteral("End time must be greater than start time");
        if (endTime == std::numeric_limits<double>::max() && subs->size() > 0)
            endTime = subs->getMaxTime();  // the whole document
        const int originalSize = subs->size();
        subs->sort(startTime, endTime);
        if (debug)
            Debug::debug(QStringLiteral("Sorted subtitles in time range %1s-%2s (%3 entries)").arg(startTime).arg(endTime).arg(originalSize));
        return QString();
    } catch (const FilterException &e) {
        return e.message();
    }
}

// ---- registration -------------------------------------------------------------

void registerSpellAndTranslateTools(ToolRegistry &reg);  // tools/SpellTranslateTools.cpp

void registerBuiltinTools(ToolRegistry &reg) {
    // Java CoreTools order (Speller and Translate are registered by their
    // own modules, in the same slots: after Rounder).
    reg.add(std::make_shared<SubSplit>());
    reg.add(std::make_shared<SubJoin>());
    reg.add(std::make_shared<Reparent>());
    reg.add(std::make_shared<Synchronize>());
    reg.add(std::make_shared<ShiftTime>());
    reg.add(std::make_shared<RecodeTime>());
    reg.add(std::make_shared<Fixer>());
    reg.add(std::make_shared<Rounder>());
    registerSpellAndTranslateTools(reg);
    reg.add(std::make_shared<JoinEntries>());
    reg.add(std::make_shared<SplitEntries>());
    reg.add(std::make_shared<DelSelection>());
    reg.add(std::make_shared<Marker>());
    reg.add(std::make_shared<Styler>());
    reg.add(std::make_shared<AddSubtitle>());
    reg.add(std::make_shared<SortSubtitles>());
    reg.add(std::make_shared<RegExpReplace>());
}
