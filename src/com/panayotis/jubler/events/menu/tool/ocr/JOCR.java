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
package com.panayotis.jubler.events.menu.tool.ocr;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.Share;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Quan Nguyen (nguyenq@users.sf.net) && Hoang Duy Tran
 */
public class JOCR implements CommonDef {

    private static final String CMD_LANGUAGE_OPTION = "-l";
    private static final String TESS_OUT_FILE = "JublerTessOutput";
    private static final String TESS_OUT_EXT = ".txt";
    private String tesseract_path;

    /** Creates a new instance of OCR */
    public JOCR(String tesseract_path) {
        this.tesseract_path = tesseract_path;
    }

    public String ocrUsingOriginalImage(File imageFile, String language_code) throws Exception {
        File[] f_list = new File[1];
        f_list[0] = imageFile;
        return performOCR(f_list, language_code, false);
    }

    public String ocrUsingTempFile(File tempImageFile, String language_code) throws Exception {
        File[] f_list = new File[1];
        f_list[0] = tempImageFile;
        return performOCR(f_list, language_code, true);
    }//end public String recognizeText(final File tempImageFile, final String language_code) throws Exception 
        
    public String performOCR(File[] imgFiles, String language_code, boolean remove_image_file) throws Exception {

        File tessOutTempFile = File.createTempFile(TESS_OUT_FILE, TESS_OUT_EXT);
        String tess_output_file_name = Share.getFileNameWithoutExtension(tessOutTempFile);
        StringBuffer output_str_buffer = new StringBuffer();

        List<String> system_command = new ArrayList<String>();
        system_command.add(tesseract_path + "tesseract" + PROG_EXT);
        system_command.add(""); // this is the slot for inputfile, which will be filed later.
        system_command.add(tess_output_file_name);
        system_command.add(CMD_LANGUAGE_OPTION);
        system_command.add(language_code);

        //new instance of the process builder
        ProcessBuilder process_bld = new ProcessBuilder();

        //set the process's directory using the working directory, ie. Jubler's dir.
        process_bld.directory(new File(USER_CURRENT_DIR));

        //Run throught the image list.
        for (File imgFile : imgFiles) {
            //fillin the input file as the data is now available.
            system_command.set(1, imgFile.getPath());

            //now set the command to be executed.
            process_bld.command(system_command);

            //redirect the error stream (3) to standard output (1).
            process_bld.redirectErrorStream(true);

            //Start the process, execute the command line.
            Process process = process_bld.start();
            int process_returned_value = process.waitFor();

            if (remove_image_file) {
                imgFile.delete();
            }//end if (remove_image_file)

            if (process_returned_value == 0) {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tessOutTempFile), "UTF-8"));

                String str;

                while ((str = in.readLine()) != null) {
                    output_str_buffer.append(str).append(EOL);
                }
                in.close();
            } else {
                String error_msg;
                switch (process_returned_value) {
                    case 1:
                        error_msg = _("There are errors in accessing image files.");
                        break;
                    case 29:
                        error_msg = _("OCR engine cannot recognize the image.");
                        break;
                    case 31:
                        error_msg = _("Unsupported image format is detected.");
                        break;
                    default:
                        error_msg = _("Some unforeseen errors occurred.");
                }//end switch (process_returned_value) 

                if (remove_image_file) {
                    for (File image : imgFiles) {
                        image.delete();
                    }//for (File image : imgFiles)
                }//end if (remove_image_file)
                tessOutTempFile.delete();
                throw new RuntimeException(error_msg);
            }

        }
        tessOutTempFile.delete();
        return output_str_buffer.toString();
    }
}
