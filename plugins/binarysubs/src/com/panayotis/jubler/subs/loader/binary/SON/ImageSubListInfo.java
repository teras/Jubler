/*
 *  ImageSubListInfo.java
 *
 *  Created on: Sep 18, 2009 at 6:07:44 PM
 *
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 *
 */

package com.panayotis.jubler.subs.loader.binary.SON;

import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.ImageTypeSubtitle;
import java.io.File;

/**
 * This class is responsible for checking the content and the status of each
 * subtitle entry in a subtitle list and gathers some basic statistics on them,
 * checking to see if they have subtitle texts only, have images only, have
 * both, or some have and some don't. If entries have images, do they have a
 * file attached to it or not. Having a file means that the file entry must not
 * null and that the file exists and it is not a directory. This will help
 * routines making decision how to treat entries when operations are applied on
 * them.
 *
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public class ImageSubListInfo {

    public Subtitles getSubList() {
        return subList;
    }

    public void setSubList(Subtitles subList) {
        this.subList = subList;
    }

    public int getTestFlag() {
        return testFlag;
    }
    public static int UNDEFINED = 0;
    public static int HAS_TEXT = 1;
    public static int HAS_NO_TEXT = 2;
    public static int HAS_IMAGE = 4;
    public static int HAS_NO_IMAGE = 8;
    public static int IMAGE_HAS_FILE = 16;
    public static int IMAGE_HAS_NO_FILE = 32;
    public static int IMAGE_FILE_EXISTS = 64;
    public static int IMAGE_FILE_NOT_EXISTS = 128;
    private Subtitles subList = null;
    private int testFlag = UNDEFINED;

    public ImageSubListInfo() {
    }

    public ImageSubListInfo(Subtitles subs) {
        this.subList = subs;
    }

    public boolean hasText() {
        boolean yes = (testFlag & HAS_TEXT) > 0;
        return yes;
    }

    public boolean hasNoText() {
        boolean yes = (testFlag & HAS_NO_TEXT) > 0;
        return yes;
    }

    public boolean hasImage() {
        boolean yes = (testFlag & HAS_IMAGE) > 0;
        return yes;
    }

    public boolean hasNoImage() {
        boolean yes = (testFlag & HAS_NO_IMAGE) > 0;
        return yes;
    }

    public boolean imageHasFile() {
        boolean yes = (testFlag & IMAGE_HAS_FILE) > 0;
        return yes;
    }

    public boolean imageHasNoFile() {
        boolean yes = (testFlag & IMAGE_HAS_NO_FILE) > 0;
        return yes;
    }

    public boolean imageFileExists() {
        boolean yes = (testFlag & IMAGE_FILE_EXISTS) > 0;
        return yes;
    }

    public boolean imageFileNotExists() {
        boolean yes = (testFlag & IMAGE_FILE_NOT_EXISTS) > 0;
        return yes;
    }

    public boolean hasTextOnly() {
        boolean yes = this.hasText()
                && (!this.hasNoText()) && // all text presents, no mix
                (!this.hasImage());
        return yes;
    }

    public boolean hasTextAndImage() {
        boolean yes = this.hasText()
                && (!this.hasNoText()) && //all has text, no mix
                this.hasImage()
                && (!this.hasNoImage()); //all has image, no mix
        return yes;
    }

    public boolean hasTextOnlyButMix() {
        boolean yes = this.hasText()
                && this.hasNoText()
                && (!this.hasImage());
        return yes;
    }

    public boolean hasImageOnlyButMix() {
        boolean yes = this.hasImage()
                && this.hasNoImage()
                && this.hasNoText()
                && (!this.hasText());
        return yes;
    }

    public boolean checkInfo() {
        boolean valid = !Share.isEmpty(subList);
        if (!valid)
            return false;

        boolean has_text = false,
                is_image = false,
                has_image = false,
                has_file = false,
                image_file_exists = false;

        for (int i = 0; i < subList.size(); i++) {
            SubEntry entry = subList.elementAt(i);
            has_text = !Share.isEmpty(entry.getText());
            is_image = (entry instanceof ImageTypeSubtitle);
            if (is_image) {
                ImageTypeSubtitle img_sub = (ImageTypeSubtitle) entry;
                has_image = !Share.isEmpty(img_sub.getImage());

                File f = img_sub.getImageFile();
                has_file = (!Share.isEmpty(f));
                image_file_exists = has_file && f.exists() && (!f.isDirectory()) && f.canRead();
            }//end if (is_image)

            if (has_text)
                testFlag |= HAS_TEXT;
            else
                testFlag |= HAS_NO_TEXT;

            if (has_image)
                testFlag |= HAS_IMAGE;
            else
                testFlag |= HAS_NO_IMAGE;

            if (has_file)
                testFlag |= IMAGE_HAS_FILE;
            else
                testFlag |= IMAGE_HAS_NO_FILE;

            if (image_file_exists)
                testFlag |= IMAGE_FILE_EXISTS;
            else
                testFlag |= IMAGE_FILE_NOT_EXISTS;
        }//end for(int i=0; i < subList.size(); i++)
        return true;
    }//end public boolean check()
}//end public class ImageSubListInfo
