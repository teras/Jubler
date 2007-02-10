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
    private long secs = -1;
    private short milli = -1;
    
    public static final long MAX_TIME=3600*24;
    
    
    /* Time in seconds */
    public Time(double time) {
        setTime(time);
    }
    
    
    /* Time in frames & FPS */
    public Time(String frame, float fps) {
        try {
            setTime(Double.parseDouble(frame) / fps);
        } catch ( NumberFormatException e) {
            invalidate();
        }
    }
    
    /* Time in hours, minutes, seconds & milliseconds */
    public Time(String h, String m, String s, String f) {
        setTime(h, m, s, f);
    }
    
    public Time(Time time) {
        setTime(time);
    }
    
    
    
    
    public boolean isValid() {
        return (secs >= 0);
    }
    
    private void invalidate() {
        secs = milli = -1;
    }
    
    
    public void addTime(double d) {
        if (!isValid()) return;
        setTime(toSeconds()+d);
    }
    
    public void recodeTime(double beg, double fac) {
        if (!isValid()) return;
        setTime( (toSeconds()-beg)*fac + beg );
    }
    
    private void setTime(String h, String m, String s, String f) {
        short hour, min, sec, milli;
        int flength;
        try {
            hour = Short.parseShort(h);
            min = Short.parseShort(m);
            sec = Short.parseShort(s);
            flength = f.length();
            if (flength<3) 
                f = f + "000".substring(flength);
            milli = Short.parseShort(f);
            setTime(hour, min, sec, milli);
        } catch ( NumberFormatException e) {
            invalidate();
        }
    }
    
    public void setTime(double time) {
        long ltime = (long)(time+0.0000005);
        setTime(ltime, (short)Math.round((time-ltime)*1000d));
    }
    
    private void setTime(short h, short m, short s, short f) {
        long ftime = h*3600 + m*60 + s;
        setTime(ftime, f);
    }
    
    private void setTime(long time, short mil) {
        if ( time < 0 ) {
            secs = 0;
            milli = 0;
            return;
        }
        
        if (mil < 0) mil = 0;
        if (mil>=1000) {
            time += mil/1000;
            mil %= 1000;
        }
        
        secs = time;
        milli = mil;
        if (secs >= MAX_TIME) {
            secs = MAX_TIME-1;
            milli = 999;
        }
    }
    
    public void setTime(Time time) {
        secs = time.secs;
        milli = time.milli;
    }
    
    
    
    public int compareTo(Time t) {
        if (secs < t.secs) return -1;
        if (secs > t.secs) return 1;
        if (milli < t.milli) return -1;
        if (milli > t.milli) return 1;
        return 0;
    }
    
    
    public String toString() {
        StringBuffer res;
        short hour, min, sec, mil;
        long time;
        
        res = new StringBuffer();
        time = secs;
        
        hour = (short)(time/3600);
        time -= hour * 3600;
        min = (short)(time/60);
        time -= min * 60;
        sec = (short)time;
        
        if ( hour < 10) res.append("0");
        res.append(hour);
        res.append(":");
        if (min < 10) res.append("0");
        res.append(min);
        res.append(":");
        if (sec < 10) res.append("0");
        res.append(sec);
        res.append(",");
        if (milli < 100) res.append("0");
        if (milli < 10 ) res.append("0");
        res.append(milli);
        return res.toString();
    }
    
    public double toSeconds() {
        double ret = (double)secs;
        ret += milli/1000d;
        return ret;
    }
    
    public long toFrame(float FPS) {
        return Math.round(toSeconds() * FPS);
    }
    
    
}
