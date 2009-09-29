/*
 * Dropper.java
 *
 * Created on 15 Σεπτέμβριος 2005, 7:22 μμ
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
package com.panayotis.jubler.os;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.undo.UndoEntry;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

/**
 * Allowing drag/drop of external files and records within the JTables. When
 * files are dragged into the JTable area, file loading is performed. When
 * records are dragged/dropped, moving or copying of records is performed.
 * Moving is done by the simple left-mouse drage/drop. The original records are
 * removed from the source table, and clones of the original are inserted into
 * the new table. Copying is done under the effect of holding down Ctrl key
 * whilst dragging/dropping. Copying DO NOT remove the original, but clones
 * of the original records are inserted into the dropping location.
 * @author teras && hoang_tran
 */
public class Dropper extends TransferHandler {

    private Jubler parent;

    /** Creates a new instance of DragReciever */
    public Dropper(Jubler parent) {
        this.parent = parent;
    }

    /**
     * Checks for the component being involved in the drag & drop operation
     * and only allow COPY_OR_MOVE if the component is an instance of JTable.
     * @param c  the component holding the data to be transferred;
     *           provided to enable sharing of <code>TransferHandler</code>s
     * @return {@code COPY_OR_MOVE} if the component is an instance of JTable,
     *          otherwise returns <code>NONE</code>
     */
    public int getSourceActions(JComponent c) {
        if (c instanceof JTable) {
            return COPY_OR_MOVE;
        } else {
            return NONE;
        }
    }//end public int getSourceActions(JComponent c)

    /**
     * Checks to see if the component is an instance of JTable and its data
     * model is an instance of {@link Subtitles}. If it is, a Vector of
     * selected {@link SubEntry SubEntries} are created and returned in an
     * instance of {@link ListTransferable}.
     * @param c  the component holding the data to be transferred;
     *           provided to enable sharing of <code>TransferHandler</code>s
     * @return An instance of {@link ListTransferable} if the component
     * is as expected, null otherwise.
     */
    public Transferable createTransferable(JComponent c) {
        Subtitles model = null;
        SubEntry record = null;
        Transferable t = null;
        if (c instanceof JTable) {
            JTable table = (JTable) c;
            boolean is_ok = (table.getModel() instanceof Subtitles);
            if (!is_ok) {
                return null;
            }
            int[] selection = table.getSelectedRows();
            Vector<SubEntry> selectedRows = new Vector<SubEntry>();
            model = (Subtitles) table.getModel();
            Jubler.copybuffer.clear();
            for (int j = 0; j < selection.length; j++) {
                record = (SubEntry) model.getRecordAtRow(selection[j]);
                if (record != null) {
                    selectedRows.add(record);
                    Jubler.copybuffer.add((SubEntry) record.clone());
                    //DEBUG.logger.log(Level.INFO, "Export: " + record.toString());
                }//end if (record != null)
            }//end for (int j = 0; j < selection.length; j++)
            t = new ListTransferable(selectedRows);
            //make sure that all selected options are passed onto the components
        }
        return t;
    }//end public Transferable createTransferable(JComponent c)

