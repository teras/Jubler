/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/subs/SubEntry.h"

#include <cmath>
#include <limits>

#include "core/i18n/I18N.h"
#include "core/options/Options.h"
#include "core/util/JavaCompat.h"

const char *const SubEntry::MarkColorKeys[SubEntry::MARK_COUNT] = {"none", "pink", "yellow", "cyan", "orange", "lightgreen"};

QString SubEntry::markName(int mark) {
    switch (mark) {
        case 1: return __("Pink");
        case 2: return __("Yellow");
        case 3: return __("Cyan");
        case 4: return __("Orange");
        case 5: return __("Light Green");
        default: return __("None");
    }
}

SubEntry::SubEntry() : SubEntry(0.0, 0.0, QString(QLatin1String(""))) {}

SubEntry::SubEntry(double start, double finish, const QString &line)
    : start_(start), finish_(finish), subtext_(line) {}

SubEntry::SubEntry(const Time &start, const Time &finish, const QString &line)
    : subtext_(line) {
    start_.setTime(start);
    finish_.setTime(finish);
}

SubEntry::SubEntry(const SubEntry &old) {
    *this = old;
}

SubEntry &SubEntry::operator=(const SubEntry &old) {
    if (this == &old)
        return *this;
    start_ = old.start_;
    finish_ = old.finish_;
    subtext_ = old.subtext_;
    mark_ = old.mark_;
    style_ = old.style_;
    layer_ = old.layer_;
    name_ = old.name_;
    marginL_ = old.marginL_;
    marginR_ = old.marginR_;
    marginV_ = old.marginV_;
    effect_ = old.effect_;
    toolTipText_ = old.toolTipText_;
    fontSizeTexts_ = old.fontSizeTexts_;
    copyOverstyle(old);
    return *this;
}

void SubEntry::copyOverstyle(const SubEntry &old) {
    hasOverstyle_ = old.hasOverstyle_;
    for (int i = 0; i < StyleType::COUNT; ++i) {
        if (old.overstyle_[i]) {
            const auto id = StyleType::Id(i);
            overstyle_[i] = std::make_unique<Styleover>(styleoverKindFor(id), id, styleoverSupported(id));
            overstyle_[i]->updateClone(*old.overstyle_[i]);
        } else
            overstyle_[i].reset();
    }
}

const StyleValue &SubEntry::basicValue(StyleType::Id type) const {
    return style_ ? style_->get(type) : SubStyleList::defaultStyle().get(type);
}

Styleover &SubEntry::styleover(StyleType::Id type) {
    hasOverstyle_ = true;
    if (!overstyle_[type])
        overstyle_[type] = std::make_unique<Styleover>(styleoverKindFor(type), type, styleoverSupported(type));
    return *overstyle_[type];
}

// Values are coerced to the type's representation: the parsers pass what
// they read, and the editors read them back with std::get.
void SubEntry::addOverStyle(StyleType::Id type, const StyleValue &value, int start) {
    styleover(type).add(StyleType::init(type, value), start);
}

void SubEntry::setOverStyle(StyleType::Id type, const StyleValue &value, int start, int end) {
    styleover(type).addEvent(StyleType::init(type, value), start, end, basicValue(type), subtext_);
}

void SubEntry::resetOverStyle() {
    for (auto &o : overstyle_)
        o.reset();
    hasOverstyle_ = false;
}

void SubEntry::cleanupEvents() {
    if (!hasOverstyle_)
        return;
    for (int i = 0; i < StyleType::COUNT - 1; ++i)  // ignore last "unknown" event
        if (overstyle_[i])
            overstyle_[i]->cleanupEvents(basicValue(StyleType::Id(i)), subtext_);
}

std::optional<StyleValue> SubEntry::overValue(StyleType::Id type, int start, int end) const {
    const StyleValue &basic = basicValue(type);
    if (!hasOverstyle_ || !overstyle_[type])
        return basic;
    return overstyle_[type]->getValue(start, end, basic, subtext_);
}

void SubEntry::insertText(int start, int length) {
    if (!hasOverstyle_) return;
    for (auto &o : overstyle_)
        if (o) o->insertText(start, length);
}

void SubEntry::removeText(int start, int length) {
    if (!hasOverstyle_) return;
    for (int i = 0; i < StyleType::COUNT; ++i)
        if (overstyle_[i])
            overstyle_[i]->removeText(start, length, subtext_.length(), basicValue(StyleType::Id(i)), subtext_);
}

