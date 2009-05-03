/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: Table.java,v 1.5 2004-09-22 11:49:32 magrokosmos Exp $
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus.sqlmodel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

public final class Table implements Comparable<Table> {

    private String _name;
    private final LinkedHashMap<String, Column> _columns;

    // private PrimaryKey _pk;

    // FIXME: add notion of schema.

    public Table(final String name) {
        _name = name;
        _columns = new LinkedHashMap<String, Column>();
    }

    public String getName() {
        return _name;
    }

    public void setName(final String string) {
        _name = string;
    }

    public void addColumn(final Column column) {
        _columns.put(column.getName(), column);
    }

    public Iterator<Column> getColumnIterator() {
        Iterator<Column> result = null;
        if (_columns != null) {
            result = _columns.values().iterator();
        }
        return result;
    }

    public Column getColumnByName(final String name, final boolean ignoreCase) {
        Column result = null;
        if (_columns != null) {
            result = _columns.get(name);
            if (result == null && ignoreCase) {
                final Iterator<String> iter = _columns.keySet().iterator();
                while (iter.hasNext()) {
                    final String colName = iter.next();
                    if (colName.equalsIgnoreCase(name)) {
                        result = _columns.get(colName);
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * @return <code>true</code>, if this <code>Table</code> has any foreign
     *         key, otherwise <code>false</code>.
     */
    public boolean hasForeignKeys() {
        return getForeignKeys() != null;
    }

    /**
     * @return A <code>Set</code> of <code>ColumnFkInfo</code> objects or
     *         <code>null</code>.
     */
    public Set<ColumnFkInfo> getForeignKeys() {
        Set<ColumnFkInfo> result = null;

        if (_columns != null) {
            final Iterator<Column> iter = _columns.values().iterator();
            while (iter.hasNext()) {
                final Column c = iter.next();
                if (c.getFkInfo() != null) {
                    if (result == null) {
                        result = new HashSet<ColumnFkInfo>();
                    }
                    result.add(c.getFkInfo());
                }
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return _name;
    }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object other) {
        boolean result = false;

        if (other == this) {
            result = true;
        } else if (other instanceof Table) {
            final Table o = (Table) other;

            if (_name != null && _name.equals(o.getName())) {
                result = true;
            } else if (_name == null && o.getName() == null) {
                result = true;
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Table other) {
        return _name.compareTo(other.getName());
    }

    /*
     * public boolean columnIsPartOfPk(String column) { boolean result = false;
     * if (_pk != null) { result = _pk.columnParticipates(column); } return
     * result; }
     */

    /**
     * @return
     */
    /*
     * public PrimaryKey getPk() { return _pk; }
     */

    /**
     * @param key
     */
    /*
     * public void setPk(PrimaryKey key) { _pk = key; }
     */

}
