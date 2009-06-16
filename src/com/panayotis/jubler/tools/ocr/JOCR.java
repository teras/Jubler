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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;

/**
 *
 * @author Quan Nguyen (nguyenq@users.sf.net)
 */
public class JOCR {
    private final String LANG_OPTION = "-l";
    private final String EOL = System.getProperty("line.separator");

    private String tessPath;

    final static String OUTPUT_FILE_NAME = "TessOutput";
    final static String FILE_EXTENSION = ".txt";
    
    /** Creates a new instance of OCR */
    public JOCR(String tessPath) {
        this.tessPath = tessPath;
    }

    /**
     *
     * @param imageList
     * @param index
     * @param lang
     * @return
     * @throws java.lang.Exception
     */
    String recognizeText(final ArrayList<IIOImage> imageList, final int index, final String lang) throws Exception {
        ArrayList<File> tempImageFiles = JImageIOHelper.createImageFiles(imageList, index);
        return recognizeText(tempImageFiles.toArray(new File[tempImageFiles.size()]), lang);
    }

    /**
     *
     * @param imageFile
     * @param index
     * @param lang
     * @return
     * @throws java.lang.Exception
     */
    String recognizeText(final File imageFile, final int index, final String lang) throws Exception {
        ArrayList<File> tempImageFiles = JImageIOHelper.createImageFiles(imageFile, index);
        return recognizeText(tempImageFiles.toArray(new File[tempImageFiles.size()]), lang);
    }

    /**
     * 
     * @param tempImageFiles
     * @param lang
     * @return
     * @throws java.lang.Exception
     */
    String recognizeText(final File[] tempImageFiles, final String lang) throws Exception {
        File tempTessOutputFile = File.createTempFile(OUTPUT_FILE_NAME, FILE_EXTENSION);
        String output = tempTessOutputFile.getPath().substring(0, tempTessOutputFile.getPath().length() - FILE_EXTENSION.length()); // chop the .txt extension
        StringBuffer strB = new StringBuffer();
        
        List<String> cmd = new ArrayList<String>();
        cmd.add(tessPath + "/tesseract");
        cmd.add(""); // placeholder for inputfile
        cmd.add(output);
        cmd.add(LANG_OPTION);
        cmd.add(lang);

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(System.getProperty("user.home")));
            
        for (File tempImageFile : tempImageFiles) {
            // actual output file will be "output.txt"
//            ProcessBuilder pb = new ProcessBuilder(tessPath + "/tesseract", tempImage.getPath(), output, LANG_OPTION, lang);
            
            cmd.set(1, tempImageFile.getPath());
            pb.command(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
//            Process process = Runtime.getRuntime().exec(cmd.toArray(new String[0]));
            
            int w = process.waitFor();
            //System.out.println("Exit value = " + w);
            
            // delete temp working files
            tempImageFile.delete();
            
            if (w == 0) {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tempTessOutputFile), "UTF-8"));
                
                String str;
                
                while ((str = in.readLine()) != null) {
                    strB.append(str).append(EOL);
                }
                in.close();
            } else {
                String msg;
                switch (w) {
                    case 1:
                        msg = "Errors accessing files.";
                        break;
                    case 29:
                        msg = "Cannot recognize the image or its selected region.";
                        break;
                    case 31:
                        msg = "Unsupported image format.";
                        break;                        
                    default:
                        msg = "Errors occurred.";
                }
                for (File image : tempImageFiles) {
                    image.delete();
                }
                tempTessOutputFile.delete();
                throw new RuntimeException(msg);
            }
            
        }
        tempTessOutputFile.delete();
        return strB.toString();
    }
}