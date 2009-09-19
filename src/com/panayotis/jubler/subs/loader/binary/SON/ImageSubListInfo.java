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
 * subtitle entry in a subtitle list and gathers some basic statistics on
 * them, checking to see if they have subtitle texts only, have images only,
 * have both, or some have and some don't. If entries have images, do they
 * have a file attached to it or not. Having a file means that the file entry
 * must not null and that the file exists and it is not a directory. This
 * will help routines making decision how to treat entries when operations
 * are applied on them.
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public class ImageSubListInfo {

    public Subtitles getSubList() {
        return subList;
    }

    public void setSubList(Subtitles subList) {
        this.subList = subList;
    }

    public ImageSubAttribute getHasWhat() {
        return hasWhat;
    }

    public void setHasWhat(ImageSubAttribute hasWhat) {
        this.hasWhat = hasWhat;
    }

    public ImageSubAttribute getHasImageFile() {
        return hasImageFile;
    }

    public void setHasImageFile(ImageSubAttribute hasImageFile) {
        this.hasImageFile = hasImageFile;
    }

    public static enum ImageSubAttribute {

        UNDEFINED,
        HAS_TEXT_ONLY,
        HAS_IMAGE_ONLY,
        HAS_TEXT_AND_IMAGE,
        MIXED_TEXT_AND_IMAGE,
        IMAGES_HAVE_FILES,
        IMAGES_DONOT_HAVE_FILES,
        SOME_IMAGES_HAVE_FILES_SOME_DONT
    }

    private Subtitles subList =null;
    private ImageSubAttribute hasWhat = ImageSubAttribute.UNDEFINED;
    private ImageSubAttribute hasImageFile = ImageSubAttribute.UNDEFINED;

    public ImageSubListInfo() {
    }

    public ImageSubListInfo(Subtitles subs) {
        this.subList = subs;
    }

    public boolean checkInfo() {
        boolean valid = !Share.isEmpty(subList);
        if (!valid) {
            return false;
        }

        boolean has_text, is_image, has_image, has_file;
        has_text = has_image = has_file = is_image = false;
        for (int i = 0; i < subList.size(); i++) {
            SubEntry entry = subList.elementAt(i);
            has_text = !Share.isEmpty(entry.getText());
            is_image = (entry instanceof ImageTypeSubtitle);
            if (is_image) {
                ImageTypeSubtitle img_sub = (ImageTypeSubtitle) entry;
                has_image = !Share.isEmpty(img_sub.getImage());
                
                File f = img_sub.getImageFile();
                has_file = (!Share.isEmpty(f)) && f.exists() && (!f.isDirectory());
            }//end if (is_image)

            switch (hasWhat) {
                case UNDEFINED:
                    if (has_text && has_image)
                        hasWhat = ImageSubAttribute.HAS_TEXT_AND_IMAGE;
                    else if (has_text)
                        hasWhat = ImageSubAttribute.HAS_TEXT_ONLY;
                    else if (has_image)
                        hasWhat = ImageSubAttribute.HAS_IMAGE_ONLY;
                    break;
                case HAS_TEXT_ONLY:
                    if (has_image)
                        hasWhat = ImageSubAttribute.MIXED_TEXT_AND_IMAGE;
                    break;
                case HAS_IMAGE_ONLY:
                    if (has_text)
                        hasWhat = ImageSubAttribute.MIXED_TEXT_AND_IMAGE;
                    break;
                case HAS_TEXT_AND_IMAGE:
                    if ((has_text && !has_image) || (has_image && !has_text))
                        hasWhat = ImageSubAttribute.MIXED_TEXT_AND_IMAGE;                        
                    break;
                default:
                    break;
            }//end switch(hasWhat)
            
            switch(hasImageFile){
                case UNDEFINED:
                    if (has_file)
                        hasImageFile = ImageSubAttribute.IMAGES_HAVE_FILES;
                    else
                        hasImageFile = ImageSubAttribute.IMAGES_DONOT_HAVE_FILES;
                    break;
                case IMAGES_DONOT_HAVE_FILES:
                    if (has_file)
                        hasImageFile = ImageSubAttribute.SOME_IMAGES_HAVE_FILES_SOME_DONT;
                    break;
                case IMAGES_HAVE_FILES:
                    if (!has_file)
                        hasImageFile = ImageSubAttribute.SOME_IMAGES_HAVE_FILES_SOME_DONT;
                    break;
                default:
                    break;
            }//end switch(hasImageFile)
        }//end for(int i=0; i < subList.size(); i++)
        return true;
    }//end public boolean check()
}//end public class ImageSubListInfo
