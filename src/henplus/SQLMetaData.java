/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: SQLMetaData.java,v 1.3 2004-03-07 14:22:02 hzeller Exp $ 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus;

import henplus.sqlmodel.Table;

import java.util.SortedSet;
import java.util.TreeSet;

public final class SQLMetaData {
    
    public static final int NOT_INITIALIZED = -1;
    
    private SortedSet /*<Table>*/ _tables;
    
    public SQLMetaData() {
        _tables = new TreeSet();
    }

    public SortedSet getTables() {
        return _tables;
    }
    
    public void addTable(Table table) {
        _tables.add(table);
    }
}
