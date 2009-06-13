/*
 * SubFormat.java
 *
 * Created on 13 Ιούλιος 2005, 7:44 μμ
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

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;

/**
 *
 * @author teras
 */
public abstract class SubFormat {

    protected float FPS;
    protected String ENCODING;

    public abstract String getExtension();

    public abstract String getName();

    public final boolean produce(Subtitles subs, File outfile, JPreferences prefs, MediaFile media) throws IOException {
        if (prefs == null) {
            FPS = 25f;
            ENCODING = "UTF-8";
        } else {
            FPS = prefs.getSaveFPS();
            ENCODING = prefs.getSaveEncoding();
        }
        return produce(subs, outfile, media);
    }

    public void init() {
    }

    /* Export subtitles to file
     * Return whether the file should be moved & renamed or not
     */
    public abstract boolean produce(Subtitles subs, File outfile, MediaFile media) throws IOException;

    public String getExtendedName() {
        return getName();
    }

    public String getDescription() {
        return getExtendedName() + "  (*." + getExtension() + ")";
    }

    public String getEncoding() {
        return ENCODING;
    }

    /* convert a string into subtitles */
    public abstract Subtitles parse(String input, float FPS, File f);

    public abstract boolean supportsFPS();

    public static short parseShort(String data) {
        return parseShort(data, (short) 0);
    }

    public static short parseShort(String data, short default_value) {
        short value = default_value;
        try {
            value = Short.parseShort(data);
        } catch (Exception ex) {
        }
        return value;
    }

    public static int parseInt(String data) {
        return parseInt(data, 0);
    }

    public static int parseInt(String data, int default_value) {
        int value = default_value;
        try {
            value = Integer.parseInt(data);
        } catch (Exception ex) {
        }
        return value;
    }

    /**
     * Using the ImageIO to read an image from a file into the internal buffer,
     * And then create a image icon which is stored in the image record and update the
     * internal image dimension if this image is larger than the previous one.
     * The image, after loaded, is cropped and transformed to a transparent 
     * image where the DVBT transparency colours are excluded.
     * @param f the image file being read
     * @return the image record if reading was successfully carried out, Null if reading's failed.
     */
    public static ImageIcon readImage(File f) {
        BufferedImage img, sub_img, tran_image = null;
        ImageIcon ico = null;
        try {
            img = ImageIO.read(f);
            boolean is_empty = (img == null);
            if (is_empty) {
                return null;
            } else {
                int w = img.getWidth();
                int h = img.getHeight();
                /**
                 * make the transparent image first
                 */
                tran_image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

                /**
                 * Whilst running the loop to find boundary, also update the
                 * transparent image with desired pixel, ignoring the colour
                 * that are transparent.
                 */
                Rectangle sr = getSubImageDimension(
                        img,
                        Share.DVBT_SUB_TRANSPARENCY,
                        tran_image);
                /**
                 * now crop the transparent image down to the size obtained.
                 */
                sub_img = tran_image.getSubimage(
                        sr.x,
                        sr.y,
                        sr.width,
                        sr.height);

                //BufferedImage comp_image = compressImage(sub_img, 0.1f);
                /**
                 * Converts the cropped image into the image-icon for displaying
                 */
                ico = new ImageIcon(sub_img);
                return ico;
            }//end if
        } catch (Exception ex) {
            return null;
        }
    }//public static ImageIcon readImage(File f)

    /**
     * This routine gets the sub-image boundary using the specified colour
     * that will be treated as transparent within an existing image. It 
     * assumes a transparent image 'dest' exists and write the pixels that
     * are not transparent to that image whilst searching for the boundary.
     * This routine cuts down an additional expensive loop running through
     * the source image to find the matching colour, as previously implemented
     * using separated routines, one to find the boundary, another to make
     * the transparent image. This routine combine the 2 functions together
     * in one solution. In order to use this routine, a preparation for 
     * transparent image must be done in advance, something like:
     * <pre>
     * int w = source_img.getWidth();
     * int h = source_img.getHeight();
     * 
     * BufferedImage tran_image = 
     *      new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
     * 
     * Rectangle rec = getSubImageDimension(
     *      source_img, some_color, tran_image );
     * 
     * BufferedImage sub_img = tran_image.getSubimage(
     *      sr.x,
     *      sr.y,
     *      sr.width,
     *      sr.height);
     * </pre>
     * The final 'sub_imag' is the cropped image and contain transparency, where
     * all instances of 'some_color' are removed (transparent).
     * @param source The non-transparent image that will be used to find the
     * sub-image boundary.
     * @param find_colour The colour that will be treated as transparent.
     * @param dest The transparent image, whose size is the same as the source
     * image, and whose content is blank.
     * @return The dimension of sub-image where it will be cropped to, plus
     * the destination image being filled with content that are NOT transparent.
     */
    public static Rectangle getSubImageDimension(
            BufferedImage source,
            Color find_colour,
            BufferedImage dest) {
        Rectangle rec = null;
        int lx, ly, rx, ry;
        int img_w, img_h, w, h;
        try {
            img_w = source.getWidth();
            img_h = source.getHeight();

            /**
             * Assuming the largest value first.
             */
            lx = img_w;
            ly = img_h;
            /**
             * Assuming the smallest value first.
             */
            rx = 0;
            ry = 0;

            /**
             * search top-left down to bottom-right and find the excluding 
             * colour. The smaller value is held.
             */
            for (int x = 0; x < img_w; x++) {
                for (int y = 0; y < img_h; y++) {
                    int img_rgb = (source.getRGB(x, y));
                    Color img_colour = new Color(img_rgb);
                    boolean is_same = (find_colour.equals(img_colour));
                    if (!is_same) {
                        if (x < lx) {
                            lx = x;
                        }
                        if (y < ly) {
                            ly = y;
                        }
                        
                        if (x > rx) {
                            rx = x;
                        }
                        if (y > ry) {
                            ry = y;
                        }
                        
                        /*copy the non-transparent pixels to new image*/
                        dest.setRGB(x, y, img_rgb);
                    }//end if
                }//end for (int col = 0; col < img_h; col++)
            }//end for(int row = 0; row < img_w; row++)

            /**
             * Calculate the width and height
             */
            w = (rx - lx);
            h = (ry - ly);
            rec = new Rectangle(lx, ly, w, h);
        } catch (Exception ex) {
        }
        return rec;
    }//end public static Rectangle getSubImageDimension

