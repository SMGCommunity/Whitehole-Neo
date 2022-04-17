/*
    © 2012 - 2017 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.aurum.whitehole;

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