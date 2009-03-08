/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.subs.loader.gui;

import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.options.gui.JRateChooser;
import com.panayotis.jubler.subs.loader.AvailSubFormats;
import com.panayotis.jubler.subs.loader.SubFormat;

/**
 *
 * @author teras
 */
public class FileOptions {

    private float LoadFPS;
    private String[] LoadEncodings = new String[3];
    private float SaveFPS;
    private String SaveEncoding;
    private SubFormat SaveFormat;

    public FileOptions() {
        /* Load */
        for (int i = 0; i < LoadEncodings.length; i++)
            setLoadEncodings(i, Options.getOption("Load.Encoding" + (i + 1), null));
        setLoadFPS(Options.getOption("Load.FPS", null));
        /* Save */
        setSaveEncoding (Options.getOption("Save.Encoding", null));
        setSaveFPS(Options.getOption("Save.FPS", null));
        setSaveFormat(Options.getOption("Save.Format", null));
    }

    public void saveOptions() {
        System.out.println("KOKO");
        /* Load */
        for (int i = 0; i < LoadEncodings.length; i++)
            Options.setOption("Load.Encoding" + (i + 1), getLoadEncoding(i));
        Options.setOption("Load.FPS", Float.toString(getLoadFPS()));

        /* Save */
        Options.setOption("Save.Encoding", getSaveEncoding());
        Options.setOption("Save.FPS", Float.toString(getSaveFPS()));
        Options.setOption("Save.Format", getSaveFormat().getName());

        Options.saveOptions();
    }

    public String getLoadEncoding(int index) {
        return LoadEncodings[index];
    }

    public String[] getLoadEncodings() {
        return LoadEncodings;
    }

    public void setLoadEncodings(String[] LoadEnc) {
        for (int i = 0; i < LoadEncodings.length; i++) {
            if (LoadEnc == null)
                setLoadEncodings(i, null);
            else
                setLoadEncodings(i, LoadEnc[i]);
        }
    }

    public void setLoadEncodings(int index, String LoadEncoding) {
        if (LoadEncoding == null)
            LoadEncoding = JPreferences.DefaultEncodings[index];
        LoadEncodings[index] = LoadEncoding;
    }

    public float getLoadFPS() {
        return LoadFPS;
    }

    public void setLoadFPS(String fps) {
        try {
            LoadFPS = Float.parseFloat(fps);
        } catch (Exception ex) {
            LoadFPS = Float.parseFloat(JRateChooser.DefaultFPSEntry);
        }
    }

    public float getSaveFPS() {
        return SaveFPS;
    }

    public void setSaveFPS(String fps) {
        try {
            SaveFPS = Float.parseFloat(fps);
        } catch (Exception ex) {
            SaveFPS = Float.parseFloat(JRateChooser.DefaultFPSEntry);
        }
    }

    public SubFormat getSaveFormat() {
        return SaveFormat;
    }

    public void setSaveFormat(String format) {
        SaveFormat = AvailSubFormats.findFromDescription(format);
        if (SaveFormat == null)
            SaveFormat = AvailSubFormats.findFromName(format);
        if (SaveFormat == null)
            SaveFormat = AvailSubFormats.Formats[0];
    }

    public String getSaveEncoding() {
        return SaveEncoding;
    }

    public void setSaveEncoding(String SaveEncoding) {
        if (SaveEncoding == null)
            SaveEncoding = JPreferences.DefaultEncodings[0];
        this.SaveEncoding = SaveEncoding;
    }
}