    /**
     * Turns a buffered image into an image with transparency bit set for the
     * chosen colour, making the image transparent to the background or
     * highlighting colours.
     * @param source The source image to be made transparent.
     * @param find_colour The colour that will be made transparent.
     * @return The image with transparency, null if there are errors during 
     * processing.
     */
    public static BufferedImage getTransparentImage(BufferedImage source, Color find_colour) {
        BufferedImage tran_image = null;
        int img_w, img_h, w, h;
        try {
            w = source.getWidth();
            h = source.getHeight();
            /**
             * make the transparent image first
             */
            tran_image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            /**
             * Then find the colours that are not the excluding colour and
             * draw that on the new transparent image, making the bit that
             * doesn't contain any colour transparent.
             */
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int img_rgb = (source.getRGB(x, y));
                    Color img_colour = new Color(img_rgb);
                    boolean is_same = (find_colour.equals(img_colour));
                    if (!is_same) {
                        tran_image.setRGB(x, y, img_rgb);
                    }//end if
                }//end for (int col = 0; col < img_h; col++)
            }//end for(int row = 0; row < img_w; row++)            
        } catch (Exception ex) {
        }
        return tran_image;
    }//end public static BufferedImage getTransparentImage(BufferedImage source, Color find_colour)

    /**
     * Gets the dimension of a sub-image within an existing one by excluding
     * a selected colour. The rectangle return contains the dimension of the
     * sub-image where the first instance of a different colour to the
     * input colour is found. This function find the boundary to be cropped.
     * @param img The existing image to be cropped.
     * @param find_colour The colour where cropping will be included.
     * @return The rectangle contains the dimension where cropping can be 
     * performed.
     */
    public static Rectangle getSubImageDimension(BufferedImage img, Color find_colour) {
        Rectangle rec = null;
        int lx, ly, rx, ry;
        int img_w, img_h, w, h;
        try {
            img_w = img.getWidth();
            img_h = img.getHeight();

            /**
             * Assuming the largest value first.
             */
            lx = img_w;
            ly = img_h;
            /**
             * Assuming the smallest value first.
             */
            rx = 0;
            ry = 0;

            /**
             * search top-left down to bottom-right and find the excluding 
             * colour. The smaller value is held.
             */
            for (int x = 0; x < img_w; x++) {
                for (int y = 0; y < img_h; y++) {
                    int img_rgb = (img.getRGB(x, y));
                    Color img_colour = new Color(img_rgb);
                    boolean is_same = (find_colour.equals(img_colour));
                    if (!is_same) {
                        if (x < lx) {
                            lx = x;
                        }
                        if (y < ly) {
                            ly = y;
                        }
                        if (x > rx) {
                            rx = x;
                        }
                        if (y > ry) {
                            ry = y;
                        }
                    }//end if
                }//end for (int col = 0; col < img_h; col++)
            }//end for(int row = 0; row < img_w; row++)

            /**
             * Calculate the width and height
             */
            w = (rx - lx);
            h = (ry - ly);
            rec = new Rectangle(lx, ly, w, h);
        } catch (Exception ex) {
        }
        return rec;
    }//end public Rectangle getSubImageDimension(BufferedImage img, Color col)

    public static BufferedImage compressImage(BufferedImage image, float quality) throws IOException {
        //int orig_size = image.getData().getDataBuffer().getSize();
        
        //System.out.println("Original size:" + orig_size);
        
        // Get a ImageWriter for jpeg format.
        Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException(_("No writers found"));
        }
        ImageWriter writer = (ImageWriter) writers.next();
        // Create the ImageWriteParam to compress the image.
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        // The output will be a ByteArrayOutputStream (in memory)
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32768);
        ImageOutputStream ios = ImageIO.createImageOutputStream(bos);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(image, null, null), param);
        ios.flush(); // otherwise the buffer size will be zero!
        
        // From the ByteArrayOutputStream create a RenderedImage.
        ByteArrayInputStream in = new ByteArrayInputStream(bos.toByteArray());
        BufferedImage out = ImageIO.read(in);        
        //int compressed_size = out.getData().getDataBuffer().getSize();
        //System.out.println("Compressed size:" + compressed_size);
        return out;
    // Uncomment code below to save the compressed files.
    //    File file = new File("compressed."+quality+".jpeg");
    //    FileImageOutputStream output = new FileImageOutputStream(file);
    //    writer.setOutput(output); writer.write(null, new IIOImage(image, null,null), param);
    }
}
