/*
 *  SplitSONSubtitleAction.java 
 * 
 *  Created on: Jun 20, 2009 at 3:27:05 PM
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
package com.panayotis.jubler.tools.duplication;

import com.panayotis.jubler.subs.loader.JNumberSelection;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.events.SubtitleUpdaterPostProcessingEvent;
import com.panayotis.jubler.subs.events.SubtitleUpdaterPostProcessingEventListener;
import com.panayotis.jubler.subs.loader.SimpleFileFilter;
import com.panayotis.jubler.subs.loader.SubtitleSplitFileFilter;
import com.panayotis.jubler.subs.loader.binary.SON.DVDMaestro;
import com.panayotis.jubler.tools.JSubtitleSetSplitter;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Vector;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * This class performs splitting of the SON subtitle file into multiple segments.
 * This is a very important task when dealing with live-subtitled programmes.
 * Due to the duplications in text, the number of subtitle events extracted 
 * could well be over 2000 (two thousands) for a one hour long programme. This
 * extreme large amount of events making it impossible to load such an index 
 * file into Jubler to split, and thus this operation MUST be done off-line.
 * This action will load the subtitle file into memory, without loading the
 * images, using a 
 * {@link SubtitleUpdaterPostProcessingEventListener} that attaches to an
 * instance of {@link DVDMaestro}, 
 * and based on the number of fragments that user selected, split and
 * write the fragments to the same directory as the input file. Each file will
 * inherit the original name with "_number" (ie. _01, _02 ...) attached to the
 * end of the file-name, before the 'son' extension. The number of subtitles
 * divided to each file is equal, but the last file will possibly hold more or
 * less the divident. For instance if the file holds 5 events, a divident of 2 
 * will produce two files, first one will have 2 records, the second one will 
 * have 3 records. The same file when divided into 3 will produce (2, 2, 1)
 * fragments. Each file will hold the same heading as the original file.
 * <br><br>
 * Once the file has been divided into much smaller fragments, each file can be
 * brought into Jubler and process without worrying too much about the memmory 
 * problem.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class SplitSONSubtitleAction extends JMenuItem implements ActionListener, CommonDef {

    private static String action_name = _("Split SON subtitle file");
    private Jubler jublerParent = null;
    private JFileChooser filedialog;
    DVDMaestro sub_loader = null;
    String msg = null;
    int split_numb = 0;
    File input_file = null;
    String input_data = null;

    public SplitSONSubtitleAction() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }

    public SplitSONSubtitleAction(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }

    private File getInputFile() {
        SimpleFileFilter son_filter =
                new SimpleFileFilter(
                DVDMaestro.sonExtendedName,
                DVDMaestro.sonExtension);

        filedialog = new JFileChooser();
        filedialog.setMultiSelectionEnabled(false);
        filedialog.addChoosableFileFilter(son_filter);
        FileCommunicator.getDefaultDialogPath(filedialog);

        filedialog.setDialogTitle(_("Load Subtitles"));
        if (filedialog.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File f = filedialog.getSelectedFile();
        return f;
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        try {
            //Get the input file to split
            input_file = getInputFile();
            if (Share.isEmpty(input_file)) {
                return;
            }

            //Get a number to split the original file to
            JNumberSelection sel_split_number =
                    new JNumberSelection(
                    jublerParent,
                    _("Divide the subtitle file"),
                    null); //use default prompt

            split_numb = sel_split_number.showDialog();
            if (split_numb < 2) {
                return;
            }

            //Load the text data for the subtitle-parser
            input_data = FileCommunicator.load(input_file, null);
            if (Share.isEmpty(input_data)) {
                msg = _("Input data is empty. Terminate operation.");
                JOptionPane.showMessageDialog(jublerParent, msg);
                return;
            }

            //creates an instance of the subtitle-loader
            sub_loader = new DVDMaestro();

            SubtitleUpdaterPostProcessingEventListener postImageLoadListener = new SubtitleUpdaterPostProcessingEventListener() {

                boolean ok = false;

                public void postProcessing(SubtitleUpdaterPostProcessingEvent e) {
                    Subtitles subs = e.getSubList();

                    if (Share.isEmpty(subs)) {
                        msg = _("No subtitle events were found. Terminate operation.");
                        JOptionPane.showMessageDialog(jublerParent, msg);
                        return;
                    }//end if

                    int number_of_events = subs.size();
                    boolean splittable = (number_of_events > split_numb);
                    if (!splittable) {
                        msg = _("Number of subtitle events smaller than the amount to split.");
                        JOptionPane.showMessageDialog(jublerParent, msg);
                        return;
                    }

                    //Now create an instance of the splitter.
                    SubtitleSplitFileFilter son_split_file_filter =
                            new SubtitleSplitFileFilter(
                            DVDMaestro.sonExtension,
                            DVDMaestro.sonExtendedName);

                    JSubtitleSetSplitter spliter = new JSubtitleSetSplitter(
                            subs,
                            split_numb,
                            input_file,
                            son_split_file_filter);


                    //Tell splitter to split the subtitle events into a list of [File, Subtitles] pairs
                    Map<File, Subtitles> splitSet = spliter.split();

                    //Run through the list of files and write out the corresponding subtitle events allocated
                    Vector<File> fileList = spliter.getFileList();
                    for (int i = 0; i < fileList.size(); i++) {
                        File f = fileList.elementAt(i);
                        Subtitles subSet = splitSet.get(f);

                        ok |= sub_loader.produce(subSet, f);
                    }//end for (int i=0; i < fileList.size(); i++)
                    if (ok) {
                        showFileList(fileList);
                    }//end if
                }//end public void postParseAction(PostParseActionEvent e) 
            };//end PostParseActionEventListener postImageLoadListener = new PostParseActionEventListener()

            sub_loader.getPostImageLoadActions().clear();
            sub_loader.getPostImageLoadActions().add(postImageLoadListener);

            //Tell it not to load images as this is the cause for memory
            //shortage problem.
            sub_loader.setLoadImages(false);

            //Load the subtitle-events without the images
            Subtitles subs = sub_loader.parse(input_data, 25f, input_file);

        } catch (Exception ex) {
            msg = _("Unexpected error:") + ex.getMessage();
            JOptionPane.showMessageDialog(jublerParent, msg);
            ex.printStackTrace(System.out);
        }
    }//public void actionPerformed(java.awt.event.ActionEvent evt)
    /**
     * 
     * @param fileList
     */
    private void showFileList(Vector<File> fileList) {
        StringBuffer bf = new StringBuffer();
        for (int i = 0; i < fileList.size(); i++) {
            bf.append(fileList.elementAt(i).getName());
            bf.append(UNIX_NL);
        }//end for
        bf.append(_("Can be found under:"));
        bf.append(UNIX_NL);
        bf.append(input_file.getParent());
        msg = bf.toString();
        JOptionPane.showMessageDialog(jublerParent, msg);
    }
}//end public class SplitSubtitleAction extends JMenuItem implements ActionListener 