    /**
     * Remove the original records in the component.
     * @param c the component that was the source of the data
     * @param t The data that was transferred or possibly null
     *               if the action is <code>NONE</code>.
     * @param action the actual action that was performed
     */
    public void exportDone(JComponent c, Transferable t, int action) {
        Subtitles model = null;
        try {
            JTable table = (JTable) c;
            ListTransferable list = (ListTransferable) t;
            if (action == MOVE) {
                Subtitles subs = parent.getSubtitles();
                parent.getUndoList().addUndo(new UndoEntry(subs, "Remove records"));
                Vector<SubEntry> record_list =
                        getListOfExportedRecords(t, list.getListFlavor());
                model = (Subtitles) table.getModel();
                model.removeAll(record_list);
                parent.tableHasChanged(null);
                //DEBUG.logger.log(Level.WARNING, "Export done!");
            }//end if (action == MOVE)
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, "Export done failed: " + ex.toString());
        }
    }//end public void exportDone(JComponent c, Transferable t, int action)

    /**
     * Checks to see if the component can receive exported data
     * @param comp  the component to receive the transfer;
     *              provided to enable sharing of <code>TransferHandler</code>s
     * @param transferFlavors  the data formats available
     * @return  true if the data can be inserted into the component, false otherwise
     */
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return true;
        /*
        for (DataFlavor data : transferFlavors) {
        String mime = data.getHumanPresentableName();
        if (mime.equals("application/x-java-file-list") || mime.equals("text/uri-list")) {
        return true;
        }
        }
        return false;
         */
    }

    /**
     * Get a list of files from the {@code Transferable}
     * @param t The data that was transferred.
     * @param flavor the data format.
     * @return A list of files, or null if errors occur.
     */
    @SuppressWarnings("unchecked")
    private AbstractList<File> getFileListString(Transferable t, DataFlavor flavor) {
        try {
            Object data = t.getTransferData(flavor);
            if (data instanceof InputStreamReader) {
                AbstractList<File> files = new ArrayList<File>();
                BufferedReader in = new BufferedReader((InputStreamReader) t.getTransferData(flavor));
                String entry;
                while ((entry = in.readLine()) != null) {
                    try {
                        files.add(new File(new URI(entry)));
                    } catch (IllegalArgumentException e) {
                    } catch (URISyntaxException e) {
                    }
                }
                return files;
            } else {
                return (AbstractList<File>) t.getTransferData(flavor);
            }
        } catch (UnsupportedFlavorException e) {
            DEBUG.debug(e);
        } catch (IOException e) {
            DEBUG.debug(e);
        }
        return null;
    }

    /**
     * Get a list of originally exported records.
     * @param t The data that was transferred.
     * @param flavor the data format.
     * @return The Vector of exported records, null if errors occur.
     */
    @SuppressWarnings("unchecked")
    public Vector<SubEntry> getListOfExportedRecords(Transferable t, DataFlavor flavor) {
        Vector<SubEntry> new_list = null;
        try {
            Object data = t.getTransferData(flavor);
            if (data instanceof Vector) {
                new_list = (Vector<SubEntry>) data;
            }//end if (data instanceof List)
        } catch (Exception ex) {
        } finally {
            return new_list;
        }
    }//end public List<SubEntry> getListOfRecord(Transferable t, DataFlavor flavor)

    /**
     * Get a list of cloned records off the original list.
     * @param t The data that was transferred.
     * @param flavor the data format.
     * @return The Vector of cloned records exported, null if errors occur.
     */
    @SuppressWarnings("unchecked")
    public Vector<SubEntry> getListOfClonedRecords(Transferable t, DataFlavor flavor) {
        Vector<SubEntry> new_list = null;
        try {
            Object data = t.getTransferData(flavor);
            if (data instanceof Vector) {
                new_list = new Vector<SubEntry>();
                Vector<SubEntry> old_list = (Vector<SubEntry>) data;
                for (SubEntry entry : old_list) {
                    new_list.add((SubEntry) entry.clone());
                }//end for(SubEntry entry : old_list)
            }//end if (data instanceof List)
        } catch (Exception ex) {
        } finally {
            return new_list;
        }
    }//end public List<SubEntry> getListOfRecord(Transferable t, DataFlavor flavor)

    /**
     * Import the data that was exported. If the exported data flavour 
     * was a list of files, each instance of the file is loaded with a new
     * instance of Jubler. If the export data flavour is a 'List', the records
     * in the list are imported.
     * @param info the information needed to determine the suitability of a
     * transfer or to import the data contained within.
     * @return true if the dropping of data is to be performed, false otherwise.
     */
    public boolean importData(TransferHandler.TransferSupport info) {
        //DEBUG.logger.log(Level.INFO, "importData: " + info.toString());
        Transferable t = info.getTransferable();
        for (DataFlavor data : info.getDataFlavors()) {
            String mime = data.getHumanPresentableName();
            //DEBUG.logger.log(Level.INFO, "Mime type: " + mime);
            if (mime.equals("text/uri-list") || mime.equals("application/x-java-file-list")) {
                AbstractList<File> files = getFileListString(t, data);
                if (files == null) {
                    break;
                }
                for (File f : files) {
                    if (f.isFile()) {
                        parent.loadFile(f, false);
                    }//end if (f.isFile())
                }//end for (File f : files)
                break;
            } else if (mime.equals("List")) {
                int drop_row = -1;
                try {
                    Component c = info.getComponent();
                    JTable tbl = (JTable) info.getComponent();
                    try {
                        drop_row = tbl.getDropLocation().getRow();
                    } catch (Exception ex) {
                        drop_row = tbl.getSelectedRow();
                    }
                    Subtitles model = (Subtitles) tbl.getModel();

                    Subtitles subs = parent.getSubtitles();
                    parent.getUndoList().addUndo(new UndoEntry(subs, "Insert records"));

                    Vector<SubEntry> record_list = getListOfClonedRecords(t, data);
                    model.addAll(record_list, drop_row);
                    break;
                } catch (Exception ex) {
                    DEBUG.logger.log(Level.WARNING, "Drop failed: " + ex.toString());
                }
            }//end if (mime.equals...)
        }//end for (DataFlavor data : t.getTransferDataFlavors())
        return true;
    }//end public boolean importData(JComponent comp, Transferable t)
}//end public class Dropper extends TransferHandler

