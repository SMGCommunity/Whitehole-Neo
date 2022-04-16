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

import java.util.Enumeration;
import java.util.Iterator;

public class AdaptedEnumeration<E> implements Enumeration<E> {
    private final Iterator<E> iterator;
    
    public AdaptedEnumeration(Iterator i) {
        this.iterator = i;
    }
    
    public Iterator getIterator() {
        return iterator;
    }
    
    @Override
    public boolean hasMoreElements() {
        return iterator.hasNext();
    }
    
    @Override
    public E nextElement() {
        return iterator.next();
    }
}