/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.console;

public interface PlayerFeedback {

    /* Volume has been changed (values between 0..1)*/
    public void volumeUpdate(float vol);

    /* The Video Player requested a quit action - i.e. no more streaming */
    public void requestQuit();
}