void SubEntry::setText(const QString &text) {
    subtext_ = text;
    if (hasOverstyle_) {
        const int textsize = text.length();
        int emptystyles = 0;
        for (auto &o : overstyle_) {
            if (o && !o->setMaxStylePosition(textsize))
                o.reset();
            if (!o)
                ++emptystyles;
        }
        if (emptystyles == StyleType::COUNT)
            hasOverstyle_ = false;
    }
}

QString SubEntry::getData(int row, int col) const {
    switch (col) {
        case 0: return QString::number(row + 1);
        case 1: return start_.toString();
        case 2: return finish_.toString();
        case 3: return getDurationTime().toString();
        case 4: return layer_;
        case 5: return style_ ? style_->toString() : QStringLiteral("?Default");
        case 6: {
            const SubMetrics m = getMetrics();
            const int deltaMs = finish_.getMillis() - start_.getMillis();
            if (deltaMs == 0)
                return QStringLiteral("∞");
            return QString::number(jc::roundJavaL(m.length * 60 / (deltaMs / 1000.0)));
        }
        case 7: {
            const SubMetrics m = getMetrics();
            if (std::isinf(m.cps))
                return QStringLiteral("∞");
            return jc::floatToString(int(m.cps * 10) / 10.0f);
        }
        case 8: {
            QString t = subtext_;
            return t.replace(QLatin1Char('\n'), QLatin1Char('|'));
        }
    }
    return QString();
}

SubMetrics SubEntry::getMetrics() const {
    SubMetrics m;
    int curlinelength = 0;
    int maxlinelength = std::numeric_limits<int>::min();
    int minlinelength = std::numeric_limits<int>::max();
    const bool newlineChars = Options::isNewlineChars();
    const bool spaceChars = Options::isSpaceChars();
    const bool otherChars = Options::isOtherChars();
    for (const QChar item : subtext_) {
        if (item == QLatin1Char('\n')) {
            m.lines++;
            if (newlineChars)
                m.length++;
            if (curlinelength > m.linelength) m.linelength = curlinelength;
            if (maxlinelength < curlinelength) maxlinelength = curlinelength;
            if (minlinelength > curlinelength) minlinelength = curlinelength;
            curlinelength = 0;
        } else if (spaceChars || !jc::isJavaWhitespace(item)) {
            if (otherChars || jc::isLetterOrDigit(item)) {
                m.length++;
                curlinelength++;
            }
        }
    }
    if (curlinelength > m.linelength) m.linelength = curlinelength;
    if (maxlinelength < curlinelength) maxlinelength = curlinelength;
    if (minlinelength > curlinelength) minlinelength = curlinelength;
    // Nothing countable: no line-balance problem (the Java's 0/0 gave 0 — JAVA_BUGS #4).
    m.fillpercent = maxlinelength == 0 ? 100 : int(minlinelength * 100.0f / maxlinelength);
    const long long delta = finish_.getMillis() - start_.getMillis();
    m.cps = delta == 0 ? std::numeric_limits<float>::infinity() : m.length / (delta / 1000.0f);
    return m;
}

void SubEntry::updateQuality() {
    updateQuality(getMetrics());
}

void SubEntry::updateQuality(const SubMetrics &m) {
    const double dur = finish_.differenceInSecs(start_);
    if (m.lines > Options::getMaxLines() || m.cps > Options::getMaxCPS()
        || m.linelength > Options::getMaxLineLength()
        || dur > Options::getMaxDuration() || dur < Options::getMinDuration()
        || m.fillpercent < Options::getFillPercent()
        || (Options::isCompactSubs() && m.length < (m.lines - 1) * Options::getMaxLineLength()))
        setMark(Options::getErrorColor());
    else if (getMark() == Options::getErrorColor())
        setMark(0);   // manual marks are kept; only the error mark is cleared
}

// ---- text helpers -----------------------------------------------------------

bool SubEntry::isHyphenatedWord(const QString &word) {
    return word.endsWith(QLatin1Char('-')) && !word.endsWith(QLatin1String("--"));
}

QString SubEntry::collectWord(const QStringList &list, int from, int to) {
    QString b;
    for (int i = from; i < to; ++i) {
        const QString &word = list.at(i);
        b += word;
        const bool isLast = i == to - 1;
        if (!(isLast || isHyphenatedWord(word)))
            b += QLatin1Char(' ');
    }
    return b;
}

int SubEntry::wordCount(const QString &text) {
    return jc::split(text, jc::reWhiteSp()).size();
}

void SubEntry::mergeRecord(const SubEntry &o) {
    appendText(o.getTextList());
    const int newStart = std::min(start_.getMillis(), o.start_.getMillis());
    const int newEnd = std::max(finish_.getMillis(), o.finish_.getMillis());
    setStartTime(Time(newStart / 1000.0));
    setFinishTime(Time(newEnd / 1000.0));  // Java sets start twice here (JAVA_BUGS #1)
}

