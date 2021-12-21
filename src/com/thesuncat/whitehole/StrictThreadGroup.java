/*
    © 2012 - 2019 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.thesuncat.whitehole;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import javax.swing.JOptionPane;

public class StrictThreadGroup extends ThreadGroup {
    public StrictThreadGroup() {
        super("StrictThreadGroup");
    }
    
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (e.getMessage().contains("Method 'gl") && e.getMessage().contains("' not available")) {
            JOptionPane.showMessageDialog(null, 
                    e.getMessage() + "\n\n"
                    + "This error is likely caused by an outdated video driver. Update it if possible.",
                    Whitehole.NAME, JOptionPane.ERROR_MESSAGE);
            
            return;
        }
        
        JOptionPane.showMessageDialog(null, 
                "An unhandled exception has occured: " + e.getMessage() + "\n"
                + "Whitehole may be unstable. It is recommended that you close it now. You can try to save your unsaved work before doing so, but at your own risks.\n\n"
                + "You should report this crash in Under Mario's Hat ("+Whitehole.CRASHURL+"), providing the detailed report found in \"whiteholeCrash.txt\".",
                Whitehole.NAME, JOptionPane.ERROR_MESSAGE);
        
        try {
            File report = new File("whiteholeCrash.txt");
            if (report.exists()) report.delete();
            report.createNewFile();
            PrintStream ps = new PrintStream(report);
            ps.append(Whitehole.NAME + " crash report\r\n");
            ps.append("Please report this in Under Mario's Hat ("+Whitehole.WEBURL+") with all the details below\r\n");
            ps.append("--------------------------------------------------------------------------------\r\n\r\n");
            e.printStackTrace(ps);
            ps.close();
        }
        catch (IOException ex) {}
    }
}