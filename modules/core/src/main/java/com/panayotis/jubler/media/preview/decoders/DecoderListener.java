/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.preview.decoders;

public interface DecoderListener {

    /* Start creation of cache file */
    public void startCacheCreation();

    /* Finish creation of cache file */
    public void stopCacheCreation();

    /* Update the status of cache */
    public void updateCacheCreation(float position);
}
