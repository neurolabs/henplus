/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: SQLMetaData.java,v 1.4 2004-06-07 08:31:56 hzeller Exp $
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus;

import henplus.sqlmodel.Table;

import java.util.SortedSet;
import java.util.TreeSet;

public final class SQLMetaData {
    public static final int NOT_INITIALIZED = -1;

    private final SortedSet<Table> _tables;

    public SQLMetaData() {
        _tables = new TreeSet();
    }

    public SortedSet<Table> getTables() {
        return _tables;
    }

    public void addTable(final Table table) {
        _tables.add(table);
    }
}
