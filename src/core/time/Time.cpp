/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/time/Time.h"

#include <cmath>

namespace {
// Java Short.parseShort semantics: optional sign, decimal digits, range of a
// 16-bit short. Anything else is a NumberFormatException → invalid time.
bool parseShort(const QString &s, int &out) {
    bool ok = false;
    const int v = s.toInt(&ok, 10);
    if (!ok || v < -32768 || v > 32767)
        return false;
    out = v;
    return true;
}

QString two(int v) {
    return v < 10 ? QLatin1Char('0') + QString::number(v) : QString::number(v);
}
}  // namespace

Time::Time(const QString &frame, float fps) {
    bool ok = false;
    const double f = frame.toDouble(&ok);
    if (ok && fps > 0)
        setTime(f / fps);
    else
        invalidate();
}

Time::Time(const QString &h, const QString &m, const QString &s, const QString &f) {
    int hour, min, sec, milli;
    QString frac = f;
    if (frac.length() < 3)
        frac += QString(3 - frac.length(), QLatin1Char('0'));
    if (parseShort(h, hour) && parseShort(m, min) && parseShort(s, sec) && parseShort(frac, milli))
        setTime(hour, min, sec, milli);
    else
        invalidate();
}

Time::Time(const QString &h, const QString &m, const QString &s, const QString &f, float fps) {
    int hour, min, sec, fram;
    if (fps > 0 && parseShort(h, hour) && parseShort(m, min) && parseShort(s, sec) && parseShort(f, fram))
        setTime(hour, min, sec, int(std::lround(fram * 1000.0f / fps)));
    else
        invalidate();
}

void Time::setTimeLiteral(const QString &h, const QString &m, const QString &s, const QString &f) {
    int hour, min, sec, milli;
    if (parseShort(h, hour) && parseShort(m, min) && parseShort(s, sec) && parseShort(f, milli))
        setTime(hour, min, sec, milli);
    else
        invalidate();
}

void Time::addTime(double seconds) {
    if (!isValid())
        return;
    setTime(toSeconds() + seconds);
}

void Time::recodeTime(double beg, double fac) {
    if (!isValid())
        return;
    setTime((toSeconds() - beg) * fac + beg);
}

void Time::setMilliSeconds(int ms) {
    if (ms < 0)
        ms = 0;
    if (ms > MAX_MILLI_TIME)
        ms = MAX_MILLI_TIME;
    msecs_ = ms;
}

QString Time::getRoundSeconds() const {
    int time = msecs_ / 1000;
    const int sec = time % 60;
    time /= 60;
    const int min = time % 60;
    const int hour = time / 60;
    return two(hour) + QLatin1Char(':') + two(min) + QLatin1Char(':') + two(sec);
}

QString Time::getSeconds(QChar milliSep) const {
    QString res = getRoundSeconds();
    const int milli = msecs_ % 1000;
    res += milliSep;
    if (milli < 100)
        res += QLatin1Char('0');
    if (milli < 10)
        res += QLatin1Char('0');
    res += QString::number(milli);
    return res;
}

QString Time::getSecondsFrames(float fps) const {
    const int milli = msecs_ % 1000;
    int frm = int(std::lround(milli * fps / 1000.0f));
    // A frame that rounds up to the frame count of a second belongs to the
    // next second ("…:01:25" at 25 fps is "…:02:00"); the Java wrote it as is.
    if (fps > 0 && frm >= int(std::ceil(fps - 0.001f)) && msecs_ - milli + 1000 <= MAX_MILLI_TIME) {
        Time next(*this);
        next.msecs_ = msecs_ - milli + 1000;
        return next.getRoundSeconds() + QLatin1String(":00");
    }
    return getRoundSeconds() + QLatin1Char(':') + two(frm);
}

QString Time::getFrames(float fps) const {
    return QString::number(int(std::lround(toSeconds() * fps)));
}
