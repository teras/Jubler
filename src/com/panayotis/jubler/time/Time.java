/*
 * SubTime.java
 *
 * Created on 22 Ιούνιος 2005, 5:15 μμ
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.panayotis.jubler.time;

/**
 *
 * @author teras
 */
public class Time implements Comparable<Time> {

    protected int msecs = -1;
    public static final int MAX_TIME = 3600 * 24;   // in seconds
    public static final int MAX_MILLI_TIME = MAX_TIME * 1000;   // in seconds

    public Time(int msecs){
        this.setMilli(msecs);
    }
    /* Time in seconds */
    public Time(double time) {
        setTime(time);
    }

    /* Time in frames & FPS */
    public Time(String frame, float fps) {
        try {
            setTime(Double.parseDouble(frame) / fps);
        } catch (NumberFormatException e) {
            invalidate();
        }
    }

    /* Time in hours, minutes, seconds & milliseconds */
    public Time(String h, String m, String s, String f) {
        setTime(h, m, s, f);
    }

    /* Time in hours, minutes, seconds & frames */
    public Time(String h, String m, String s, String f, float fps) {
        setTime(h, m, s, f, fps);
    }

    public Time(Time time) {
        setTime(time);
    }

    public void addTime(double d) {
        if (!isValid())
            return;
        setTime(toSeconds() + d);
    }

    public void recodeTime(double beg, double fac) {
        if (!isValid())
            return;
        setTime((toSeconds() - beg) * fac + beg);
    }

    private void setTime(String h, String m, String s, String f, float fps) {
        short hour, min, sec, milli, fram;
        try {
            hour = Short.parseShort(h);
            min = Short.parseShort(m);
            sec = Short.parseShort(s);
            fram = Short.parseShort(f);
            milli = (short) Math.round(fram * 1000f / fps);
            setTime(hour, min, sec, milli);
        } catch (NumberFormatException e) {
            invalidate();
        }
    }

    private void setTime(String h, String m, String s, String f) {
        short hour, min, sec, milli;
        int flength;
        try {
            hour = Short.parseShort(h);
            min = Short.parseShort(m);
            sec = Short.parseShort(s);
            flength = f.length();
            if (flength < 3)
                f = f + "000".substring(flength);
            milli = Short.parseShort(f);
            setTime(hour, min, sec, milli);
        } catch (NumberFormatException e) {
            invalidate();
        }
    }

    public boolean isValid() {
        return msecs >= 0;
    }

    private void invalidate() {
        msecs = -1;
    }

    public void setTime(double time) {
        msecs = (int)(time * 1000d+0.5d);
    }

    public void setTime(Time time) {
        msecs = time.msecs;
    }

    private void setTime(short h, short m, short s, short f) {
        msecs = (h * 3600 + m * 60 + s) * 1000 + f;
    }

    private void setMilliSeconds(int msecs) {
        if (msecs < 0)
            msecs = 0;
        if (msecs > MAX_MILLI_TIME)
            msecs = MAX_MILLI_TIME;
        this.setMilli(msecs);
    }

    public int compareTo(Time t) {
        if (msecs < t.msecs)
            return -1;
        if (msecs > t.msecs)
            return 1;
        return 0;
    }

    public String getSeconds() {
        StringBuffer res;
        int hour, min, sec, milli;

        res = new StringBuffer();
        int time;
        milli = msecs % 1000;
        time = msecs / 1000;
        sec = time % 60;
        time /= 60;
        min = time % 60;
        time /= 60;
        hour = time;

        if (hour < 10)
            res.append("0");
        res.append(hour);
        res.append(":");
        if (min < 10)
            res.append("0");
        res.append(min);
        res.append(":");
        if (sec < 10)
            res.append("0");
        res.append(sec);
        res.append(",");
        if (milli < 100)
            res.append("0");
        if (milli < 10)
            res.append("0");
        res.append(milli);
        return res.toString();
    }

    public String getSecondsFrames(float FPS) {
        StringBuffer res;
        int hour, min, sec, milli, frm;

        res = new StringBuffer();
        int time;
        milli = msecs % 1000;
        time = msecs / 1000;
        sec = time % 60;
        time /= 60;
        min = time % 60;
        time /= 60;
        hour = time;

        if (hour < 10)
            res.append("0");
        res.append(hour);
        res.append(":");
        if (min < 10)
            res.append("0");
        res.append(min);
        res.append(":");
        if (sec < 10)
            res.append("0");
        res.append(sec);
        res.append(":");

        frm = Math.round(milli * FPS / 1000f);
        if (frm < 10)
            res.append("0");
        res.append(frm);
        return res.toString();
    }

    public String getFrames(float FPS) {
        return Integer.toString((int) Math.round(toSeconds() * FPS));
    }

    public double toSeconds() {
        return msecs / 1000d;
    }
    public String toString() {
        return getSeconds();
    }

    /**
     * @return the msecs
     */
    public int getMilli() {
        return msecs;
    }

    /**
     * @param msecs the msecs to set
     */
    public void setMilli(int msecs) {
        this.msecs = msecs;
    }
}
