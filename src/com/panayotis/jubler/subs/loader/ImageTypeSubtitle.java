/*
 * ImageTypeSubtitle.java
 *
 * Created on 25 November 2008, 00:42 am
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
 */

package com.panayotis.jubler.subs.loader;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.ImageIcon;

/**
 * This file is a template for subtitle which contains images.
 * @author Hoang Duy Tran
 */
public interface ImageTypeSubtitle {

    public int getMaxImageHeight();
    public ImageIcon getImage();
    public void setImage(ImageIcon img);
    public File getImageFile();
    public void setImageFile(File imageFile);
}
