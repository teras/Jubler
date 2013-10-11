/*
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

package com.panayotis.jubler.tools;

import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.time.gui.JTimeRegion;
import javax.swing.JComponent;
import static com.panayotis.jubler.i18n.I18N.__;

/**
 *
 * @author teras
 */
public class Fixer extends OneByOneTool {

    private boolean fix;
    private int pushmodel;
    private double min_abs, min_cps, max_abs, max_cps, gap;
    private double push_time;

    public Fixer() {
        super(false, new ToolMenu(__("Time fix"), "TFI", Location.TIMETOOL, 0, 0));
    }

    @Override
    protected JComponent constructToolVisuals() {
        return new FixerGUI();
    }

    @Override
    protected String getToolTitle() {
        return __("Fix time inconsistencies");
    }

    @Override
    protected void storeSelections() {
        FixerGUI vis = (FixerGUI) getToolVisuals();

        /* Sort subtitles first */
        if (vis.SortB.isSelected())
            subtitles.sort(((JTimeRegion) getTimeArea()).getStartTime(), ((JTimeRegion) getTimeArea()).getFinishTime());

        /* What to do with the remaining duration */
        fix = vis.FixT.isSelected();
        if (fix)
            pushmodel = vis.PushModelB.getSelectedIndex();

        min_abs = vis.mintime.getAbsTime();
        min_cps = vis.mintime.getCPSTime();
        max_abs = vis.maxtime.getAbsTime();
        max_cps = vis.maxtime.getCPSTime();

        gap = 0;
        if (vis.GapB.isSelected())
            try {
                gap = Double.parseDouble(vis.GapNum.getText()) / 1000;
            } catch (NumberFormatException e) {
            }

        /* These values are to be used when iterating along the subtitles */
        push_time = 0;
    }

    @Override
    public boolean execute(JubFrame jub) {
        boolean res = super.execute(jub);
        if (res)
            if (((FixerGUI) getToolVisuals()).SortB.isSelected()) {
                subtitles.sort(((JTimeRegion) getTimeArea()).getStartTime(), ((JTimeRegion) getTimeArea()).getFinishTime());
                return true;
            }
        return false;
    }

    protected void affect(SubEntry sub) {
        double curstart; /* The original start time */
        double curdur;  /* The original duration of the subtitle */
        int charcount;  /* The number of characters of the subtitle */
        double mindur, maxdur;  /* minimum & maximum duration of the subtitle */
        double lowerlimit, upperlimit; /* limits dictated by the neighbour subtitles */
        double avail; /* Maximum space available for the subtitle */

        /* Add cumulative changes from older subtitle fixes */
        sub.getStartTime().addTime(push_time);
        sub.getFinishTime().addTime(push_time);

        /* Get current information */
        curstart = sub.getStartTime().toSeconds();
        curdur = sub.getFinishTime().toSeconds() - curstart;
        charcount = sub.getText().length();

        /* initialize minimum/maximum duration */
        mindur = maxdur = -1;
        /* Calculate minimum /maximum duration */
        if (min_abs >= 0)
            mindur = min_abs;
        else if (min_cps >= 0)
            mindur = charcount * min_cps;
        if (max_abs >= 0)
            maxdur = max_abs;
        else if (max_cps >= 0)
            maxdur = charcount * max_cps;

        /* Make sure min & max have a valid value */
        if (mindur < 0 && maxdur < 0)
            mindur = maxdur = curdur;
        else if (mindur < 0)
            mindur = (curdur > maxdur) ? maxdur : curdur;
        else if (maxdur < 0)
            maxdur = (curdur < mindur) ? mindur : curdur;

        /* Fix duration depending on their values */
        if (curdur > maxdur)
            curdur = maxdur;
        if (curdur < mindur)
            curdur = mindur;

        /* If smart duration fix is not wanted, set duration time and exit */
        if (!fix) {
            /* Do not fix time */
            sub.getFinishTime().setTime(curstart + curdur);
            /* That was easy! */
            return;
        }

        /* The following code is executed *only* when the user wants
         * to fix subtitle times
         */

        /* Find limits depending on their neighbour subtitles */
        SubEntry next = getNextEntry();
        SubEntry prev = getPreviousEntry();
        double dt;
        lowerlimit = prev == null ? 0 : prev.getFinishTime().toSeconds();
        upperlimit = next == null ? Time.MAX_TIME : next.getStartTime().toSeconds();

        switch (pushmodel) {
            case 2:
                /* Fix time by pushing the subtitles up */
                sub.getFinishTime().setTime(curstart + curdur);   /* Calculate new finish time */
                dt = curstart + curdur + gap - upperlimit;
                if (dt > 0)
                    /* Ooops, time is not enough, we have to push everything up from now on */
                    push_time += dt;
                break;
            case 1:
                /* Fix time by equally divide overlapped subtitle time */
                if (prev != null) {
                    double timesplit = (lowerlimit - curstart + gap) / 2d;
                    if (timesplit > 0) {
                        sub.getStartTime().setTime(curstart + timesplit);
                        prev.getFinishTime().setTime(lowerlimit - timesplit);
                    }
                }
                break;
            default:
                /* Try to cleverly rearrange the subtitles */
                /* Available space */
                avail = upperlimit - lowerlimit;
                if ((curdur + 2 * gap) <= avail) {
                    /* We have enough space, phewwww... */
                    double fulldur = curdur + 2 * gap;  /* The new full duration, i.e. subtitle + 2 gaps */
                    double center = curstart + (curdur / 2); /* Calculate the old center of the subtitle display */
                    double newstart = center - (fulldur / 2); /* Calculate the new *full* start of the subtitle */
                    double newfinish = center + (fulldur / 2);    /* Calculate the new *full* finish of the subtitle */
                    dt = 0;  /* Initialize deviation of the desired center */
                    if (newfinish > upperlimit)
                        dt = upperlimit - newfinish;   /* Make sure duration does not leak to the right */

                    else if (newstart < lowerlimit)
                        dt = lowerlimit - newstart;   /* Make sure duration does not leak to the left */
                    newstart += dt + gap;   /* Calculate new start WITHOUT the gap */
                    sub.getStartTime().setTime(newstart);
                    sub.getFinishTime().setTime(newstart + curdur);
                } else if (mindur >= avail) {
                    /* We don't have space at all... */
                    sub.getStartTime().setTime(lowerlimit);
                    sub.getFinishTime().setTime(lowerlimit + avail);
                } else {
                    /* We try to evenly distribute the time between the subtitles and the gap */
                    double dcur = curdur - mindur;  /* The difference between current and minimum subtitle */
                    double factor = (avail - mindur) / (dcur + 2 * gap); /* calculate normalization factor */
                    double newdur = (dcur * factor) + mindur; /* Calculate new duration */

                    double newbegin = lowerlimit;    /* Calculate new beginnig */
                    if (prev != null)
                        newbegin += (gap * factor);     /* Add the gap only if it's between subtitles (to fix an error with the first subtitle)
                     /* Fianlly, set net times */
                    sub.getStartTime().setTime(newbegin);
                    sub.getFinishTime().setTime(newbegin + newdur);
                }
        }
    }
}
