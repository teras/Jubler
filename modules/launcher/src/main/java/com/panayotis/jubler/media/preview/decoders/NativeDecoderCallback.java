/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview.decoders;

public interface NativeDecoderCallback {
    boolean getInterruptStatus();

    void updateViewport(float position);
}
