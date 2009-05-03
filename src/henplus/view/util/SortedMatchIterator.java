/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SortedMatchIterator.java,v 1.4 2004-03-07 15:28:28 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.view.util;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.SortedMap;

/**
 * An Iterator returning end-truncated matching values from a sorted List.
 * <p>
 * This Iterator is initialized with a sorted Set, sorted Map or another
 * Iterator that must be placed at the beginning of the matching area of a
 * sorted set.
 * </p>
 * This Iterator is commonly used for TAB-completion..
 */
public class SortedMatchIterator implements Iterator<String> {
    private final Iterator _it;
    private final String _partialMatch;

    private String _prefix;
    private String _suffix;

    /** the current match */
    private String _current;

    /**
     * Return all Key-Elements from the given Iterator that have the common
     * prefix given in 'partialMatch'. The Iterator must provide a sorted
     * sequence of the potential matches that is placed on the first match.
     * 
     * @param partialMatch
     *            the prefix that should match
     * @param it
     *            the Iterator positioned at the first partial match.
     */
    public SortedMatchIterator(final String partialMatch, final Iterator<String> it) {
        _partialMatch = partialMatch;
        _it = it;
    }

    /**
     * Return all Key-Elements from the given SortedSet that have the common
     * prefix given in 'partialMatch'.
     * 
     * @param partialMatch
     *            the prefix that should match
     * @param set
     *            the SortedSet from which the matches should be iterated.
     */
    public SortedMatchIterator(final String partialMatch, final SortedSet<String> set) {
        this(partialMatch, set.tailSet(partialMatch).iterator());
    }

    /**
     * Return all Key-Elements from the given SortedMap that have the common
     * prefix given in 'partialMatch'.
     * 
     * @param partialMatch
     *            the prefix that should match
     * @param map
     *            the SortedMap from its matching keys the matches should be
     *            iterated.
     */
    public SortedMatchIterator(final String partialMatch, final SortedMap<String, ?> map) {
        this(partialMatch, map.tailMap(partialMatch).keySet().iterator());
    }

    /**
     * If a prefix is set, then return the matching element with with this
     * prefix prepended.
     */
    public void setPrefix(final String prefix) {
        _prefix = prefix;
    }

    /**
     * If a suffix is set, then return the matching element with with this
     * suffix appended.
     */
    public void setSuffix(final String suffix) {
        _suffix = suffix;
    }

    /**
     * Override this method if you want to exclude certain values from the
     * iterated values returned. By default, no value is excluded.
     */
    protected boolean exclude(final String current) {
        return false;
    }

    // -- java.util.Iterator interface implementation
    public boolean hasNext() {
        while (_it.hasNext()) {
            _current = (String) _it.next();
            if (_current.length() == 0) {
                continue;
            }
            if (!_current.startsWith(_partialMatch)) {
                return false;
            }
            if (exclude(_current)) {
                continue;
            }
            return true;
        }
        return false;
    }

    public String next() {
        String result = _current;
        if (_prefix != null) {
            result = _prefix + result;
        }
        if (_suffix != null) {
            result = result + _suffix;
        }
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException("no!");
    }
}
