/*
 * TreeWalker.java
 *
 * Created on October 3, 2006, 3:07 AM
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

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.tools.externals.ExtPath;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Vector;
import java.util.logging.Level;

/**
 *
 * @author teras
 */
public class TreeWalker {

    public static File searchExecutable(String application, String[] parameters, String test_signature, String deflt) {
        Vector<ExtPath> paths = new Vector<ExtPath>();
        paths.add(new ExtPath(deflt, ExtPath.FILE_ONLY));
        SystemDependent.appendSpotlightApplication(application, paths);
        SystemDependent.appendPathApplication(paths);
        SystemDependent.appendLocateApplication(application, paths);

        if (parameters == null) {
            parameters = new String[0];
        }

        for (ExtPath path : paths) {
            String msg = _("Wizard is looking inside {0}", path.getPath());
            DEBUG.logger.log(Level.WARNING, msg);
            File f = new File(path.getPath());
            if (path.searchForFile() && (!f.isFile())) {
                continue;
            }   // If we want a file and this is not, ignore this entry
            File res = searchExecutable(f, application.toLowerCase(), parameters, test_signature, path.getRecStatus());
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    /* filename is already in lower case... */
    public static File searchExecutable(File root, String program, String[] parameters, String test_signature, int recursive) {
        if (!root.exists()) {
            return null;
        }
        if (root.isFile()) {
            if (!root.canRead()) {
                return null;
            }
            if (!root.getName().toLowerCase().equals(program + SystemDependent.PROG_EXT)) {
                return null;
            }
            if (!execIsValid(root, parameters, program, test_signature)) {
                return null;
            }
            /* All checks OK - valid executable! */
            return root;
        } else {
            if (recursive <= ExtPath.FILE_ONLY) {
                return null;
            }   // No more recursive should be done
            recursive--;
            File[] childs = root.listFiles();
            if (childs != null) {
                for (int i = 0; i < childs.length; i++) {
                    File res = searchExecutable(childs[i], program, parameters, test_signature, recursive);
                    if (res != null) {
                        return res;
                    }
                }
            }
        }
        return null;
    }

    public static boolean execIsValid(File exec, String[] parameters, String app_signature, String test_signature) {
        boolean valid = false, sig_valid = false, test_valid = false;
        BufferedReader infopipe_in = null, infopipe_err = null;
        Process proc = null;
        String line_in = null, line_err = null, line = null;
        String[] cmd = new String[parameters.length + 1];
        cmd[0] = exec.getAbsolutePath();
        if (parameters.length > 0) {
            System.arraycopy(parameters, 0, cmd, 1, parameters.length);
        }

        try {
            StringBuffer buf = new StringBuffer();
            buf.append(_("Testing:")).append(" ");
            for (int i = 0; i < cmd.length; i++) {
                buf.append(cmd[i]).append(' ');
            }
            DEBUG.logger.log(Level.WARNING, buf.toString());

            proc = Runtime.getRuntime().exec(cmd);
            InputStream inp = proc.getInputStream();
            InputStream erp = proc.getErrorStream();
            //OutputStream out = proc.getOutputStream();
            
            infopipe_in = new BufferedReader(new InputStreamReader(inp));
            infopipe_err = new BufferedReader(new InputStreamReader(erp));
            line_in = infopipe_in.readLine();
            line_err = infopipe_err.readLine();
            //DEBUG.logger.log(Level.OFF, "pipe-in:" + line_in);
            //DEBUG.logger.log(Level.OFF,  "pipe-err:" + line_err);
            line = (line_in != null ? line_in : line_err);
            while (line != null) {
                boolean must_find_app_signature = !((app_signature == null) || sig_valid);
                if (must_find_app_signature) {
                    sig_valid = line.toLowerCase().contains(app_signature);
                }

                boolean must_find_test_signature = !((test_signature == null) || test_valid);
                if (must_find_test_signature) {
                    test_valid = (!test_valid) && line.toLowerCase().contains(test_signature);
                }else{
                    test_valid = true;
                }
                line = (line_in != null ? infopipe_in.readLine() : infopipe_err.readLine()); 
            }//end while ((line = infopipe.readLine()) != null)
             
            valid = (sig_valid && test_valid);
        } catch (Exception ex) {
        } finally {
            try {
                infopipe_err.close();
            } catch (Exception e) {
            }
            try {
                infopipe_in.close();
            } catch (Exception e) {
            }
            try {
                proc.destroy();
            } catch (Exception e) {
            }
        }
        return valid;
    }
}