SubEntry SubEntry::splitRecord() {
    SubEntry newSub(*this);
    const QStringList list = jc::split(subtext_, jc::reWhiteSp());
    const int len = list.size();
    if (len >= 2) {
        const int mid = len / 2;
        setText(collectWord(list, 0, mid));
        newSub.setText(collectWord(list, mid, len));
    } else
        newSub.setText(QString(QLatin1String("")));
    const int dur = getDurationTime().getMillis();
    const int half = dur / 2;
    const int ts1 = start_.getMillis();
    const int te1 = ts1 + half;
    const int ts2 = te1 + 1;
    const int te2 = finish_.getMillis();
    start_.setTime(ts1 / 1000.0);
    finish_.setTime(te1 / 1000.0);
    newSub.start_.setTime(ts2 / 1000.0);
    newSub.finish_.setTime(te2 / 1000.0);
    return newSub;
}

int SubEntry::getTextLineCount() const {
    return jc::split(subtext_, jc::reNl()).size();
}

QStringList SubEntry::getTextList() const {
    return jc::split(subtext_, jc::reNl());
}

bool SubEntry::setText(const QStringList &lines) {
    setText(lines.join(QLatin1Char('\n')));
    return true;
}

QString SubEntry::getTextWithoutLineBreak() const {
    const QStringList list = jc::split(subtext_, jc::reNl());
    return collectWord(list, 0, list.size());
}

QString SubEntry::getTextLine(int line) const {
    const QStringList list = jc::split(subtext_, jc::reNl());
    if (line < 0 || line >= list.size())
        return QString();
    return list.at(line);
}

bool SubEntry::isTextLineEqual(const SubEntry &other, int line) const {
    const QString a = getTextLine(line), b = other.getTextLine(line);
    if (a.isNull() || b.isNull()) return false;
    return a == b;
}

bool SubEntry::isThisBottomTextLineDuplicatedToOtherTopLine(const SubEntry &other) const {
    const QString a = getTextLine(getTextLineCount() - 1), b = other.getTextLine(0);
    if (a.isNull() || b.isNull()) return false;
    return a == b;
}

QStringList SubEntry::getTextExcludingLine(int line) const {
    QStringList list = getTextList();
    if (line >= 0 && line < list.size())
        list.removeAt(line);
    return list;
}

void SubEntry::addAllText(const QStringList &more) {
    QStringList cur = getTextList();
    cur.append(more);
    setText(cur);
}

bool SubEntry::isSameStartTime(const SubEntry &o) const {
    const int t1 = start_.getMillis(), t2 = o.start_.getMillis();
    return t1 == t2 || std::abs(t2 - t1) < SMALL_MILLIS;
}

bool SubEntry::isSameEndTime(const SubEntry &o) const {
    const int t1 = finish_.getMillis(), t2 = o.finish_.getMillis();
    return t1 == t2 || std::abs(t2 - t1) < SMALL_MILLIS;
}

bool SubEntry::appendText(const QString &line, const QString &separator) {
    bool added = false;
    QString b;
    if (!subtext_.isEmpty()) {
        b += subtext_;
        b += separator;
    }
    if (!line.isEmpty()) {
        b += line;
        added = true;
    }
    setText(b);
    return added;
}

bool SubEntry::isOneWord() const {
    const QString t = getTextWithoutLineBreak();
    return !t.isEmpty() && wordCount(t) == 1;
}

bool SubEntry::appendText(const QStringList &lines) {
    if (lines.size() == 1) {
        const QString &text = lines.first();
        if (wordCount(text) == 1)
            addWord(text);
        else
            addTextLine(text);
    } else
        addAllText(lines);
    return true;
}

bool SubEntry::reSpacingText() {
    const QStringList list = jc::split(subtext_, jc::reSp());
    QString b;
    for (int i = 0; i < list.size(); ++i) {
        b += list.at(i);
        if (!(i == list.size() - 1 || isHyphenatedWord(list.at(i))))
            b += QLatin1Char(' ');
    }
    setText(b);
    return true;
}

bool SubEntry::removeLineBreak(int line) {
    const QStringList list = getTextList();
    QString b;
    for (int i = 0; i < list.size(); ++i) {
        const QString &textLine = list.at(i);
        b += textLine;
        const bool required = i == line;
        const bool last = i == list.size() - 1;
        if (required) {
            if (!(last || isHyphenatedWord(textLine)))
                b += QLatin1Char(' ');
        } else if (!last)
            b += QLatin1Char('\n');
    }
    setText(b);
    return true;
}
