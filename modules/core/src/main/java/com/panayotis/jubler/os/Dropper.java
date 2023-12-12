/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.os;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubFile;
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
import javax.swing.JComponent;
import javax.swing.TransferHandler;

public class Dropper extends TransferHandler {

    private JubFrame parent;

    /**
     * Creates a new instance of DragReciever
     */
    public Dropper(JubFrame parent) {
        this.parent = parent;
    }

    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        for (DataFlavor data : transferFlavors) {
            String mime = data.getHumanPresentableName();
            if (mime.equals("application/x-java-file-list") || mime.equals("text/uri-list"))
                return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private AbstractList<File> getFileListString(Transferable t, DataFlavor flavor) {
        try {
            Object data = t.getTransferData(flavor);
            if (data instanceof InputStreamReader) {
                AbstractList<File> files = new ArrayList<File>();
                BufferedReader in = new BufferedReader((InputStreamReader) t.getTransferData(flavor));
                String entry;
                while ((entry = in.readLine()) != null)
                    try {
                        files.add(new File(new URI(entry)));
                    } catch (IllegalArgumentException e) {
                    } catch (URISyntaxException e) {
                    }
                return files;
            } else
                return (AbstractList<File>) t.getTransferData(flavor);
        } catch (UnsupportedFlavorException e) {
            DEBUG.debug(e);
        } catch (IOException e) {
            DEBUG.debug(e);
        }
        return null;
    }

    public boolean importData(JComponent comp, Transferable t) {
        for (DataFlavor data : t.getTransferDataFlavors()) {
            String mime = data.getHumanPresentableName();
            if (mime.equals("text/uri-list") || mime.equals("application/x-java-file-list")) {
                AbstractList<File> files = getFileListString(t, data);
                if (files == null)
                    return true;
                for (File f : files)
                    if (f.isFile())
                        parent.loadFile(new SubFile(f, SubFile.EXTENSION_GIVEN), false);
                return true;
            }
        }
        return true;
    }
}
