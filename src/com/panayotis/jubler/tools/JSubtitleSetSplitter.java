/*
 *  SubtitleSetSplitter.java 
 * 
 *  Created on: Jun 22, 2009 at 2:25:54 PM
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
package com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.SubtitleSplitFileFilter;
import java.io.File;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

/**
 * This class splits a subtitle data set into the required number of fragments.
 * Each fragment is a file with a number of subtitle events that has been
 * divided. The result of split action will produce a table of pair-values
 * [file, subtitles] where the file is a computed instance of the original
 * file with a numbered name. Each file will inherit the original file-name
 * with a numerical ending (ie. _01, _02) and the extension provided in the
 * instance of {@link SubtitleSplitFileFilter}. The [subtitles] value is the
 * divident from the data set of the original file. Each set should contain
 * an equal number of divident, apart from the last one, which could have
 * a larger or smaller number of records, depending on the number of records
 * and the divident value. The list of these pair values is expected to be
 * used by external programmes to write the file and the divided content out
 * to a storage medium.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class JSubtitleSetSplitter implements CommonDef{

    private File inputFile = null;
    private Subtitles data = null;
    private int splitNumber = 0;
    private int numRecLoaded = 0;
    private int numRecPerFile = 0;
    private SubtitleSplitFileFilter splitFilefilter = null;
    private Vector<File> fileList = null;

    public JSubtitleSetSplitter() {
    }

    public JSubtitleSetSplitter(Subtitles data, int splitNumber, File inputFile, SubtitleSplitFileFilter filter) {
        this.inputFile = inputFile;
        this.data = data;
        this.splitNumber = splitNumber;
        this.splitFilefilter = filter;
    }

    private void calculateNumRecsPerFile() {
        try {
            numRecLoaded = data.size();
            numRecPerFile = (numRecLoaded / splitNumber);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }

    private void createFileVector() {
        try {
            String input_file_dir = inputFile.getParent();
            String input_file_name = inputFile.getName();
            String file_ext = splitFilefilter.getExtension();
            int ext_len = file_ext.length();
            int name_len = input_file_name.length();
            int name_len_without_extension = name_len - ext_len - 1;

            String input_file_name_extensionless = input_file_name.substring(
                    0,
                    name_len_without_extension);

            int n_digits = String.valueOf(splitNumber).length() + 1;
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMinimumIntegerDigits(n_digits);

            int starting_number = getStartingFileNumber(input_file_dir);
            int ending_number = (starting_number + splitNumber);

            fileList = new Vector<File>();
            for (int i = starting_number; i < ending_number; i++) {
                String fname =
                        input_file_name_extensionless +
                        char_ucore +
                        nf.format(i) +
                        char_dot +
                        file_ext;
                File f = new File(input_file_dir, fname);
                fileList.add(f);
            }//end for (int i=1; i <= num_files_needed; i++)  
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }

    private int getStartingFileNumber(String path) {
        try {
            File dir = new File(path);
            String[] file_list = dir.list(splitFilefilter);
            return splitFilefilter.getNextNumber();
        } catch (Exception ex) {
            return 1;
        }
    }

    private Subtitles getDataSubSet(int from, int to) {
        Subtitles subs = new Subtitles();
        try {
            int len = data.size();
            for (int i = from; i <= to && i < len; i++) {
                SubEntry entry = data.elementAt(i);
                subs.add(entry);
            }//end for (int i = from; i < to; i++)
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return subs;
    }

    private Map<File, Integer[]> getDataRangeMap() {
        Map<File, Integer[]> range_map = new Hashtable<File, Integer[]>();
        try {
            int index = 0;
            int len = data.size();
            for (int i = 0; i < splitNumber; i++) {
                int from = index;
                int to = from + numRecPerFile - 1;
                boolean is_last_file = (i == splitNumber-1);
                if (is_last_file){
                    to = len-1;
                }//end if
                
                Integer[] range = new Integer[2];
                range[0] = Integer.valueOf(
                        Math.min(from, numRecLoaded - 1));
                range[1] =
                        Integer.valueOf(
                        Math.min(to, numRecLoaded - 1));

                File f = fileList.elementAt(i);
                range_map.put(f, range);
                index += numRecPerFile;
            }//end for (int i=0; i < num_files_needed; i++)
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return range_map;
    }

    public Map<File, Subtitles> split() {
        Map<File, Subtitles> data_set = new Hashtable<File, Subtitles>();
        try {
            calculateNumRecsPerFile();
            createFileVector();
            Map<File, Integer[]> range_map = getDataRangeMap();
            for (int i = 0; i < fileList.size(); i++) {
                File f = fileList.elementAt(i);
                Integer[] range = range_map.get(f);
                int from = range[0].intValue();
                int to = range[1].intValue();

                Subtitles subs = getDataSubSet(from, to);
                data_set.put(f, subs);
            }//end for(int = 0; i < fileList.size(); i++)        
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return data_set;
    }

    public Subtitles getData() {
        return data;
    }

    public void setData(Subtitles data) {
        this.data = data;
    }

    public int getSplitNumber() {
        return splitNumber;
    }

    public void setSplitNumber(int splitNumber) {
        this.splitNumber = splitNumber;
    }

    public File getInputFile() {
        return inputFile;
    }

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    public int getNumRecLoaded() {
        return numRecLoaded;
    }

    public void setNumRecLoaded(int numRecLoaded) {
        this.numRecLoaded = numRecLoaded;
    }

    public int getNumRecPerFile() {
        return numRecPerFile;
    }

    public void setNumRecPerFile(int numRecPerFile) {
        this.numRecPerFile = numRecPerFile;
    }

    public Vector<File> getFileList() {
        return fileList;
    }

    public void setFileList(Vector<File> fileList) {
        this.fileList = fileList;
    }

    public SubtitleSplitFileFilter getSplitFilefilter() {
        return splitFilefilter;
    }

    public void setSplitFilefilter(SubtitleSplitFileFilter splitFilefilter) {
        this.splitFilefilter = splitFilefilter;
    }
}//end class SubtitleSetSplitter 

