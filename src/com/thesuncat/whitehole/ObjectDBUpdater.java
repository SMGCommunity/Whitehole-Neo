/*
    © 2012 - 2021 - Whitehole Team
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

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.nio.charset.*;
import java.util.zip.GZIPInputStream;
import javax.swing.JLabel;

public class ObjectDBUpdater extends Thread {
    public ObjectDBUpdater(JLabel status) {
        statusLabel = status;
    }

    @Override
    public void run() {
        try {
            String ts = String.format("&ts=%1$d", ObjectDB.timestamp);
            URL url = new URL (Settings.objectDB_url + "?whitehole&gzip" + ts);
            URLConnection conn = url.openConnection();
            DataInputStream dis = new DataInputStream(conn.getInputStream());

            int length = conn.getContentLength();
            if (length < 8) {
                if(!Settings.japanese)
                    statusLabel.setText("Failed to update object database: received invalid data.");
                else
                    statusLabel.setText("オブジェクトデータベースをアップデートできませんでした");
                return;
            }

            byte[] data = new byte[length];
            for (int i = 0; i < data.length; i++)
                data[i] = dis.readByte();

            Charset charset = Charset.forName("UTF-8");
            CharsetDecoder dec = charset.newDecoder();
            String strdata = dec.decode(ByteBuffer.wrap(data, 0, 8)).toString();

            if (strdata.equals("noupdate")) {
                if(Settings.japanese)
                    statusLabel.setText("オブジェクトデータベースは既にアップデートされています。");
                else
                    statusLabel.setText("Object database already up-to-date.");
                return;
            }
            else if (data.length < 10) {
                if(Settings.japanese)
                    statusLabel.setText("オブジェクトデータベースを更新できませんでした。無効なデータを受信しました。");
                else
                    statusLabel.setText("Failed to update object database: received invalid data.");
                return;
            }

            CRC32 crc = new CRC32();
            crc.update(data, 9, data.length-9);
            long crcref;
            try { crcref = Long.parseLong(strdata, 16); }
            catch (NumberFormatException ex) { crcref = -1; }
            if (crc.getValue() != crcref) {
                if(Settings.japanese)
                    statusLabel.setText("オブジェクトデータベースを更新できませんでした。無効なデータを受信しました。");
                else
                    statusLabel.setText("Failed to update object database: received invalid data.");
                return;
            }

            File odbbkp = new File("objectdb.xml.bak");
            File odb = new File("objectdb.xml");

            try {
                if (odb.exists()) {
                    odb.renameTo(odbbkp);
                    odb.delete();
                }

                ByteArrayInputStream compstream = new ByteArrayInputStream(data, 9, data.length-9);
                GZIPInputStream gzstream = new GZIPInputStream(compstream);

                odb.createNewFile();
                FileOutputStream odbstream = new FileOutputStream(odb);

                int curbyte;
                while ((curbyte = gzstream.read()) != -1)
                    odbstream.write(curbyte);

                odbstream.flush();
                odbstream.close();

                gzstream.close();
                compstream.close();

                if (odbbkp.exists())
                    odbbkp.delete();
            }
            catch (IOException ex) {
                if(Settings.japanese)
                    statusLabel.setText("オブジェクトデータベースを保存できませんでした。正しいリンクですか？");
                else
                    statusLabel.setText("Could not save the object database. Is the link still valid?");
                if (odbbkp.exists())
                    odbbkp.renameTo(odb);
                return;
            }

            if(Settings.japanese)
                statusLabel.setText("オブジェクトデータベースは通常に更新されました。");
            else
                statusLabel.setText("Object database updated.");
            ObjectDB.init();
        }
        catch (MalformedURLException ex) {
            if(Settings.japanese)
                statusLabel.setText("アップデートサーバーへの接続に失敗しました。");
            else
                statusLabel.setText("Failed to connect to the update server.");
        }
        catch (IOException ex) {
            if(Settings.japanese)
                statusLabel.setText("オブジェクトデータベースを保存できませんでした。正しいリンクですか？");
            else
                statusLabel.setText("Could not save the object database. Is the link still valid?.");
        }
    }

    private final JLabel statusLabel;
} 