/*
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
package com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.NonDuplicatedVector;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.subs.Share;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
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
import javax.swing.JOptionPane;

/**
 * This class deals with images.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class JImage implements CommonDef {

    /**
     * The colour that is used for DVB-T subtitle's transparency.
     */
    public static int DVBT_SUB_TRANSPARENCY = 0x60;
    /**
     * The black background colour in DVB-T subtitle blocks
     */
    public static Color DVBT_SUB_BLACK_BC = new Color(31, 31, 31);

    /**
     * Display an option panel which allows, among other options,
     * browsing for directories where new directory can be created. Other options
     * include the ability to select to use current dir, the ability to set the
     * flag not to remind the creation of a new directory again in the future.
     * Setting the "Do not remind again!" will deny the ability to prompt for
     * the option to create a directory. Option "Use current" will only
     * stops the prompt for the current set of images until the maximum
     * number of images is reached.
     * @param default_directory The default directory at which the file-chooser
     * dialog will change to when it starts.
     * @return The new directory created and selected
     */
    public static File[] createImageDirectory(File default_directory) {
        Object[] options = {
            _("Create/Select new"),
            _("Use current")
        };

        StringBuffer b = new StringBuffer();
        b.append(_("Current directory is " + UNIX_NL + "\"{0}\"",
                default_directory.getAbsoluteFile())).append(UNIX_NL);
        b.append(_("Large number of files in one directory could")).append(UNIX_NL);
        b.append(_("deteriorate the system's performance.")).append(UNIX_NL);
        b.append(_("Would you like to create new directories")).append(UNIX_NL);
        b.append(_("and select them for storing images?")).append(UNIX_NL);
        b.append(_("Note:"));
        b.append(_("(Images are divided equally over the group")).append(UNIX_NL);
        b.append(_("of selected directories.)"));
        String msg = b.toString();
        String title = _("Creating directories for images");

        int selected_option = JOptionPane.showOptionDialog(null,
                msg,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        switch (selected_option) {
            case JOptionPane.YES_OPTION: //Browse...
                File[] directories = Share.browseDir(default_directory);
                return directories;
            case JOptionPane.NO_OPTION: //Not this image
                break;
        }//end selected_option
        return null;
    }//public static File[] createImageDirectory(File default_directory)
    /**
     * Initiates the {@link #createImageDirectory} - singular - to obtain
     * a directory selection using a file dialog, defaulted at an initial
     * directory. Once the selection has been made, the list of selected
     * directories, plus the default directory, are grouped into a single
     * non-duplicated list and returned to the calling routine.
     * @param default_directory The default directory to start-up the file
     * dialog with.
     * @return A non-duplicated list of directories which has been selected,
     * empty if the directory selection operation has been cancelled.
     */
    public static NonDuplicatedVector<File> createImageDirectories(File default_directory) {
        NonDuplicatedVector<File> dirList = new NonDuplicatedVector<File>();
        File[] selectedDirectories = createImageDirectory(default_directory);
        boolean has_new_directories =
                (selectedDirectories != null) && (selectedDirectories.length > 0);
        if (has_new_directories) {
            for (int i = 0; i < selectedDirectories.length; i++) {
                File selected_dir = selectedDirectories[i];
                dirList.add(selected_dir);
            }//end for
        }//end if
        return dirList;
    }//end public static Vector<File> createImageDirectory(File default_directory)
    /**
     * Write an image to a pre-defined file.
     * @param img The image to write
     * @param dir The directory to write to
     * @param filename The image's filename
     * @param extension The extension, indicating the type ie. png, jpg, bmp
     * @return true if the image was written without errors, false otherwise.
     */
    /**
     * Write an image to a pre-defined file.
     * @param img The image to write.
     * @param file The file to write the image to.
     * @param extension The image format, recognised by the file's extension.
     * @return true if the operation carried out without errors, false otherwise.
     */
    public static boolean writeImage(BufferedImage img, File file, String extension) {
        try {
            ImageIO.write(img, extension, file);
            return true;
        } catch (Exception ex) {
            //ex.printStackTrace(System.out);
            return false;
        }
    }

    /**
     * Writes an image to a file.
     * @param img The image to write
     * @param dir The directory to write to
     * @param filename The image's filename
     * @param extension The extension, indicating the type ie. png, jpg, bmp
     * @return true if the image was written without errors, false otherwise.
     */
    public static boolean writeImage(BufferedImage img, String dir, String filename, String extension) {
        try {
            File image_file = new File(dir, filename);
            ImageIO.write(img, extension, image_file);
            return true;
        } catch (Exception ex) {
            //ex.printStackTrace(System.out);
            return false;
        }
    }//end public static void writeImage(BufferedImage img, String file_name)
    /**
     * Using the ImageIO to read an image from a file into the internal buffer,
     * And then create a image icon which is stored in the image record and update the
     * internal image dimension if this image is larger than the previous one.
     * The image, after loaded, is cropped and transformed to a transparent 
     * image where the DVBT transparency colours are excluded.
     * @param f the image file being read
     * @return the image record if reading was successfully carried out, Null if reading's failed.
     */
    public static BufferedImage readImage(File f) {
        BufferedImage loaded_image;
        try {
            loaded_image = ImageIO.read(f);
            return loaded_image;
        } catch (Exception ex) {
            return null;
        }
    }//public static ImageIcon readImage(File f)
    public static BufferedImage makeTransparentImage(BufferedImage img,
            Object[] color_index_list,
            Object[] transparency_index_list,
            Object[] color_table) {
        try {
            BufferedImage tran_image = img;
            for (int i = 0; i < color_index_list.length; i++) {
                int transparentcy = Integer.parseInt(transparency_index_list[i].toString());
                if (transparentcy == 0) {
                    int color_i =
                            Integer.parseInt(color_index_list[i].toString());

                    int transparent_color =
                            Integer.parseInt(color_table[color_i].toString());
                    tran_image = makeTransparentImage(tran_image, transparent_color);
                }
            }//end for(int i=0; i < color.length; i++)
            return tran_image;
        } catch (Exception ex) {
            return img;
        }
    }//end public static BufferedImage makeTransparentImage(BufferedImage img...
    public static BufferedImage makeTransparentImage(BufferedImage img, int color) {
        BufferedImage sub_img, tran_image = null;
        try {
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
            getSubImageDimension(
                    img,
                    color,
                    tran_image);
            return tran_image;
        } catch (Exception ex) {
            return img;
        }
    }

    /**
     * Cut the image and produce the transparent image of the original
     * @param img The image to cut
     * @param color The color to make transparent
     * @return The sub-image
     */
    public static BufferedImage cutImage(BufferedImage img, int color) {
        BufferedImage sub_img, tran_image = null;
        try {
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
                    color,
                    tran_image);

            /**
             * now crop the transparent image down to the size obtained.
             */
            sub_img = tran_image.getSubimage(
                    sr.x,
                    sr.y,
                    sr.width,
                    sr.height);
            return sub_img;
        } catch (Exception ex) {
            //ex.printStackTrace(System.out);
            //return the original image
            return tran_image;
        }
    }//public static BufferedImage cutImage(BufferedImage img, int color)
    /**
     * Cut the image and produce the transparent image of the original
     * @param img The image to cut
     * @return The sub-image
     */
    public static BufferedImage cutImage(BufferedImage img) {
        return cutImage(img, DVBT_SUB_TRANSPARENCY);
    }//end public static BufferedImage cutImage(BufferedImage img)
    public static File bwConversionToBMPTempFile(ImageIcon source) {
        String img_ext = "bmp";
        try {
            BufferedImage bw_img = bwConversion(source);
            File tempFile = File.createTempFile("JublerTempImg", char_dot + img_ext);
            writeImage(bw_img, tempFile, img_ext);
            return tempFile;
        } catch (Exception ex) {
            return null;
        }
    }//end public static File bwConversionToTempFile(ImageIcon source) 
    public static BufferedImage bwConversion(ImageIcon source) {
        BufferedImage new_image = null;
        int w, h;
        try {
            w = source.getIconWidth();
            h = source.getIconHeight();
            new_image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            new_image.getGraphics().drawImage(source.getImage(), 0, 0, null);
            return new_image;
        } catch (Exception ex) {
            return null;
        }
    }//public static BufferedImage bwConversion(BufferedImage source)
    public static BufferedImage icoToBufferedImage(ImageIcon source) {
        BufferedImage new_image = null;
        int w, h;
        try {
            w = source.getIconWidth();
            h = source.getIconHeight();
            new_image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            new_image.getGraphics().drawImage(source.getImage(), 0, 0, null);
            return new_image;
        } catch (Exception ex) {
            return null;
        }
    }//end public static BufferedImage icoToBufferedImage(ImageIcon source) 
    /**
     * Creates a new buffered image using TYPE_BYTE_GRAY meaning converts
     * the current image to gray scale.
     * @param source The source image
     * @return The newly converted b/w image if no errors occured, source
     * image if some errors occured.
     */
    public static BufferedImage bwConversion(BufferedImage source) {
        BufferedImage new_image = null;
        int w, h;
        try {
            w = source.getWidth();
            h = source.getHeight();
            new_image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            new_image.getGraphics().drawImage(source, 0, 0, null);
            return new_image;
        } catch (Exception ex) {
            return source;
        }
    }//public static BufferedImage bwConversion(BufferedImage source)
 
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
            int find_colour,
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
            int[] image_data = source.getRGB(0, 0, img_w, img_h, null, 0, img_w);
            int[] dest_image_data = new int[img_w * img_h];
            for (int i = 0,  x = 0,  y = 0; i < image_data.length; i++) {
                boolean is_reset_xy = (i > 0) && (i % img_w == 0);
                if (is_reset_xy) {
                    x = 0;
                    y += 1;
                } else {
                    x++;
                }
                int img_rgb = image_data[i];
                
                //force checking the RGB components only, excludes the Transparency bits.
                int mask = 0x00ffffff; 
                boolean is_same = (find_colour & mask) == (img_rgb & mask);
                if (!is_same) {
                    dest_image_data[i] = img_rgb;
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
            }//end for(int i=0; i < image_data.length; i++)
            dest.setRGB(0, 0, img_w, img_h, dest_image_data, 0, img_w);
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

            int[] image_data = source.getRGB(0, 0, w, h, null, 0, w);
            int[] dest_image_data = new int[w * h];
            int f_rgb = find_colour.getRGB();
            for (int i = 0; i < image_data.length; i++) {
                int img_rgb = image_data[i];
                //force checking the RGB components only, excludes the Transparency bits.
                int mask = 0x00ffffff; 
                boolean is_same = (f_rgb & mask) == (img_rgb & mask);
                if (!is_same) {
                    dest_image_data[i] = img_rgb;
                }//end if
            }//end for(int i=0; i < image_data.length; i++)
            tran_image.setRGB(0, 0, w, h, dest_image_data, 0, w);
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
            int[] image_data = img.getRGB(0, 0, img_w, img_h, null, 0, img_w);
            int f_rgb = find_colour.getRGB();
            for (int i = 0,  x = 0,  y = 0; i < image_data.length; i++) {
                boolean is_reset_xy = (i > 0) && (i % img_w == 0);
                if (is_reset_xy) {
                    x = 0;
                    y += 1;
                } else {
                    x++;
                }
                
                int img_rgb = image_data[i];
                int mask = 0x00ffffff; 
                boolean is_same = (f_rgb & mask) == (img_rgb & mask);
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
            }//end for(int i=0; i < image_data.length; i++)

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
    /**
     * Note the copyright of this code is: Rafael Santos - 
     * Author of: Java Image Processing Cookbook
     * http://www.lac.inpe.br/~rafael.santos/JIPCookbook/6040-howto-compressimages.jsp
     * 
     * The routine compress a BufferedImage by forming a JPEG writer and 
     * write the original image to the buffer of the compressed writer which
     * will compress the image as it writes the image out. The compressed
     * image is then read back again into another instance of BufferedImage
     * and return it for use. The returned image is the compressed image of
     * the original.
     * @param image The image to be compressed.
     * @param quality The factor to compress. 1.0f being the highest quality,
     * 0.1f being the worst.
     * @return The compressed BufferedImage.
     * @throws java.io.IOException When a JPEG write is not available.
     */
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
    }//end public static BufferedImage compressImage(BufferedImage image, float quality) throws IOException
    public static BufferedImage captureComponentGraphic(Component comp) {
        BufferedImage img = null;
        try {
            Dimension dim = comp.getSize();
            img = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
            Graphics g2d = img.getGraphics();
            comp.paint(g2d);
        } catch (Exception ex) {
        }
        return img;
    }//end 
}//end public class JImage

