package com.panayotis.jubler.media.preview.decoders;

import com.panayotis.jubler.plugins.PluginManager;

import java.util.ArrayList;

public class AvailDecoders {

    private final ArrayList<DecoderInterface> decoders;

    public AvailDecoders() {
        decoders = new ArrayList<DecoderInterface>();
        PluginManager.manager.callPluginListeners(this);
    }

    public void addDecoder(DecoderInterface di) {
        decoders.add(di);
    }

    public DecoderInterface get() {
        return decoders.isEmpty()
                ? NullDecoder.INSTANCE
                : decoders.get(0);
    }
}
