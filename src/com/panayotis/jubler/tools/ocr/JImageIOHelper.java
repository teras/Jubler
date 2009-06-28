/**
 * Copyright @ 2008 Quan Nguyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.panayotis.jubler.tools.ocr;

import com.panayotis.jubler.options.gui.ProgressBar;
import com.panayotis.jubler.os.DEBUG;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.tools.JImage;
import com.sun.media.imageio.plugins.tiff.TIFFImageWriteParam;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;

/**
 * This class holds methods to help with image files creations. It's dominantly
 * uses the library <b>jai_imageio</b> and {@link javax.imageio.ImageIO} to
 * implement routines.
 * @author Quan Nguyen (nguyenq@users.sf.net) & Hoang Duy Tran (hoangduytran1960@googlemail.com)
 */
public class JImageIOHelper {

    /**
     * The front part of the temporary image files.
     */
    private final static String OUTPUT_FILE_NAME = "JublerTempFile";
    /**
     * The default extension for a tiff (Tagged Image File Format) image type
     * file.
     */
    public final static String TIFF_EXT = ".tif";
    /**
     * The default name that can be recognised by a format writer locator.
     */
    public final static String TIFF_FORMAT = "tiff";

    /**
     * This method writes a {@link BufferedImage} to a temporary file and
     * return the name of the file in an {@link ArrayList}. The temporary
     * file is system generated and will be of tiff format and will reside
     * in the system's temporary directory - depending on what operating
     * system user using. This file can then be used by other operation,
     * such as OCR action.
     * @param source The reference to an instance of {@link BufferedImage}.
     * @return An {@link ArrayList} contains the temporary file generated if
     * the operation was carried out without errors.
     * @throws java.lang.Exception When an error occurs, a
     * {@link RuntimeException} will be generated containing the original
     * exeption information.
     */
    public static ArrayList<File> createImageFile(BufferedImage source) throws Exception {
        ImageOutputStream ios = null;
        ImageWriter writer = null;

        ArrayList<File> tempImageFiles = new ArrayList<File>();
        try {
            //Set up the writeParam
            TIFFImageWriteParam tiffWriteParam = new TIFFImageWriteParam(Locale.US);
            DEBUG.logger.log(Level.INFO, tiffWriteParam.toString());
            tiffWriteParam.setCompressionMode(ImageWriteParam.MODE_DISABLED);

            //Get tif writer and set output to file
            Iterator writers = ImageIO.getImageWritersByFormatName(TIFF_FORMAT);
            writer = (ImageWriter) writers.next();

            //Read the stream metadata
            IIOMetadata streamMetadata = writer.getDefaultStreamMetadata(tiffWriteParam);

            IIOImage image = new IIOImage(source, null, null);

            File tempFile = File.createTempFile(OUTPUT_FILE_NAME, TIFF_EXT);
            ios = ImageIO.createImageOutputStream(tempFile);
            writer.setOutput(ios);
            writer.write(streamMetadata, image, tiffWriteParam);
            tempImageFiles.add(tempFile);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (ios != null) {
                    ios.close();
                }
                if (writer != null) {
                    writer.dispose();
                }
            } catch (Exception ex) {
            }
        }
        return tempImageFiles;
    }//end public static ArrayList<File> createImageFiles(BufferedImage) throws Exception

    /**
     * This method takes a list of image files, and an output file with
     * '.tif' extension and write all images in the input files to this
     * single output file in sequence. The output file is a uncompressed,
     * multi-paged 'tiff' image file. Many OCR application support the reading
     * of such as file.
     * @param image_file_list The list of files that contain images.
     * @param output_file The '.tif' output file.
     */
    public static void createPackedTiff(File[] image_file_list, File output_file) {
        ImageReader reader = null;
        ImageInputStream iis = null;
        ImageOutputStream ios = null;
        ImageWriter writer = null;
        IIOImage iio_img = null;
        ProgressBar pb = new ProgressBar();
        try {

            //Set up the writeParam
            TIFFImageWriteParam tiffWriteParam = new TIFFImageWriteParam(Locale.US);
            tiffWriteParam.setCompressionMode(ImageWriteParam.MODE_DISABLED);

            //Get tif writer and set output to file
            Iterator writers = ImageIO.getImageWritersByFormatName(TIFF_FORMAT);
            writer = (ImageWriter) writers.next();


            //Get the stream metadata
            IIOMetadata streamMetadata = writer.getDefaultStreamMetadata(tiffWriteParam);
            ios = ImageIO.createImageOutputStream(output_file);
            writer.setOutput(ios);
            writer.prepareWriteSequence(streamMetadata);

            pb.setMinValue(0);
            pb.setMaxValue(image_file_list.length);
            pb.on();
            int i = 0;
            for (File image_file : image_file_list) {

                String imageFileName = image_file.getName();
                String imageFormat = imageFileName.substring(imageFileName.lastIndexOf('.') + 1);

                Iterator readers = ImageIO.getImageReadersByFormatName(imageFormat);
                reader = (ImageReader) readers.next();
                if (reader == null) {
                    System.out.println(_("No reader found. Unable to load image: {0}", imageFileName));
                    continue;
                }

                iis = ImageIO.createImageInputStream(image_file);
                reader.setInput(iis);

                BufferedImage bi = reader.read(0);
                iio_img = new IIOImage(bi, null, null);
                writer.writeToSequence(iio_img, tiffWriteParam);

                i++;
                pb.setTitle(_("Packaging image file: " + imageFileName));
                pb.setValue(i);

            }//end for for (ImageIcon image : imageList)
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
            try {
                if (iis != null) {
                    iis.close();
                }
                if (reader != null) {
                    reader.dispose();
                }

                if (ios != null) {
                    ios.close();
                }
                if (writer != null) {
                    writer.dispose();
                }
            } catch (Exception ex) {
            }
            pb.off();
        }
    }//end public static void createPackedTiff(File[] image_file_list, File output_file) {

    /**
     * This method takes an array-list of images and an output file. It writes
     * all images in the sequence ordered by the array-list provided. Each
     * image is converted to B/W before writing out to external tiff file. Note
     * the file must containt a valid extension. The result is a multi-paged
     * tiff file, which contains all images given.
     * @param imageList The list of icon images to write
     * @param output_file The multi-page tiff output file.
     */
    public static void createPackedTiff(ArrayList<ImageIcon> imageList, File output_file) {
        ImageOutputStream ios = null;
        ImageWriter writer = null;
        ProgressBar pb = new ProgressBar();
        try {
            //Set up the writeParam
            TIFFImageWriteParam tiffWriteParam = new TIFFImageWriteParam(Locale.US);

            tiffWriteParam.setCompressionMode(ImageWriteParam.MODE_DEFAULT);

            //Get tif writer and set output to file
            Iterator writers = ImageIO.getImageWritersByFormatName(TIFF_FORMAT);
            writer = (ImageWriter) writers.next();


            //Get the stream metadata
            IIOMetadata streamMetadata = writer.getDefaultStreamMetadata(tiffWriteParam);
            ios = ImageIO.createImageOutputStream(output_file);
            writer.setOutput(ios);
            writer.prepareWriteSequence(streamMetadata);

            pb.setMinValue(0);
            pb.setMaxValue(imageList.size());
            pb.on();
            int i = 0;
            for (ImageIcon image : imageList) {
                BufferedImage b_img = JImage.bwConversion(image);
                IIOImage iio_img = new IIOImage(b_img, null, null);
                writer.writeToSequence(iio_img, tiffWriteParam);
                i++;
                pb.setTitle(_("Packaging image number: " + i));
                pb.setValue(i);
            }//end for for (ImageIcon image : imageList)
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
            try {
                if (ios != null) {
                    ios.close();
                }
                if (writer != null) {
                    writer.dispose();
                }
            } catch (Exception ex) {
            }
            pb.off();
        }
    }//end public static void createPackedTiff(ArrayList<ImageIcon> imageList, File output_file) 

    /**
     * This method takes an image file, with the potential that the file could
     * contain many images, and convert each image within the input file to a
     * temporary tiff formatted image file, in the system's temporary directory.
     * The list of temporary files generated will be returned in a non-null
     * {@link ArrayList}.
     * @param imageFile The image file where images are to be extracted
     * @param index The index of the image within a file. -1 to indicate that
     * all images are to be extracted.
     * @return A non-null instance of {@link ArrayList}. The list will containt
     * a list of temporary tiff image files that it was generated.
     * @throws java.lang.Exception When an error occurs, a
     * {@link RuntimeException} will be generated containing the original
     * exeption information.
     */
    public static ArrayList<File> createImageFiles(File imageFile, int index) throws Exception {
        ImageReader reader = null;
        ImageInputStream iis = null;
        ImageOutputStream ios = null;
        ImageWriter writer = null;
        ArrayList<File> tempImageFiles = new ArrayList<File>();
        try {
            String imageFileName = imageFile.getName();
            String imageFormat = imageFileName.substring(imageFileName.lastIndexOf('.') + 1);

            Iterator readers = ImageIO.getImageReadersByFormatName(imageFormat);
            reader = (ImageReader) readers.next();

            iis = ImageIO.createImageInputStream(imageFile);
            reader.setInput(iis);
            //Read the stream metadata
            //IIOMetadata streamMetadata = reader.getStreamMetadata();

            //Set up the writeParam
            TIFFImageWriteParam tiffWriteParam = new TIFFImageWriteParam(Locale.US);
            tiffWriteParam.setCompressionMode(ImageWriteParam.MODE_DISABLED);

            //Get tif writer and set output to file
            Iterator writers = ImageIO.getImageWritersByFormatName(TIFF_FORMAT);
            writer = (ImageWriter) writers.next();

            //Read the stream metadata
            IIOMetadata streamMetadata = writer.getDefaultStreamMetadata(tiffWriteParam);

            BufferedImage out;
            if (index == -1) {
                int imageTotal = reader.getNumImages(true);

                for (int i = 0; i < imageTotal; i++) {
                    BufferedImage bi = reader.read(i);
                    IIOImage image = new IIOImage(bi, null, reader.getImageMetadata(i));
                    File tempFile = File.createTempFile(OUTPUT_FILE_NAME, TIFF_EXT);
                    ios = ImageIO.createImageOutputStream(tempFile);
                    writer.setOutput(ios);
                    writer.write(streamMetadata, image, tiffWriteParam);
                    ios.close();
                    tempImageFiles.add(tempFile);
                }
            } else {
                BufferedImage bi = reader.read(index);
                IIOImage image = new IIOImage(bi, null, reader.getImageMetadata(index));
                File tempFile = File.createTempFile(OUTPUT_FILE_NAME, TIFF_EXT);
                ios = ImageIO.createImageOutputStream(tempFile);
                writer.setOutput(ios);
                writer.write(streamMetadata, image, tiffWriteParam);
                ios.close();
                tempImageFiles.add(tempFile);
            }//end if/else
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (iis != null) {
                    iis.close();
                }
                if (reader != null) {
                    reader.dispose();
                }

                if (ios != null) {
                    ios.close();
                }
                if (writer != null) {
                    writer.dispose();
                }
            } catch (Exception ex) {
            }
            return tempImageFiles;
        }//end try/catch/finally
    }//end public static ArrayList<File> createImageFiles(File imageFile, int index) throws Exception

    /**
     * This method takes an array of images and convert each image to a
     * temporary tiff formatted image file, in the system's temporary directory.
     * The list of temporary files generated will be returned in a non-null
     * {@link ArrayList}. 
     * @param imageList The list of images to be written to their temporary
     * files.
     * @param index The index of the image within a file. -1 to indicate that
     * all images are to be written.
     * @return A non-null instance of {@link ArrayList}. The list will containt
     * a list of temporary tiff image files that it was generated.
     * @throws java.lang.Exception When an error occurs, 
     * an instance of {@link RuntimeException} will be generated containing
     * the original exeption information.
     */
    public static ArrayList<File> createImageFiles(ArrayList<IIOImage> imageList, int index) throws Exception {
        ImageReader reader = null;
        ImageInputStream iis = null;
        ImageOutputStream ios = null;
        ImageWriter writer = null;
        ArrayList<File> tempImageFiles = new ArrayList<File>();
        try {
            //Set up the writeParam
            TIFFImageWriteParam tiffWriteParam = new TIFFImageWriteParam(Locale.US);
            tiffWriteParam.setCompressionMode(ImageWriteParam.MODE_DISABLED);

            //Get tif writer and set output to file
            Iterator writers = ImageIO.getImageWritersByFormatName(TIFF_FORMAT);
            writer = (ImageWriter) writers.next();

            //Get the stream metadata
            IIOMetadata streamMetadata = writer.getDefaultStreamMetadata(tiffWriteParam);

            if (index == -1) {
                for (IIOImage image : imageList) {
                    File tempFile = File.createTempFile(OUTPUT_FILE_NAME, TIFF_EXT);
                    ios = ImageIO.createImageOutputStream(tempFile);
                    writer.setOutput(ios);
                    writer.write(streamMetadata, image, tiffWriteParam);
                    ios.close();
                    tempImageFiles.add(tempFile);
                }
            } else {
                IIOImage image = imageList.get(index);
                File tempFile = File.createTempFile(OUTPUT_FILE_NAME, TIFF_EXT);
                ios = ImageIO.createImageOutputStream(tempFile);
                writer.setOutput(ios);
                writer.write(streamMetadata, image, tiffWriteParam);
                ios.close();
                tempImageFiles.add(tempFile);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (iis != null) {
                    iis.close();
                }
                if (reader != null) {
                    reader.dispose();
                }

                if (ios != null) {
                    ios.close();
                }
                if (writer != null) {
                    writer.dispose();
                }
            } catch (Exception ex) {
            }
            return tempImageFiles;
        }
    }//end public static ArrayList<File> createImageFiles(ArrayList<IIOImage> imageList, int index) throws Exception 

    /**
     * Reading a possible multi-paged image file, and return them in a non-null
     * array.
     * @param imageFile The possible multi-paged image file.
     * @return The non-null array which contains the images read from the file.
     */
    public static ArrayList<IIOImage> getIIOImageList(File imageFile) {
        ImageReader reader = null;
        ImageInputStream iis = null;
        ArrayList<IIOImage> iioImageList = new ArrayList<IIOImage>();
        try {

            String imageFileName = imageFile.getName();
            String imageFormat = imageFileName.substring(imageFileName.lastIndexOf('.') + 1);
            Iterator readers = ImageIO.getImageReadersByFormatName(imageFormat);
            reader = (ImageReader) readers.next();

            if (reader == null) {
                throw new RuntimeException("Need to install JAI Image I/O package.\nhttps://jai-imageio.dev.java.net");
            }

            iis = ImageIO.createImageInputStream(imageFile);
            reader.setInput(iis);

            int imageTotal = reader.getNumImages(true);

            for (int i = 0; i < imageTotal; i++) {
                IIOImage image = new IIOImage(reader.read(i), null, reader.getImageMetadata(i));
                iioImageList.add(image);
            }

            return iioImageList;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (iis != null) {
                    iis.close();
                }
                if (reader != null) {
                    reader.dispose();
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }
    /*
    public static ArrayList<ImageIconScalable> getImageList(ArrayList<IIOImage> iioImageList) {
    try {
    ArrayList<ImageIconScalable> al = new ArrayList<ImageIconScalable>();
    for (IIOImage iioImage : iioImageList) {
    al.add(new ImageIconScalable((BufferedImage) iioImage.getRenderedImage()));
    }
    return al;
    } catch (Exception e) {
    return null;
    }
    }
     */
}
