/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader.gui;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.Subtitles;

/**
 * Load accessory: empty. Encoding and FPS are no longer chosen up-front — the file is auto-detected
 * on load and corrected afterwards through the encoding bar ({@link JEncodingBar}). Kept as a no-op
 * so the dialog code path stays uniform; {@code JSubFileDialog} skips it because it has no content.
 */
public class JLoadOptions extends JFileOptions {

    public void updateVisuals(Subtitles subs, MediaFile mfile) {
        /* nothing to show on load */
    }
}
