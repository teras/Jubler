/*
 *  RecordComponent.java 
 * 
 *  Created on: Jun 21, 2009 at 11:34:21 AM
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
package com.panayotis.jubler.subs;

import static com.panayotis.jubler.i18n.I18N._;

/**
 * This class holds definitions and routines that deals with record components. 
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class RecordComponent {

    /**
     * These values indicates the component
     * of a record will be used. The typical use
     * will be for cutting/copying, and for importing.
     */
    public static final int CP_INVALID = 0;
    public static final int CP_TEXT = 1;
    public static final int CP_TIME = 2;
    public static final int CP_HEADER = 4;
    public static final int CP_IMAGE = 8;
    public static final int CP_RECORD = 16;

    public static boolean isCP_TEXT(int opt) {
        return ((opt & CP_TEXT) != 0);
    }

    public static boolean isCP_TIME(int opt) {
        return ((opt & CP_TIME) != 0);
    }

    public static boolean isCP_HEADER(int opt) {
        return ((opt & CP_HEADER) != 0);
    }

    public static boolean isCP_IMAGE(int opt) {
        return ((opt & CP_IMAGE) != 0);
    }

    public static boolean isCP_RECORD(int opt) {
        return ((opt & CP_RECORD) != 0);
    }

    public static boolean isCP_SELECTED(int opt) {
        return (isCP_TEXT(opt) ||
                isCP_TIME(opt) ||
                isCP_HEADER(opt) ||
                isCP_IMAGE(opt) ||
                isCP_RECORD(opt));
    }//public static boolean isCP_SELECTED(int opt)
    public static int CP_EXC(int opt, int excluding_opt) {
        int excluding_bits = ~excluding_opt;
        int ex_opt = (opt & excluding_bits);
        return ex_opt;
    }

    public static boolean isCP_SELECTED_EXC(int opt, int excluding_opt) {
        int ex_opt = CP_EXC(opt, excluding_opt);

        boolean is_selected = isCP_TEXT(ex_opt) ||
                isCP_TIME(ex_opt) ||
                isCP_HEADER(ex_opt) ||
                isCP_IMAGE(ex_opt) ||
                isCP_RECORD(ex_opt);

        return is_selected;
    }//public static boolean isCP_SELECTED(int opt)
    /**
     * The array for all components
     */
    public static int[] recordComponentList = new int[]{
        CP_TEXT,
        CP_TIME,
        CP_HEADER,
        CP_IMAGE,
        CP_RECORD
    };
    /**
     * This is the readable names of the record component
     * above and is used for human interaction, plus translation
     * purposes.
     */
    public static String[] componentNames = new String[]{
        _("Text"),
        _("Time"),
        _("Header"),
        _("Image"),
        _("Record")};

    public static int getComponentIndex(int entry) {
        try {
            for (int i = 0; i < recordComponentList.length; i++) {
                boolean is_found = (recordComponentList[i] == entry);
                if (is_found) {
                    return i;
                }//end if
            }//end for
        } catch (Exception ex) {
        }
        return -1;
    }//end public static int getComponentIndex(SubtitleRecordComponent entry)
    /**
     * Converting the selected value to the enumeration value of the 
     * selected component.
     * @param value The string value selected from list
     * @return One of the value listed in {@link SubtitleRecordComponent}, null
     * if the input value is not found in {@link componentNames}
     */
    public static int getSelectedComponent(String value) {
        int sel = CP_INVALID;
        try {
            for (int i = 0; i < componentNames.length; i++) {
                String list_value = componentNames[i];
                boolean found = (value.equals(list_value));
                if (found) {
                    sel = recordComponentList[i];
                    break;
                }//end if (found)
            }//end for (int i=0; i < componentNames.length; i++)
        } catch (Exception ex) {
        }//end try/catch
        return sel;
    }//end public SubtitleRecordComponent getSelectedComponent(String value)
    /**
     * Copy text content of the source record to target record, providing that
     * the option value indicating that is the case.
     * @param target 
     * @param source
     * @param opt
     * @return true if the option is set and the operation 
     * is carried out successful. false otherwise.
     */
    public static boolean copyText(SubEntry target, SubEntry source, int opt) {
        boolean has_changed = false;
        try {
            if (isCP_TEXT(opt)) {
                has_changed = target.copyText(source);
            }//end if
        } catch (Exception ex) {
        }
        return has_changed;
    }//end public static boolean copyText(SubEntry target, SubEntry source, int opt)
    /**
     * Copy time content of the source record to target record, providing that
     * the option value indicating that is the case.
     * @param target
     * @param source
     * @param opt
     * @return true if the option is set and the operation 
     * is carried out successful. false otherwise.
     */
    public static boolean copyTime(SubEntry target, SubEntry source, int opt) {
        boolean has_changed = false;
        try {
            if (isCP_TIME(opt)) {
                has_changed = target.copyTime(source);
            }//end if
        } catch (Exception ex) {
        }
        return has_changed;
    }//end public static boolean copyTime(SubEntry target, SubEntry source, int opt)
    /**
     * Copy image content of the source record to target record, providing that
     * the option value indicating that is the case.
     * @param target
     * @param source
     * @param opt
     * @return true if the option is set and the operation 
     * is carried out successful. false otherwise.
     */
    public static boolean copyImage(SubEntry target, SubEntry source, int opt) {
        boolean has_changed = false;
        try {
            if (isCP_IMAGE(opt)) {
                has_changed = target.copyImage(source);
            }//end if
        } catch (Exception ex) {
        }
        return has_changed;
    }//end public static boolean copyImage(SubEntry target, SubEntry source, int opt)
    /**
     * Copy header content of the source record to target record, providing that
     * the option value indicating that is the case.
     * @param target
     * @param source
     * @param opt
     * @return true if the option is set and the operation 
     * is carried out successful. false otherwise.
     */
    public static boolean copyHeader(SubEntry target, SubEntry source, int opt) {
        boolean has_changed = false;
        try {
            if (isCP_HEADER(opt)) {
                has_changed = target.copyHeader(source);
            }//end if
        } catch (Exception ex) {
        }
        return has_changed;
    }//end public static boolean copyImage(SubEntry target, SubEntry source, int opt)
    /**
     * Cut textual content of target record, providing that
     * the option value indicating that is the case.
     * @param target
     * @param opt
     * @return true if the option is set and the operation 
     * is carried out successful. false otherwise.
     */
    public static boolean cutText(SubEntry target, int opt) {
        boolean has_changed = false;
        try {
            if (isCP_TEXT(opt)) {
                has_changed = target.cutText();
            }//end if
        } catch (Exception ex) {
        }
        return has_changed;
    }//end public static boolean cutText(SubEntry target, int opt) {
    /**
     * Cut timing content of target record, providing that
     * the option value indicating that is the case.
     * @param target
     * @param opt
     * @return true if the option is set and the operation 
     * is carried out successful. false otherwise.
     */
    public static boolean cutTime(SubEntry target, int opt) {
        boolean has_changed = false;
        try {
            if (isCP_TIME(opt)) {
                has_changed = target.cutTime();
            }//end if
        } catch (Exception ex) {
        }
        return has_changed;
    }//end public static boolean cutTime(SubEntry target, int opt) {
    /**
     * Cut image content of target record, providing that
     * the option value indicating that is the case.
     * @param target
     * @param opt
     * @return true if the option is set and the operation 
     * is carried out successful. false otherwise.
     */
    public static boolean cutImage(SubEntry target, int opt) {
        boolean has_changed = false;
        try {
            if (isCP_IMAGE(opt)) {
                has_changed = target.cutImage();
            }//end if
        } catch (Exception ex) {
        }
        return has_changed;
    }//end public static boolean cutImage(SubEntry target, int opt) {
    /**
     * Cut header content of target record, providing that
     * the option value indicating that is the case.
     * @param target
     * @param opt
     * @return true if the option is set and the operation 
     * is carried out successful. false otherwise.
     */
    public static boolean cutHeader(SubEntry target, int opt) {
        boolean has_changed = false;
        try {
            if (isCP_HEADER(opt)) {
                has_changed = target.cutHeader();
            }//end if
        } catch (Exception ex) {
        }
        return has_changed;
    }//end public static boolean cutHeader(SubEntry target, int opt)
        
}//end RecordComponent

