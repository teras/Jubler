/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.preview.decoders.AudioPreview;
import com.panayotis.jubler.media.preview.decoders.DecoderListener;
import com.panayotis.jubler.subs.SubEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import static com.panayotis.jubler.media.preview.JFramePreview.DT;
import static com.panayotis.jubler.os.UIUtils.scale;

public class JWavePreview extends JPanel implements DecoderListener {

    private final static Color[] background = {new Color(0, 20, 0), new Color(0, 20, 20)};
    private final static Color bordercolor = Color.WHITE;
    private final static Color basecolor = Color.LIGHT_GRAY;
    private WavePanel[] panels;
    private final JSubTimeline timeline;
    private static final AudioPreview demoaudio = new AudioPreview(1, 1000);
    private MediaFile mfile;
    private final JAudioLoader loader;
    private double start_time = -1, end_time = -1;
    /* Whether the waveform in these panels will be maximized or not */
    private boolean is_maximized = false;

    /**
     * Creates a new instance of JWavePreview
     */
    public JWavePreview(JSubTimeline tline) {
        timeline = tline;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        loader = new JAudioLoader();

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                timeline.mouseDragged(e);
            }

            public void mouseMoved(MouseEvent e) {
                timeline.mouseMoved(e);
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                timeline.mousePressed(e);
            }

            public void mouseReleased(MouseEvent e) {
                timeline.mouseReleased(e);
            }
        });

        addMouseWheelListener(timeline::mouseWheelUpdates);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(scale(50), scale(50));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(scale(10), scale(20));
    }

    public void startCacheCreation() {
        add(loader);
        loader.setVisible(true);
        loader.setValue(0);
        setEnabled(false);
    }

    public void stopCacheCreation() {
        remove(loader);
        loader.setVisible(false);
        setEnabled(true);
        updateWave();
    }

    public void updateCacheCreation(float state) {
        loader.setValue((int) (state * 100));
    }

    public void updateMediaFile(MediaFile mfile) {
        /*  start creation of cache files */
        this.mfile = mfile;
        loader.updateMediaFile(mfile);
    }

    public void setTime(double start, double end) {
        if (Math.abs(start - start_time) < DT && Math.abs(end - end_time) < DT) {
            repaint();
            return;
        }
        start_time = start;
        end_time = end;
        updateWave();
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateWave();
    }

    public void setMaximized(boolean maximized) {
        is_maximized = maximized;
        updateWave();
    }

    private void updateWave() {
        AudioPreview audio;
        if (isEnabled() && mfile != null)
            audio = mfile.getAudioPreview(start_time, end_time);
        else
            audio = null;
        if (audio == null)
            audio = demoaudio;
        if (is_maximized)
            audio.normalize();

        /* Remove old panels */
        if (panels != null)
            for (WavePanel panel : panels)
                if (panel != null)
                    remove(panel);
        /* Create new panels */
        panels = new WavePanel[audio.channels()];
        for (int i = 0; i < panels.length; i++) {
            panels[i] = new WavePanel(audio.getChannel(i), background[i % 2]);
            add(panels[i]);
        }
        validate();
    }

    public void playbackWave() {
        if (timeline.getSelectedList().isEmpty())
            return;
        if (mfile == null)
            return;

        int which = timeline.getSelectedList().get(0).pos;
        SubEntry entry = timeline.getJubler().getSubtitles().elementAt(which);
        mfile.playAudioClip(entry.getStartTime().toSeconds(), entry.getFinishTime().toSeconds());
    }

    private class WavePanel extends JPanel {

        private final float[][] data;
        private final Color c;

        public WavePanel(float[][] data, Color c) {
            this.data = data;
            this.c = c;
        }

        public void paintComponent(Graphics g) {
            int width = getWidth();
            int height = getHeight();

            /* Draw channel box */
            g.setColor(c);
            g.fillRect(0, 0, width - 1, height - 1);

            /* Draw selected boxes */
            if (timeline != null) {
                g.setColor(JSubTimeline.SelectColor);
                for (SubInfo i : timeline.getSelectedList()) {
                    double sstart, send;
                    if (i.startPercent > i.endPercent) {
                        sstart = i.endPercent;
                        send = i.startPercent;
                    } else {
                        sstart = i.startPercent;
                        send = i.endPercent;
                    }
                    g.fill3DRect((int) (sstart * width), 0, (int) ((send - sstart) * width), height, false);
                }
            }

            /* Draw lines */
            g.setColor(bordercolor);
            g.drawRect(0, 0, width - 1, height - 1);
            g.setColor(basecolor);
            g.drawLine(1, height / 2, width - 1, height / 2);

            int x1, x2, y1, y2, yswap;
            float factor = ((float) width) / data.length;
            for (int i = 0; i < data.length; i++) {
                x1 = (int) (factor * i);
                x2 = (int) (factor * (i + 1));
                y1 = (int) (height * data[i][0]);
                y2 = (int) (height * data[i][1]);
                if (y1 > y2) {
                    yswap = y1;
                    y1 = y2;
                    y2 = yswap;
                }
                g.fillRect(x1, y1, x2 - x1, y2 - y1);
            }
        }
    }
}
