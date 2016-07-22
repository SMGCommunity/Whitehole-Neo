/*
    Copyright 2012 The Whitehole team

    This file is part of Whitehole.

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
    FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole;

import java.io.File;
import java.io.PrintStream;
import javax.swing.JOptionPane;

public class StrictThreadGroup extends ThreadGroup
{
    public StrictThreadGroup()
    {
        super("StrictThreadGroup");
    }
    
    @Override
    public void uncaughtException(Thread t, Throwable e)
    {
        if (e.getMessage().contains("Method 'gl") && e.getMessage().contains("' not available"))
        {
            JOptionPane.showMessageDialog(null, 
                    e.getMessage() + "\n\n"
                    + "This error is likely caused by an outdated video driver. Update it if possible.",
                    Whitehole.fullName, JOptionPane.ERROR_MESSAGE);
            
            return;
        }
        
        JOptionPane.showMessageDialog(null, 
                "An unhandled exception has occured: " + e.getMessage() + "\n"
                + "Whitehole may be unstable. It is recommended that you close it now. You can try to save your unsaved work before doing so, but at your own risks.\n\n"
                + "You should report this crash at EggStarGalaxy ("+Whitehole.crashReportURL+"), providing the detailed report found in \"whiteholeCrash.txt\".",
                Whitehole.fullName, JOptionPane.ERROR_MESSAGE);

        try
        {
            File report = new File("whiteholeCrash.txt");
            if (report.exists()) report.delete();
            report.createNewFile();
            PrintStream ps = new PrintStream(report);
            ps.append(Whitehole.fullName + " crash report\r\n");
            ps.append("Please report this at EggStarGalaxy ("+Whitehole.websiteURL+") with all the details below\r\n");
            ps.append("--------------------------------------------------------------------------------\r\n\r\n");
            e.printStackTrace(ps);
            ps.close();
        }
        catch (Exception ex) {}
    }
}
