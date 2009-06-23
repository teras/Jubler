package com.panayotis.jubler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Other {

    public static void main(String[] args) {

        System.out.println(getFreePort());
        
        int i = 53281;
        try {
            Socket soc = new Socket((String)null, i);
            BufferedReader in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));

            out.write("help");
            out.newLine();
            out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    
    private static int getFreePort() {
        ServerSocket soc;
        for (int i = 50000; i < 51000; i++) {
            soc = null;
            try {
                soc = new ServerSocket(i);
            } catch (Exception ex) {
            }
            if (soc != null) {
                try {
                    soc.close();
                } catch (IOException ex) {
                }
                return i;
            }
        }
        return -1;
    }
}