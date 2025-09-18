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

/**
 *
 * @author Lord-Giganticus
 */
public final class Discord {
    private final IPCClient client;
    private final Builder builder;
    public Discord() {
        // Currently uses Lord-G's Neo ID, please inform if any assets need change
        client = new IPCClient(1418260169854423142L);
        builder = new Builder();
        builder.setLargeImage("icon");
        builder.setStartTimestamp(OffsetDateTime.now());
    }
    public void init() {
        try {
            client.connect();
        } catch (NoDiscordClientException ex) {
            Logger.getLogger(Discord.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void setDesc(String desc) {
        builder.setDetails(desc);
        client.sendRichPresence(builder.build());
    }
    public void setStatus(String status) {
        builder.setState(status);
        client.sendRichPresence(builder.build());
    }
    public void close() {
        client.close();
    }
}