package com.panayotis.jubler.media.preview.decoders.ffmpeg;

import com.panayotis.jubler.media.preview.decoders.AvailDecoders;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;

public class FFMPEGPlugin extends SystemDependent implements Plugin, PluginItem<AvailDecoders> {
    @Override
    public PluginItem[] getPluginItems() {
        return new PluginItem[]{this};
    }

    @Override
    public String getPluginName() {
        return "Embedded FFMPEG";
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public void setClassLoader(ClassLoader cl) {

    }

    @Override
    public Class<AvailDecoders> getPluginAffection() {
        return AvailDecoders.class;
    }

    @Override
    public void execPlugin(AvailDecoders decoders, Object parameter) {
        decoders.addDecoder(new FFMPEG());
    }

    public static String[] playAudioCommand(String filePath) {
        return IS_MACOSX
                ? new String[]{"afplay", filePath}
                : new String[]{"ffplay", "-i", filePath, "-nodisp", "-autoexit", "-hide_banner"};
    }
}
