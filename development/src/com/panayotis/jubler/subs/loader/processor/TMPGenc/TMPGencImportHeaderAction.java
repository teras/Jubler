/*
 * TMPGencImportHeaderAction.java
 *
 * Created on 12-Jan-2009, 00:07:29
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
package com.panayotis.jubler.subs.loader.processor.TMPGenc;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.subs.loader.text.TMPGenc;
import com.panayotis.jubler.subs.records.TMPGenc.TMPGencSubtitleRecord;
import com.panayotis.jubler.subs.records.TMPGenc.TMPGencHeaderRecord;
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;

/**
 * This menu action import header from a different file. The header block contains
 * definitions of layouts, which is not currently editable from within this
 * software. This is useful during editing for TMPGenc DVD Authoring, especially when
 * there are definitions available from previous editing sessions, which can
 * now be used in the new file.
 *
 * A typical example of header would be:
 * <blockquote><pre>
 * [layoutDataItemList]
 * "Picture bottom layout",4,Tahoma,0.07,17588159451135,0,0,0,0,1,2,0,1,0.0035,0
 * "Picture top layout",4,Tahoma,0.1,17588159451135,0,0,0,0,1,0,0,1,0.0050,0
 * "Picture left layout",4,Tahoma,0.1,17588159451135,0,0,0,0,0,1,1,1,0.0050,0
 * "Picture right layout",4,Tahoma,0.1,17588159451135,0,0,0,0,2,1,1,1,0.0050,0
 *
 * [LayoutDataEx]
 * 0,0
 * 1,0
 * 1,0
 * 1,1
 *
 * </pre></blockquote>
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public class TMPGencImportHeaderAction extends JMenuItem implements ActionListener {

    private static String action_name = _("Import TMPGenc header");
    private Jubler jublerParent = null;

    public TMPGencImportHeaderAction() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }//end public TMPGencImportHeaderAction()

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        try {
            File input_file = null;
            String input_data = null;

            Subtitles current_subtitle = jublerParent.getSubtitles();
            if (Share.isEmpty(current_subtitle)) {
                return;
            }

            Object entry = current_subtitle.elementAt(0);
            boolean is_tmpgenc = (!Share.isEmpty(entry)) && (entry instanceof TMPGencSubtitleRecord);
            if (!is_tmpgenc) {
                return;
            }

            JFileChooser filedialog = new JFileChooser();
            filedialog.setMultiSelectionEnabled(false);
            filedialog.addChoosableFileFilter(new TMPGencFilter());
            FileCommunicator.getDefaultDialogPath(filedialog);

            filedialog.setDialogTitle(_("Load Subtitles"));
            if (filedialog.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            input_file = filedialog.getSelectedFile();
            input_data = FileCommunicator.load(input_file, null);
            if (Share.isEmpty(input_data)) {
                return;
            }

            //create a new instance to load the header
            TMPGenc sub_loader = new TMPGenc();
            sub_loader.parse(input_data, 25f, input_file);
            TMPGencHeaderRecord header = sub_loader.getHeader();
            if (header == null) {
                return;
            }

            boolean imported = false;
            TMPGencSubtitleRecord r=null;
            //Using the old instance to set the header of the newly loaded file
            for (int i = 0; i < current_subtitle.size(); i++) {
                entry = current_subtitle.elementAt(i);
                is_tmpgenc = (entry instanceof TMPGencSubtitleRecord);
                if (is_tmpgenc) {
                    r = (TMPGencSubtitleRecord) entry;
                    r.setHeaderRecord(header);
                    imported = true;
                }//end if
            }//end for(int i=0; i < current_subtitle.size(); i++)

            /* This is only debugging codes
            if (imported) {
                System.out.println("Imported new header: " + header);
                System.out.println("Last record: " + r);
            }//end if (imported)
             */
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }//end public void actionPerformed(java.awt.event.ActionEvent evt)

    /**
     * @return the jublerParent
     */
    public Jubler getJublerParent() {
        return jublerParent;
    }

    /**
     * @param jublerParent the jublerParent to set
     */
    public void setJublerParent(Jubler jublerParent) {
        this.jublerParent = jublerParent;
    }
}//end public class TMPGencImportHeaderAction extends JMenuItem

class TMPGencFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {

    public boolean accept(File pathname) {
        if (pathname.isDirectory()) {
            return true;
        }
        String fname = pathname.getName().toLowerCase();
        return fname.endsWith(TMPGenc.extension);
    }

    public String getDescription() {
        return TMPGenc.extendedName;
    }
}
