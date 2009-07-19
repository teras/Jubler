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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author teras
 */
public class Time implements Comparable<Time> {

    public static double PAL_VIDEOFRAMERATE = 3600.0;
    public static double NTSC_VIDEOFRAMERATE = 3003.0;
    public static DateFormat time_format_1 = new SimpleDateFormat("HH:mm:ss.SSS");
    public static DateFormat time_format_2 = new SimpleDateFormat("HH:mm:ss:SSS");
    public static DateFormat time_format_3 = new SimpleDateFormat("dd.MM.yy  HH:mm");
    public static DateFormat time_format_4 = new SimpleDateFormat("HH:mm:ss");
    protected int msecs = -1;
    public static final int MAX_TIME = 3600 * 24;   // in seconds
    public static final int MAX_MILLI_TIME = MAX_TIME * 1000;   // in seconds

    /**
     * This is to generate end-time, using starting frame-count and 
     * the duration in the 10th.
     * @param frames The starting time in frame-count.
     * @param frame_rate The frame-rate.
     * @param duration The duration 
     */
    public Time(long frames, long frame_rate, int duration) {
        this(frames, frame_rate);
        this.msecs += (duration * 10);
    }
    /**
     * This is to generate the start-time from a frame-count, that is
     * says 25 frames/second, 1500 frames/minute and 90,000 frames/hour.
     * By dividing the frames to 90, the value is suitable for
     * Date, which uses milliseconds, however the millisecond part needs
     * to convert by (mill * 90 / frame_rate) to get the time-equipvalent.
     * @param frames The frame count.
     * @param frame_rate The frame-rate, ie. 36000
     */
    public Time(long frames, long frame_rate) {
        frames /= 90;
        String time_s = formatTime(frames, frame_rate, true);        
        try {
            Date dt = time_format_2.parse(time_s);
            this.msecs = (int)dt.getTime();            
        } catch (Exception ex) {
        }
    }

    public Time(int msecs) {
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
        if (!isValid()) {
            return;
        }
        setTime(toSeconds() + d);
    }

    public void recodeTime(double beg, double fac) {
        if (!isValid()) {
            return;
        }
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

    public void setTimeLiteral(String h, String m, String s, String f) {
        short hour, min, sec, milli;
        int flength;
        try {
            hour = Short.parseShort(h);
            min = Short.parseShort(m);
            sec = Short.parseShort(s);
            milli = Short.parseShort(f);
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
            if (flength < 3) {
                f = f + "000".substring(flength);
            }
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
        msecs = (int) (time * 1000d + 0.5d);
    }

    public void setTime(Time time) {
        msecs = time.msecs;
    }

    private void setTime(short h, short m, short s, short f) {
        msecs = (h * 3600 + m * 60 + s) * 1000 + f;
    }

    private void setMilliSeconds(int msecs) {
        if (msecs < 0) {
            msecs = 0;
        }
        if (msecs > MAX_MILLI_TIME) {
            msecs = MAX_MILLI_TIME;
        }
        this.setMilli(msecs);
    }

    public int compareTo(Time t) {
        if (msecs < t.msecs) {
            return -1;
        }
        if (msecs > t.msecs) {
            return 1;
        }
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

        if (hour < 10) {
            res.append("0");
        }
        res.append(hour);
        res.append(":");
        if (min < 10) {
            res.append("0");
        }
        res.append(min);
        res.append(":");
        if (sec < 10) {
            res.append("0");
        }
        res.append(sec);
        res.append(",");
        if (milli < 100) {
            res.append("0");
        }
        if (milli < 10) {
            res.append("0");
        }
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

        if (hour < 10) {
            res.append("0");
        }
        res.append(hour);
        res.append(":");
        if (min < 10) {
            res.append("0");
        }
        res.append(min);
        res.append(":");
        if (sec < 10) {
            res.append("0");
        }
        res.append(sec);
        res.append(":");

        frm = Math.round(milli * FPS / 1000f);
        if (frm < 10) {
            res.append("0");
        }
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

    /**
     * Converts the time that includes the frame-count in the time-value by
     * extracts the millisecond part and convert it appropriately to 
     * time unit. The time input must have been divided by 90, 
     * (ie. taken from 90,000 frames per hour), before passing to this
     * routine.
     * @param time_value The time includes the frame-count.
     * @param frame_rate The frame-rate for TV system used.
     * @param is_to_time true if it is converted to time, false back to frames.
     * @return The string representing time format, suitable to be parsed
     * by a DateFormat instance.
     */
    private String formatTime(long time_value, long frame_rate, boolean is_to_time) {
        time_format_2.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));
        String time_str = time_format_2.format(new Date(time_value));

        int time_len = time_str.length();
        int sub_time_len = time_len - 3;

        String time_sub_str = time_str.substring(0, sub_time_len);

        int n1 = Integer.parseInt(time_str.substring(time_str.length() - 3));

        int milli_part = 0;
        if (is_to_time) {
            milli_part = n1 * 90 / (int) frame_rate;
        } else {
            milli_part = n1 * (int) frame_rate / 90;
        }

        return (time_sub_str + milli_part);
    }

    /**
     * This routine return the number of frames/second, given that the the
     * internal milliseconds used to store this value. This takes the
     * default 90,000 frames per hour, and since it was stored in the
     * milliseconds, the value stored will be multiplied by 90.
     * @param frame_rates The frame-rate, ie. PAL=36000, NTSC=3003
     * @return The frame-count per second equivalent of the millisecond stored.
     */
    public int getFrames(long frame_rates){
        int frame_count = 0;
        try{
            String time_s = this.formatTime(msecs, frame_rates, false);
            Date dt = time_format_2.parse(time_s);
            frame_count = (int)(dt.getTime() * 90L);            
        }catch(Exception ex){}
        return frame_count;
    }
    /**
     * Inserting zeros into the back of the value input, based on the 
     * total length required. If the length of the original is equal or longer
     * than the total length required, the original is returned.
     * @param str The value that need patching with zeros at the back.
     * @param len the total length of the result string. 
     * @return The string patched with zeros if needed, and the length of it
     * must have a minimum required length.
     */
    private String adaptString(String str, int len) {
        StringBuffer strbuf = new StringBuffer(str.trim());

        while (strbuf.length() < len) {
            strbuf.insert(0, "0");
        }

        return strbuf.toString();
    }

    /**
     * Inserting zeros into the back of the value input, based on the 
     * total length required. If the length of the original is equal or longer
     * than the total length required, the original is returned.
     * @param str The value that need patching with zeros at the back.
     * @param len the total length of the result string. 
     * @return The string patched with zeros if needed, and the length of it
     * must have a minimum required length.
     */
    private String adaptString(int str, int len) {
        return adaptString(String.valueOf(str), len);
    }
}
