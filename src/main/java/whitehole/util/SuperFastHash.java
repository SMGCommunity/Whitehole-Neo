/*
 * Copyright (C) 2022 Whitehole Team
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

import whitehole.db.FieldHashes;
import whitehole.smg.Bcsv;

public class SuperFastHash {
    public static long calculate(byte[] data, long start, int offset, int len) {
        int hash = (int) start;
        int pos = offset;

        if (len <= 0)
            return hash & 0xFFFFFFFFL;

        int rem = len & 3;
        int count = len >>> 2;

        for (; count > 0; count--) {
            int low = (data[pos++] & 0xFF) | ((data[pos++] & 0xFF) << 8);
            int high = (data[pos++] & 0xFF) | ((data[pos++] & 0xFF) << 8);

            hash += low;
            int tmp = (high << 11) ^ hash;
            hash = (hash << 16) ^ tmp;
            hash += (hash >>> 11);
        }

        switch (rem) {
            case 3:
                hash += (data[pos++] & 0xFF) | ((data[pos++] & 0xFF) << 8);
                hash ^= (hash << 16);
                hash ^= (data[pos++] & 0xFF) << 18;
                hash += (hash >>> 11);
                break;

            case 2:
                hash += (data[pos++] & 0xFF) | ((data[pos++] & 0xFF) << 8);
                hash ^= (hash << 11);
                hash += (hash >>> 17);
                break;

            case 1:
                hash += (data[pos++] & 0xFF);
                hash ^= (hash << 10);
                hash += (hash >>> 1);
                break;
        }

        hash ^= (hash << 3);
        hash += (hash >>> 5);
        hash ^= (hash << 4);
        hash += (hash >>> 17);
        hash ^= (hash << 25);
        hash += (hash >>> 6);

        return hash & 0xFFFFFFFFL;
    }
    
    public static void addToLookup(String fieldName)
    {
        // Check if field is actually just a hash
        if (Bcsv.getNameHash(fieldName) != Bcsv.calcJGadgetHash(fieldName))
            return;
        
        FieldHashes.addHashToBaseTable(fieldName);
    }
}
