/*
 * Copyright (C) 2025 Whitehole Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whitehole.util;

import com.jagrosh.discordipc.*;
import com.jagrosh.discordipc.entities.ActivityType;
import com.jagrosh.discordipc.entities.DisplayType;
import com.jagrosh.discordipc.entities.RichPresence.Builder;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.JFrame;
import whitehole.Settings;
import whitehole.Settings.RPC_STATE;

/**
 *
 * @author Lord-Giganticus
 */
public final class Discord {
    private IPCClient client = null;
    private Builder builder = null;
    private final LinkedHashMap<JFrame, FrameRef> Frames = new LinkedHashMap<>();
    private boolean enabled = true;
    private boolean detailed = false;
    private final class FrameRef {
        private final JFrame frame;
        private String firstRow;
        private String secondRow;
        public FrameRef(JFrame f, String d, String s) {
            frame = f;
            firstRow = d;
            secondRow = s;
        }
        public String getFirstRow() {
            return firstRow;
        }
        public String getSecondRow() {
            return secondRow;
        }
        public JFrame getFrame() {
            return frame;
        }
        public void setFirstRow(String val) {
            firstRow = val;
        }
        public void setSecondRow(String val) {
            secondRow = val;
        }
    }

    public Discord() {
        int state = Settings.getDiscordRichPresenceState();
        enabled = state != RPC_STATE.OFF.ordinal();
        detailed = state == RPC_STATE.DETAILED.ordinal();
        try {
            // Currently uses Lord-G's Neo ID, please inform if any assets need change
            client = new IPCClient(1418260169854423142L);
            builder = new Builder();
            builder.setLargeImage("icon");
            builder.setStartTimestamp(OffsetDateTime.now());
        } catch (Exception ex) {
            System.err.println("Discord RPC had an error upon startup (" + ex.getLocalizedMessage() + "). Disabling temporarily.");
            if (Settings.getDebugAdditionalLogs())
                ex.printStackTrace();
            enabled = false;
        }
        
    }
    
    /**
     * Tries to initialize discord RPC. If it doesn't work, it disables RPC.
     */
    public void init() {
        if (!enabled) return;
        try {
            client.connect();
            builder.setActivityType(ActivityType.PLAYING);
            builder.setDisplayType(DisplayType.NAME);
        } catch (Exception ex) {
            System.err.println("Discord RPC had an error upon startup (" + ex.getLocalizedMessage() + "). Disabling temporarily.");
            if (Settings.getDebugAdditionalLogs())
                ex.printStackTrace();
            enabled = false;
        }
    }
    
    /**
     * Adds a new status associated with the frame for Whitehole Neo if the frame isn't already present.
     * If it is present, it updates the existing frame's status with the new status.
     * @param frame The associated frame.
     * @param firstRow The first row in the status. It should be generic (ex. "Editing a Galaxy").
     * @param secondRow The second row in the status. It should give details (ex. "Flip Swap Galaxy"). Only appears if the user sets the RPC state to "Detailed".
     */
    public void updateStatus(JFrame frame, String firstRow, String secondRow) {
        if (!enabled) return;
        var frameRef = new FrameRef(frame, firstRow, secondRow);
        
        // re-add it
        if (Frames.containsKey(frame)) {
            Frames.remove(frame); 
        }
        Frames.put(frame, frameRef);
        setFirstAndSecondRow(firstRow, secondRow);
    }
    
    /**
     * Removes a frame from the frame list. Meant to be used when the associated frame is closing.
     * @param frame The frame associated with the status to remove.
     */
    public void removeStatus(JFrame frame) {
        if (!enabled || !Frames.containsKey(frame)) return;
        // Check if it is at the top before we remove it.
        if (topFrameRef().frame == frame) {
            Frames.remove(frame);
            var newTop = topFrameRef();
            if (newTop == null)
                return;
            setFirstAndSecondRow(newTop.firstRow, newTop.secondRow);
        }
        else {
            Frames.remove(frame);
        }
    }
    
    /**
     * Finds if a frame exists.
     * @param frame
     * @return 
     */
    public Boolean frameExists(JFrame frame) {
        return Frames.containsKey(frame);
    }
    
    /**
     * Gets the topmost FrameRef.
     * @return 
     */
    private FrameRef topFrameRef() {
        FrameRef last = null;
        for (FrameRef f : Frames.values()) {
            last = f;
        }
        return last;
    }
    
    /**
     * Sets the first and second row in the discord RPC.
     * @param firstRow The first row.
     * @param secondRow The second row. Only appears if {@link detailed} is enabled.
     */
    private void setFirstAndSecondRow(String firstRow, String secondRow) {
        if (!enabled) return;
        builder.setDetails(firstRow);
        if (detailed)
            builder.setState(secondRow);
        client.sendRichPresence(builder.build());
    }
}