/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: NameCompleter.java,v 1.1 2003-05-01 16:50:45 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.util;

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
    private final SortedSet nameSet;
    private final SortedMap canonicalNames;

    public NameCompleter() {
	nameSet = new TreeSet();
	canonicalNames = new TreeMap();
    }
    
    public NameCompleter(Iterator names) {
	this();
	while (names.hasNext()) {
	    addName((String) names.next());
	}
    }

    public NameCompleter(Collection c) {
	this(c.iterator());
    }

    public NameCompleter(String names[]) {
        this();
        for (int i=0; i < names.length; ++i) {
            addName(names[i]);
        }
    }

    public void addName(String name) {
	nameSet.add(name);
	canonicalNames.put(name.toLowerCase(), name);
    }
    
    public Iterator getAllNames() {
        return nameSet.iterator();
    }

    /**
     * returns an iterator with alternatives that match the partial name
     * given or 'null' if there is no alternative.
     */
    public Iterator getAlternatives(String partialName) {
	// first test, if we find the name directly
	Iterator testIt = nameSet.tailSet(partialName).iterator();
	String testMatch = (testIt.hasNext()) ? (String) testIt.next() : null;

	if (testMatch == null || !testMatch.startsWith(partialName)) {
	    String canonical = partialName.toLowerCase();
	    testIt = canonicalNames.tailMap(canonical).keySet().iterator();
	    testMatch = (testIt.hasNext()) ? (String) testIt.next() : null;
	    if (testMatch == null || !testMatch.startsWith(canonical))
		return null; // nope.
	    String foundName = (String) canonicalNames.get(testMatch);
	    partialName = foundName.substring(0, partialName.length());
	}

	final Iterator nameIt = nameSet.tailSet(partialName).iterator();
	final String   namePattern  = partialName;

	return new Iterator() {
		String current = null;
		public boolean hasNext() {
		    if (nameIt.hasNext()) {
			current = (String) nameIt.next();
			if (current.startsWith(namePattern))
			    return true;
		    }
		    return false;
		}
		public Object  next() {
		    return current;
		}
		public void remove() { 
		    throw new UnsupportedOperationException("no!");
		}
	    };
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
