/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SortedMatchIterator.java,v 1.1 2004-03-06 00:15:28 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.view.util;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.SortedMap;

/**
 * The usual Iterator used to return matching values.
 * This Iterator is initialized with a sorted Set or another
 * Iterator that must be placed at the beginning of the matching
 * area of a sorted set.
 */
public class SortedMatchIterator implements Iterator {
    private final Iterator _it;
    private final String _match;
    private String _current;

    public SortedMatchIterator(String match, Iterator/*<String>*/ it) {
        _match = match;
        _it = it;
    }

    public SortedMatchIterator(String match, SortedSet/*<String>*/ set) {
        this(match, set.tailSet(match).iterator());
    }

    public SortedMatchIterator(String match, SortedMap/*<String>*/ map) {
        this(match, map.tailMap(match).keySet().iterator());
    }

    
    public boolean hasNext() {
        while (_it.hasNext()) {
            _current = (String) _it.next();
            if (_current.length() == 0) {
                continue;
            }
            if (!_current.startsWith(_match)) {
                return false;
            }
            if (exclude(_current)) {
                continue;
            }
            return true;
        }
        return false;
    }

    protected boolean exclude(String current) { return false; }

    public Object next() { return _current; }
    public void remove() { 
        throw new UnsupportedOperationException("no!");
    }
}
