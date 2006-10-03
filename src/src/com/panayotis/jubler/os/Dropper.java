/*
 * DragReciever.java
 *
 * Created on 15 Σεπτέμβριος 2005, 7:22 μμ
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.panayotis.jubler.os;

import com.panayotis.jubler.*;
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

/**
 *
 * @author teras
 */
public class Dropper extends TransferHandler {
    private Jubler parent;
    
    /** Creates a new instance of DragReciever */
    public Dropper(Jubler parent) {
        this.parent = parent;
    }
    
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors)  {
        for (DataFlavor data : transferFlavors) {
            String mime = data.getHumanPresentableName();
            if (mime.equals("application/x-java-file-list") || mime.equals("text/uri-list")) {
                return true;
            }
        }
        return false;
    }
    
    private AbstractList<File> getFileListString(Transferable t, DataFlavor flavor) {
        try {
            Object data = t.getTransferData(flavor);
            if (data instanceof InputStreamReader) {
                AbstractList<File> files = new ArrayList<File>();
                BufferedReader in = new BufferedReader((InputStreamReader)t.getTransferData(flavor));
                String entry;
                while ( (entry=in.readLine()) != null ) {
                    try {
                        files.add(new File(new URI(entry)));
                    } catch (IllegalArgumentException e) {
                    } catch (URISyntaxException e) {
                    }
                }
                return files;
            } else {
                return (AbstractList<File>)t.getTransferData(flavor);
            }
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    public boolean importData(JComponent comp, Transferable t) {
        for (DataFlavor data: t.getTransferDataFlavors()) {
            String mime = data.getHumanPresentableName();
            if (mime.equals("text/uri-list") || mime.equals("application/x-java-file-list")) {
                AbstractList<File> files = getFileListString(t, data);
                if (files==null) return true;
                for (File f : files) {
                    if (f.isFile()) parent.loadFile(f, false);
                }
                return true;
            }
        }
        return true;
    }
}
