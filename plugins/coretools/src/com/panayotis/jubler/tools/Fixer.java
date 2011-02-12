/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.time.gui.JTimeRegion;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class Fixer extends RegionTool {

    private boolean fix;
    private int pushmodel;
    private double min_abs, min_cps, max_abs, max_cps, gap;

    public Fixer() {
        super(false, new ToolMenu(_("Time fix"), null, "TFI", null));
    }

    @Override
    protected ToolGUI constructToolVisuals() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected String getToolTitle() {
        return _("Fix time inconsistencies");
    }

    protected void storeSelections() {
        FixerGUI vis = (FixerGUI) getVisuals();

        /* Sort subtitles first */
        if (vis.SortB.isSelected())
            subs.sort(((JTimeRegion) getTimeArea()).getStartTime(), ((JTimeRegion) getTimeArea()).getFinishTime());

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
    }

    @Override
    public boolean execute(JubFrame jub) {
        boolean res = super.execute(jub);
        if (res)
            if (((FixerGUI) getVisuals()).SortB.isSelected()) {
                subs.sort(((JTimeRegion) getTimeArea()).getStartTime(), ((JTimeRegion) getTimeArea()).getFinishTime());
                return true;
            }
        return false;
    }

    protected void affect(int index) {
        SubEntry sub = affected_list.get(index);

        double curstart; /* The original start time */
        double curdur;  /* The original duration of the subtitle */
        int charcount;  /* The number of characters of the subtitle */
        double mindur, maxdur;  /* minimum & maximum duration of the subtitle */
        double lowerlimit, upperlimit; /* limits dictated by the neighbour subtitles */
        double avail; /* Maximum space available for the subtitle */

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

        /* The following part of the code is executed *only* when the user wanted
        to fix the subtitle times
         */

        /* Find limits depending on their neighbour subtitles */
        if (index == 0)
            lowerlimit = 0;
        else
            lowerlimit = affected_list.get(index - 1).getFinishTime().toSeconds();
        if (index == (affected_list.size() - 1))
            upperlimit = Time.MAX_TIME;
        else
            upperlimit = affected_list.get(index + 1).getStartTime().toSeconds();

        /* Fix time by pushing the subtitles up */
        if (pushmodel == 2) {
            sub.getFinishTime().setTime(curstart + curdur);   /* Calculate new finish time */
            double dt = curstart + curdur + gap - upperlimit;
            if (dt > 0) {
                /* Ooops, time is not enough, we have to push everything up from now on */
                SubEntry uppersub;
                for (int i = index + 1; i < affected_list.size(); i++) {
                    uppersub = affected_list.get(i);
                    uppersub.getStartTime().addTime(dt);
                    uppersub.getFinishTime().addTime(dt);
                }
            }
            return;
        }

        /* Fix time by equally divide overlapped subtitle time */
        if (pushmodel == 1) {
            if (index > 0) {
                double timesplit = (lowerlimit - curstart + gap) / 2d;
                if (timesplit > 0) {
                    sub.getStartTime().setTime(curstart + timesplit);
                    affected_list.get(index - 1).getFinishTime().setTime(lowerlimit - timesplit);
                }
            }
            return;
        }


        /* Try to cleverly rearrange the subtitles */

        /* Available space */
        avail = upperlimit - lowerlimit;

        /* The real work starts here */
        if ((curdur + 2 * gap) <= avail) {
            /* We have enough space, phewwww... */
            double fulldur = curdur + 2 * gap;  /* The new full duration, i.e. subtitle + 2 gaps */
            double center = curstart + (curdur / 2); /* Calculate the old center of the subtitle display */
            double newstart = center - (fulldur / 2); /* Calculate the new *full* start of the subtitle */
            double newfinish = center + (fulldur / 2);    /* Calculate the new *full* finish of the subtitle */
            double dt = 0;  /* Initialize deviation of the desired center */
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
            if (index > 0)
                newbegin += (gap * factor);     /* Add the gap only if it's between subtitles (to fix an error with the first subtitle)
            /* Fianlly, set net times */
            sub.getStartTime().setTime(newbegin);
            sub.getFinishTime().setTime(newbegin + newdur);
        }
    }
}
