/*
 * EditCopy.java
 *
 * Created on 20-May-2009, 19:26:57
 */

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
package com.panayotis.jubler.tools.ocr;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.options.gui.ProgressBar;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.ImageTypeSubtitle;
import static com.panayotis.jubler.i18n.I18N._;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/**
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class OCRAction extends JMenuItem implements ActionListener {

    private static String action_name = _("Tools OCR");
    private Jubler jublerParent = null;
    private String language;
    private String tessPath;
    private boolean ocrAllList = false;
    private JTable subTable = null;
    private Subtitles subs = null;
    private int len = 0;
    private int row_count = 0;

    public OCRAction() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }

    public OCRAction(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        try {
            subTable = jublerParent.getSubTable();
            subs = jublerParent.getSubtitles();
            row_count = subTable.getSelectedRowCount();
            len = (isOcrAllList() ? subs.size() : row_count);

            Thread ocr_thread = new Thread() {

                @Override
                public void run() {
                    boolean is_image = false;
                    int[] selected;
                    int row;
                    SubEntry sub;
                    ImageTypeSubtitle img_sub;
                    File[] file_list;

                    ProgressBar pb = ProgressBar.getInstance();
                    pb.setMinValue(0);
                    pb.setMaxValue(len - 1);
                    pb.on();

                    try {
                        for (int i = 0; i < len; i++) {

                            if (isOcrAllList()) {
                                row = i;
                            } else {
                                selected = subTable.getSelectedRows();
                                row = selected[i];
                            }//end if/else;

                            sub = subs.elementAt(row);

                            is_image = (sub instanceof ImageTypeSubtitle);
                            if (!is_image) {
                                continue;
                            }

                            img_sub = (ImageTypeSubtitle) sub;

                            String msg = "OCR: " + img_sub.getImageFile().getName();
                            pb.setTitle(msg);
                            pb.setValue(i);

                            final ArrayList<File> image_file_list = JImageIOHelper.createImageFiles(
                                    img_sub.getImageFile(), 0);

                            file_list = image_file_list.toArray(new File[image_file_list.size()]);

                            JOCR ocrEngine = new JOCR(tessPath);
                            String result = ocrEngine.recognizeText(file_list, language);
                            sub.setText(result.trim());
                                                        
                        // postprocess to correct common OCR errors
                        //result = Processor.postProcess(result, curLangCode);

                        }//end for(int i=0; i < len; i++)
                        jublerParent.tableHasChanged(null);
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                    } finally {
                        pb.off();
                    }
                }
            };
            
            ocr_thread.start();

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTessPath() {
        return tessPath;
    }

    public void setTessPath(String tessPath) {
        this.tessPath = tessPath;
    }

    public boolean isOcrAllList() {
        return ocrAllList;
    }

    public void setOcrAllList(boolean ocrAllList) {
        this.ocrAllList = ocrAllList;
    }
}//end public class OCRAction extends JMenuItem implements ActionListener 
