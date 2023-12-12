/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * This file is a template for subtitle which contains images.
 *
 * @author Hoang Duy Tran
 */
public interface ImageTypeSubtitle {

    public int getMaxImageHeight();

    public void setMaxImageHeight(int value);

    public BufferedImage getImage();

    public void setImage(BufferedImage img);

    public String getImageFileName();

    public void setImageFileName(String name);

    public File getImageFile();

    public void setImageFile(File imageFile);

    public SubtitleImageAttribute getImageAttribute();

    public void setImageAttribute(SubtitleImageAttribute attrib);
}
