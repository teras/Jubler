/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>

// Subtitle timestamp. Port of the Java `Time`: an integer number of
// milliseconds, clamped to [0, 24h], with -1 meaning "invalid" (a parse
// failure). All arithmetic on an invalid time is a no-op, exactly as in Java.
class Time {
public:
    static constexpr int MAX_TIME = 3600 * 24;              // seconds
    static constexpr int MAX_MILLI_TIME = MAX_TIME * 1000;  // milliseconds

    Time() = default;
    explicit Time(double seconds) { setTime(seconds); }
    // Frame number (as text) at the given frame rate. Invalid on a non-number.
    Time(const QString &frame, float fps);
    // "h", "m", "s", "fraction": the fraction is right-padded with zeros to
    // three digits, so "5" is 500 ms and "05" is 50 ms (Java behaviour).
    Time(const QString &h, const QString &m, const QString &s, const QString &f);
    // "h", "m", "s", "frames" at the given frame rate (frames rounded to ms).
    Time(const QString &h, const QString &m, const QString &s, const QString &f, float fps);

    static Time fromMillis(int ms) { Time t; t.setMilliSeconds(ms); return t; }

    bool isValid() const { return msecs_ >= 0; }

    void addTime(double seconds);
    // Scale around `beg`: t = (t - beg) * fac + beg.
    void recodeTime(double beg, double fac);

    // Like the 4-string constructor but with NO zero padding of the fraction.
    void setTimeLiteral(const QString &h, const QString &m, const QString &s, const QString &f);
    // Java: (int)(time*1000+0.5) — the cast saturates, so clamp in double first.
    void setTime(double seconds) {
        double ms = seconds * 1000.0 + 0.5;
        if (ms > MAX_MILLI_TIME) ms = MAX_MILLI_TIME;
        if (ms < 0) ms = -1;
        setMilliSeconds(int(ms));
    }
    void setTime(const Time &t) { msecs_ = t.msecs_; }

    int compareTo(const Time &t) const { return msecs_ < t.msecs_ ? -1 : (msecs_ > t.msecs_ ? 1 : 0); }
    bool operator<(const Time &o) const { return msecs_ < o.msecs_; }
    bool operator<=(const Time &o) const { return msecs_ <= o.msecs_; }
    bool operator>(const Time &o) const { return msecs_ > o.msecs_; }
    bool operator>=(const Time &o) const { return msecs_ >= o.msecs_; }
    bool operator==(const Time &o) const { return msecs_ == o.msecs_; }
    bool operator!=(const Time &o) const { return msecs_ != o.msecs_; }

    // "HH:MM:SS"
    QString getRoundSeconds() const;
    // "HH:MM:SS<sep>mmm"
    QString getSeconds(QChar milliSep) const;
    // "HH:MM:SS:FF" with FF = round(ms * fps / 1000)
    QString getSecondsFrames(float fps) const;
    // Absolute frame count as text: round(seconds * fps)
    QString getFrames(float fps) const;

    double toSeconds() const { return msecs_ / 1000.0; }
    int getMillis() const { return msecs_; }
    // Java toString(): "HH:MM:SS,mmm"
    QString toString() const { return getSeconds(QLatin1Char(',')); }

    double differenceInSecs(const Time &other) const { return (msecs_ - other.msecs_) / 1000.0; }
    Time difference(const Time &other) const { return Time((msecs_ - other.msecs_) / 1000.0); }

private:
    void invalidate() { msecs_ = -1; }
    void setTime(int h, int m, int s, int f) { setMilliSeconds((h * 3600 + m * 60 + s) * 1000 + f); }
    void setMilliSeconds(int ms);

    int msecs_ = -1;
};
