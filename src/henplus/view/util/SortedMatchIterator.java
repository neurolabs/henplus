/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SortedMatchIterator.java,v 1.2 2004-03-07 11:59:29 hzeller Exp $ 
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
 * This Iterator is commonly used for TAB-completion..
 */
public class SortedMatchIterator implements Iterator {
    private final Iterator _it;
    private final String _match;

    private String _prefix;
    private String _suffix;

    /** the current match */
    private String _current;

    /**
     * Return all Key-Elements from the given Iterator that have the common
     * prefix given in 'match'. The Iterator must provide a sorted
     * sequence of the potential matches that is placed on the first match.
     */
    public SortedMatchIterator(String match, Iterator/*<String>*/ it) {
        _match = match;
        _it = it;
    }

    /**
     * Return all Key-Elements from the given SortedSet that have the common
     * prefix given in 'match'.
     */
    public SortedMatchIterator(String match, SortedSet/*<String>*/ set) {
        this(match, set.tailSet(match).iterator());
    }

    /**
     * Return all Key-Elements from the given SortedMap that have the common
     * prefix given in 'match'.
     */
    public SortedMatchIterator(String match, SortedMap/*<String>*/ map) {
        this(match, map.tailMap(match).keySet().iterator());
    }

    /**
     * If a prefix is set, then return the matching element with
     * with this prefix prepended.
     */
    public void setPrefix(String prefix) {
        _prefix = prefix;
    }

    /**
     * If a suffix is set, then return the matching element with
     * with this suffix appended.
     */
    public void setSuffix(String suffix) {
        _suffix = suffix;
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

    public Object next() { 
        String result = _current; 
        if (_prefix != null) result = _prefix + result;
        if (_suffix != null) result = result + _suffix;
        return result;
    }
    public void remove() { 
        throw new UnsupportedOperationException("no!");
    }
}
