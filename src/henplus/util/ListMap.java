/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: ListMap.java,v 1.3 2004-03-07 14:22:03 hzeller Exp $ 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * This provides the functionality of LinkedHashMap. However, that
 * Collection became available at 1.4. So provide this for backward
 * compatibility.
 *
 * @author Martin Grotzke
 */
public final class ListMap implements Map, Serializable {

    private List keys;
    private List values;

    public ListMap() {
        keys = new ArrayList();
        values = new ArrayList();
    }
    public int size() {
        return keys.size();
    }
    public boolean isEmpty() {
        return keys.isEmpty();
    }
    public boolean containsKey(Object key) {
        return keys.contains(key);
    }
    public boolean containsValue(Object value) {
        return values.contains(value);
    }
    public Object get(Object key) {
        int index = keys.indexOf(key);
        return (index > -1) ? values.get(index) : null;
    }
    public Object put(Object key, Object value) {
        Object orgValue = get(key);
        keys.add(key);
        values.add(value);
        return orgValue;
    }
    public Object remove(Object key) {
        Object orgValue = get(key);
        keys.remove(key);
        values.remove(orgValue);
        return orgValue;
    }
    public void putAll(Map t) {
        /**@todo Implement this java.util.Map method*/
        throw new java.lang.UnsupportedOperationException(
            "Method putAll() not yet implemented.");
    }
    public void clear() {
        keys.clear();
        values.clear();
    }
    public Set keySet() {
        return new HashSet( (Collection)((ArrayList)values).clone() );
    }
    /**
     * Returns a <code>List</code> containing all keys.
     * @return a <code>List</code> containing all keys.
     */
    public List keys() {
        return keys;
    }
    /**
     * Returns a <code>ListIterator</code> over the keys.
     * Use this method instead of combining the <code>keySet</code> with it's <code>iterator</code> method.
     */
    public ListIterator keysListIterator() {
        return keys.listIterator();
    }
    /**
     * Returns the values as a <code>Collection</code>, as defined in <code>java.util.Map</code>.
     */
    public Collection values() {
        return (Collection) ((ArrayList)values).clone();
    }
    /**
     * Returns the values as a <code>List</code>.
     */
    public List valuesList() {
        return (List) ((ArrayList)values).clone();
    }
    /**
     * Returns a <code>ListIterator</code> over the values.
     */
    public ListIterator valuesListIterator() {
        return values.listIterator();
    }
    public Set entrySet() {
        /**@todo Implement this java.util.Map method*/
        throw new java.lang.UnsupportedOperationException(
            "Method entrySet() not yet implemented.");
    }
    public boolean equals(Object o) {
        /**@todo Implement this java.util.Map method*/
        throw new java.lang.UnsupportedOperationException(
            "Method equals() not yet implemented.");
    }
}
