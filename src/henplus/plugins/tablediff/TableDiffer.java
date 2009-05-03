/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 */
package henplus.plugins.tablediff;

import henplus.sqlmodel.Column;
import henplus.sqlmodel.Table;

import java.util.Iterator;
import java.util.ListIterator;

/**
 * <p>
 * Title: TableDiffer
 * </p>
 * <p>
 * Description:<br>
 * Created on: 24.07.2003
 * </p>
 * 
 * @version $Id: TableDiffer.java,v 1.2 2004-01-27 18:16:33 hzeller Exp $
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public final class TableDiffer {

    /**
     * Compares two tables by their columns.
     * 
     * @param referenceTable
     * @param diffTable
     * @param colNameIgnoreCase
     *            specifies if column names shall be compared in a case
     *            insensitive way.
     * @return An instance of <code>TableDiffResult</code> if their are
     *         differences between the tables, otherwise <code>null</code>.
     */
    public static TableDiffResult diffTables(final Table referenceTable,
            final Table diffTable, final boolean colNameIgnoreCase) {
        TableDiffResult result = null;

        if (referenceTable != null && diffTable != null) {
            result = new TableDiffResult();
            // first check for all columns of the reference table
            final Iterator<Column> refIter = referenceTable.getColumnIterator();
            if (refIter != null) {
                while (refIter.hasNext()) {
                    final Column col = (Column) refIter.next();
                    // System.out.println(
                    // "[TableDiffer.diffTables] querying table for " +
                    // col.getName());
                    final Column diff = diffTable.getColumnByName(col.getName(),
                            colNameIgnoreCase);
                    // System.out.println("[TableDiffer.diffTables] got: " +
                    // diff);
                    if (diff == null) {
                        // System.out.println("missing col: " + col.getName());
                        result.addRemovedColumn(col);
                    } else if (!col.equals(diff, colNameIgnoreCase)) {
                        // System.out.println("modified col: " + col.getName());
                        result.putModifiedColumns(col, diff);
                    }
                }
            }
            // now check for columns which were added to the second table
            final Iterator<Column> diffIter = diffTable.getColumnIterator();
            if (diffIter != null) {
                while (diffIter.hasNext()) {
                    final Column col = diffIter.next();
                    final Column ref = referenceTable.getColumnByName(col.getName(),
                            colNameIgnoreCase);
                    if (ref == null) {
                        // System.out.println("added col: " + col.getName());
                        result.addAddedColumn(col);
                    }
                }
            }
            result = result.hasDiffs() ? result : null;
        }
        return result;
    }

}
