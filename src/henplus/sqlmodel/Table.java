/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: Table.java,v 1.4 2004-06-07 08:31:56 hzeller Exp $ 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus.sqlmodel;

import henplus.util.ListMap;

import java.util.Iterator;
import java.util.ListIterator;

public final class Table {
    private String _name;
    private ListMap /*<String, Column>*/ _columns;

    // private PrimaryKey _pk;
    
    // FIXME: add notion of schema.

    public Table(String name) {
        _name = name;
        _columns = new ListMap();
    }

    public String getName() {
        return _name;
    }

    public void setName(String string) {
        _name = string;
    }
    
    public void addColumn(Column column) {
        _columns.put(column.getName(), column);
    }
    
    public ListIterator getColumnIterator() {
        ListIterator result = null;
        if (_columns != null) {
            result = _columns.valuesListIterator();
        }
        return result;
    }
    
    public Column getColumnByName(String name, boolean ignoreCase) {
        Column result = null;
        if (_columns != null) {
            result = (Column)_columns.get(name);
            if (result == null && ignoreCase) {
                final Iterator iter = _columns.keysListIterator();
                while (iter.hasNext()) {
                    String colName = (String)iter.next();
                    if (colName.equalsIgnoreCase(name)) {
                        result = (Column)_columns.get(colName);
                        break;
                    }
                }
            }
        }
        return result;
    }
    
    /*
    public boolean columnIsPartOfPk(String column) {
        boolean result = false;
        if (_pk != null) {
            result = _pk.columnParticipates(column);
        }
        return result;
    }
    */

    /**
     * @return
     */
    /*
    public PrimaryKey getPk() {
        return _pk;
    }
    */

    /**
     * @param key
     */
    /*
    public void setPk(PrimaryKey key) {
        _pk = key;
    }
    */

}
