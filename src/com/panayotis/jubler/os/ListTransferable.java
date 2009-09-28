/*
 * ListTransferable.java
 *
 * Created on 04 September 2007, 15:09
 *
 * Copyright 2007-2008 Hoang Duy Tran. All rights reserved.
 */

package com.panayotis.jubler.os;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
/**
 * Defines the interface for classes that can be used to provide data
 * for a transfer operation.
 * <p>
 * For information on using data transfer with Swing, see
 * <a href="http://java.sun.com/docs/books/tutorial/uiswing/misc/dnd.html">
 * How to Use Drag and Drop and Data Transfer</a>
 */
public class ListTransferable implements Transferable {
    private static DataFlavor listFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType +
                                                           ";class=java.util.List", "List");

    private java.util.List data;

    /**
     * Constructor
     * @param data The default data list for transfering
     */
    public ListTransferable(java.util.List data) {
        this.setData(data);
    }

    /**
     * Constructor
     * @param data The default data list for transfering
     */
    public ListTransferable(Object[] data) {
        this.setData(Arrays.asList(data));
    }

    /**
     * Returns an object which represents the data to be transferred.  The class
     * of the object returned is defined by the representation class of the flavor.
     * @param flavor The requested flavor for the data
     * @return The data list for transfering
     * @exception IOException If the data is no longer available in the requested flavor.
     * @exception UnsupportedFlavorException If the requested data flavor is not supported.
     * @see DataFlavor#getRepresentationClass
     */
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,
            IOException {
        if (isDataFlavorSupported(flavor)) {
            return getData();
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    /**
     * Returns an array of DataFlavor objects indicating the flavors the data
     * can be provided in.  The array should be ordered according to preference
     * for providing the data (from most richly descriptive to least descriptive).
     * @return an array of data flavors in which this data can be transferred
     */
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{getListFlavor()};
    }

    /**
     * Returns whether or not the specified data flavor is supported for
     * this object.
     * @param flavor the requested flavor for the data
     * @return boolean indicating whether or not the data flavor is supported
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        DataFlavor[] flavors = getTransferDataFlavors();
        for (int i = 0; i < flavors.length; i++) {
            if (flavors[i].equals(flavor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reference to the list data flavour
     * @return Reference to the list data flavour
     */
    public static DataFlavor getListFlavor() {
        return listFlavor;
    }

    /**
     * Set reference to the list data flavour
     * @param aListFlavor reference to the list data flavour
     */
    public static void setListFlavor(DataFlavor aListFlavor) {
        listFlavor = aListFlavor;
    }

    /**
     * Get the data list
     * @return data list
     */
    public java.util.List getData() {
        return data;
    }

    /**
     * Set the data list
     * @param data data list
     */
    public void setData(java.util.List data) {
        this.data = data;
    }
}
