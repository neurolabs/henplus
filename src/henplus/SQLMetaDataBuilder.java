/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: SQLMetaDataBuilder.java,v 1.7 2005-06-18 04:58:13 hzeller Exp $
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus;

import henplus.sqlmodel.Column;
import henplus.sqlmodel.ColumnFkInfo;
import henplus.sqlmodel.PrimaryKey;
import henplus.sqlmodel.Table;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public final class SQLMetaDataBuilder {
    final private static String[] LIST_TABLES = { "TABLE" };
    private static final boolean VERBOSE = false;

    // column description
    public static final int TABLE_NAME = 3; // String
    public static final int COLUMN_NAME = 4; // String
    public static final int DATA_TYPE = 5; // int -> java.sql.Types
    public static final int TYPE_NAME = 6; // String
    public static final int COLUMN_SIZE = 7; // int
    public static final int NULLABLE = 11; // int:
    public static final int COLUMN_DEF = 13; // String
    public static final int ORDINAL_POSITION = 17; // int, starting at 1
    /*
     * columnNoNulls - might not allow NULL values columnNullable - definitely
     * allows NULL values columnNullableUnknown - nullability unknown
     */
    public static final int IS_NULLABLE = 17;

    // primary key description
    public static final int PK_DESC_COLUMN_NAME = 4;
    public static final int PK_DESC_KEY_SEQ = 5;
    public static final int PK_DESC_PK_NAME = 6;

    // foreign key description
    private static final int FK_PKTABLE_NAME = 3;
    private static final int FK_PKCOLUMN_NAME = 4;
    private static final int FK_FKCOLUMN_NAME = 8;
    private static final int FK_FK_NAME = 12;

    private boolean _interrupted;

    /*
     * (non-Javadoc)
     * 
     * @see henplus.Interruptable#interrupt()
     */
    public void interrupt() {
        _interrupted = true;
    }

    public SQLMetaData getMetaData(final SQLSession session) {
        ResultSet rset = null;
        final List tableList = new ArrayList();
        try {
            final DatabaseMetaData meta = session.getConnection().getMetaData();
            rset = meta.getTables(null, null, null, LIST_TABLES);
            while (rset.next()) {
                tableList.add(rset.getString(3));
            }
        } catch (final Exception e) {
            // ignore.
        } finally {
            if (rset != null) {
                try {
                    rset.close();
                } catch (final Exception e) {
                }
            }
        }
        return getMetaData(session, tableList);
    }

    public SQLMetaData getMetaData(final SQLSession session,
            final Collection /* <String> */tableNames) {
        return getMetaData(session, tableNames.iterator());
    }

    public SQLMetaData getMetaData(final SQLSession session,
            final Iterator /* <String> */tableNamesIter) {
        final SQLMetaData result = new SQLMetaData();

        ResultSet rset = null;
        try {
            _interrupted = false;
            final String catalog = session.getConnection().getCatalog();

            if (_interrupted) {
                return null;
            }

            final DatabaseMetaData meta = session.getConnection().getMetaData();

            while (tableNamesIter.hasNext() && !_interrupted) {
                final String tableName = (String) tableNamesIter.next();
                rset = meta.getColumns(catalog, null, tableName, null);
                final Table table = buildTable(catalog, meta, tableName, rset);
                result.addTable(table);
            }
        } catch (final Exception e) {
            if (VERBOSE) {
                e.printStackTrace();
            }
            HenPlus.msg().println(
                    "Database problem reading meta data: "
                    + e.getMessage().trim());
        } finally {
            if (rset != null) {
                try {
                    rset.close();
                } catch (final Exception e) {
                }
            }
        }

        return result;
    }

    public Table getTable(final SQLSession session, final String tableName) {
        Table table = null;
        ResultSet rset = null;
        try {
            final String catalog = session.getConnection().getCatalog();
            final DatabaseMetaData meta = session.getConnection().getMetaData();
            rset = meta.getColumns(catalog, null, tableName, null);
            table = buildTable(catalog, meta, tableName, rset);
        } catch (final Exception e) {
            if (VERBOSE) {
                e.printStackTrace();
            }
            HenPlus.msg().println(
                    "Database problem reading meta data: "
                    + e.getMessage().trim());
        } finally {
            if (rset != null) {
                try {
                    rset.close();
                } catch (final Exception e) {
                }
            }
        }
        return table;
    }

    private Table buildTable(final String catalog, final DatabaseMetaData meta,
            final String tableName, final ResultSet rset) throws SQLException {

        Table table = null;
        if (rset != null) {
            table = new Table(tableName);
            final PrimaryKey pk = getPrimaryKey(meta, tableName);
            final Map fks = getForeignKeys(meta, tableName);
            // what about the following duplicate?
            // rset = meta.getColumns(catalog, null, tableName, null);
            while (!_interrupted && rset.next()) {
                final String colname = rset.getString(COLUMN_NAME);
                final Column column = new Column(colname);
                column.setType(rset.getString(TYPE_NAME));
                column.setSize(rset.getInt(COLUMN_SIZE));
                final boolean nullable = rset.getInt(NULLABLE) == DatabaseMetaData.columnNullable ? true
                        : false;
                column.setNullable(nullable);
                final String defaultVal = rset.getString(COLUMN_DEF);
                column.setDefault(defaultVal != null ? defaultVal.trim()
                        : null);
                column.setPosition(rset.getInt(ORDINAL_POSITION));
                column.setPkInfo(pk.getColumnPkInfo(colname));
                column.setFkInfo((ColumnFkInfo) fks.get(colname));

                table.addColumn(column);
            }
            rset.close();
        }
        return table;
    }

    private PrimaryKey getPrimaryKey(final DatabaseMetaData meta, final String tabName)
    throws SQLException {
        PrimaryKey result = null;
        final ResultSet rset = meta.getPrimaryKeys(null, null, tabName);
        if (rset != null) {
            result = new PrimaryKey();
            String pkname = null;
            while (rset.next()) {
                final String col = rset.getString(PK_DESC_COLUMN_NAME);
                pkname = rset.getString(PK_DESC_PK_NAME);
                final int pkseq = rset.getInt(PK_DESC_KEY_SEQ);
                result.addColumn(col, pkname, pkseq);
            }
            rset.close();
        }
        return result;
    }

    private Map getForeignKeys(final DatabaseMetaData meta, final String tabName)
    throws SQLException {
        final Map fks = new HashMap();

        ResultSet rset = null;
        // some jdbc version 2 drivers (connector/j) have problems with foreign
        // keys...
        try {
            rset = meta.getImportedKeys(null, null, tabName);
        } catch (final NoSuchElementException e) {
            if (VERBOSE) {
                HenPlus.msg().println(
                        "Database problem reading meta data: " + e);
            }
        }

        if (rset != null) {
            while (rset.next()) {
                final ColumnFkInfo fk = new ColumnFkInfo(rset.getString(FK_FK_NAME),
                        rset.getString(FK_PKTABLE_NAME), rset
                        .getString(FK_PKCOLUMN_NAME));
                final String col = rset.getString(FK_FKCOLUMN_NAME);
                fks.put(col, fk);
            }
            rset.close();
        }
        return fks;
    }

}
