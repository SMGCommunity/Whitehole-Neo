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
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import com.jagrosh.discordipc.entities.RichPresence.Builder;
import java.time.OffsetDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import whitehole.Settings;

/**
 *
 * @author Lord-Giganticus
 */
public final class Discord {
    private IPCClient client = null;
    private Builder builder = null;
    private final ArrayList<FrameRef> Frames = new ArrayList<>();
    private boolean enabled = true;
    private final class FrameRef {
        private final JFrame frame;
        private String desc;
        private String status;
        public FrameRef(JFrame f, String d, String s) {
            frame = f;
            desc = d;
            status = s;
        }
        public String getDesc() {
            return desc;
        }
        public String getStatus() {
            return status;
        }
        public JFrame getFrame() {
            return frame;
        }
        public void setDesc(String new_desc) {
            desc = new_desc;
        }
        public void setStatus(String new_status) {
            status = new_status;
        }
    }

    public Discord() {
        if (!Settings.getDiscordRichPresenceEnabled())
        {
            enabled = false;
        }
        try {
            // Currently uses Lord-G's Neo ID, please inform if any assets need change
            client = new IPCClient(1418260169854423142L);
            builder = new Builder();
            builder.setLargeImage("icon");
            builder.setStartTimestamp(OffsetDateTime.now());
        } catch (Exception ex) {
            System.err.println("Unexpected error while constructing Discord RPC. Disabling.");
            if (Settings.getDebugAdditionalLogs())
                ex.printStackTrace();
            enabled = false;
            Settings.setDiscordRichPresenceEnabled(false);
        }
        
    }
    public void init() {
        if (!enabled) return;
        try {
            client.connect();
            return;
        } catch (NoDiscordClientException ex) {
            System.out.println("Discord not found. Disabling.");
        } catch (Exception ex) {
            System.err.println("Unexpected error while initializing Discord RPC. Disabling.");
            ex.printStackTrace();
        }
        enabled = false;
        Settings.setDiscordRichPresenceEnabled(false);
    }
    public void setDesc(String desc) {
        if (!enabled) return;
        builder.setDetails(desc);
        client.sendRichPresence(builder.build());
    }
    public void setStatus(String status) {
        if (!enabled) return;
        builder.setState(status);
        client.sendRichPresence(builder.build());
    }
    public void setDescAndStatus(String desc, String status) {
        if (!enabled) return;
        builder.setDetails(desc);
        builder.setState(status);
        client.sendRichPresence(builder.build());
    }
    public void close() {
        if (!enabled) return;
        try {
            client.close();
        } catch (Exception ex) {
            System.err.println("Unexpected error while closing Discord RPC.");
            ex.printStackTrace();
            enabled = false;
            Settings.setDiscordRichPresenceEnabled(false);
        }
    }
    private FrameRef Last() {
        return Frames.get(Frames.size() - 1);
    }
    public void addFrame(JFrame frame, String desc, String status) {
        if (!enabled) return;
        FrameRef ref = new FrameRef(frame, desc, status);
        Frames.add(ref);
        setDescAndStatus(desc, status);
    }
    public void removeFrame(JFrame frame) {
        if (!enabled) return;
        for (int i = 0; i < Frames.size(); i++) {
            FrameRef f = Frames.get(i);
            if (f.getFrame() == frame) {
                Frames.remove(i);
                FrameRef last = Last();
                setDescAndStatus(last.getDesc(), last.getStatus());
            }
        }
    }
    public Boolean frameExists(JFrame frame) {
        if (!enabled) return false;
        for (FrameRef frameRef : Frames) {
            if (frameRef.getFrame() == frame)
                return true;
        }
        return false;
    }
    public FrameRef findFrame(JFrame frame) {
        if (!enabled) return null;
        for (var ref : Frames) {
            if (ref.getFrame() == frame)
                return ref;
        }
        return null;
    }
    public void setFrame(JFrame frame, String desc, String status) {
        if (!enabled) return;
        for (FrameRef frameRef : Frames) {
            if (frameRef.getFrame() == frame) {
                frameRef.setDesc(desc);
                frameRef.setStatus(status);
                if (Last() == frameRef) {
                    setDescAndStatus(desc, status);
                }
            }
        }
    }
    public void addOrSetFrame(JFrame frame, String desc, String status) {
        if (!enabled) return;
        if (!frameExists(frame)) {
            addFrame(frame, desc, status);
        } else {
            setFrame(frame, desc, status);
        }
    }
}