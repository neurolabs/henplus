/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: NameCompleter.java,v 1.5 2004-03-23 11:05:38 magrokosmos Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.view.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.TreeMap;

/**
 * a Completer for names that are only given partially. This is used for
 * tab-completion or to automatically correct names.
 */
public class NameCompleter {
    private final SortedSet<String> _nameSet;
    private final SortedMap<String, String> _canonicalNames;

    public NameCompleter() {
        _nameSet = new TreeSet<String>();
        _canonicalNames = new TreeMap<String, String>();
    }

    public NameCompleter(final Iterator<String> names) {
        this();
        while (names.hasNext()) {
            addName(names.next());
        }
    }

    public NameCompleter(final Collection<String> c) {
        this(c.iterator());
    }

    public NameCompleter(final String names[]) {
        this();
        for (int i = 0; i < names.length; ++i) {
            addName(names[i]);
        }
    }

    public void addName(final String name) {
        _nameSet.add(name);
        _canonicalNames.put(name.toLowerCase(), name);
    }

    public Iterator<String> getAllNamesIterator() {
        return _nameSet.iterator();
    }

    public SortedSet<String> getAllNames() {
        return _nameSet;
    }

    public String findCaseInsensitive(String name) {
        if (name == null) {
            return null;
        }
        return _canonicalNames.get(name.toLowerCase());
    }

    /**
     * returns an iterator with alternatives that match the partial name given
     * or 'null' if there is no alternative.
     */
    public Iterator<String> getAlternatives(String partialName) {
        // first test, if we find the name directly
        Iterator<String> testIt = _nameSet.tailSet(partialName).iterator();
        String testMatch = testIt.hasNext() ? (String) testIt.next() : null;
        if (testMatch == null || !testMatch.startsWith(partialName)) {
            final String canonical = partialName.toLowerCase();
            testIt = _canonicalNames.tailMap(canonical).keySet().iterator();
            testMatch = testIt.hasNext() ? (String) testIt.next() : null;
            if (testMatch == null || !testMatch.startsWith(canonical)) {
                return null; // nope.
            }
            final String foundName = _canonicalNames.get(testMatch);
            partialName = foundName.substring(0, partialName.length());
        }

        return new SortedMatchIterator(partialName, _nameSet);
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
