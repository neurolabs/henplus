/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: SQLMetaData.java,v 1.2 2004-01-27 18:16:33 hzeller Exp $ 
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

    /**
     * @return
     */
    public SortedSet getTables() {
        return _tables;
    }
    
    public void addTable(Table table) {
        _tables.add(table);
    }

}
