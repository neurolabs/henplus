/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: SQLMetaDataBuilder.java,v 1.5 2004-09-22 11:49:31 magrokosmos Exp $ 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus;

import henplus.HenPlus;

import henplus.sqlmodel.Column;
import henplus.sqlmodel.ColumnFkInfo;
import henplus.sqlmodel.PrimaryKey;
import henplus.sqlmodel.Table;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

public final class SQLMetaDataBuilder {
    final private static String[] LIST_TABLES = { "TABLE" };
    private static final boolean _verbose = false;

    // column description
    public static final int TABLE_NAME  = 3; // String
    public static final int COLUMN_NAME = 4; // String
    public static final int DATA_TYPE   = 5; // int -> java.sql.Types
    public static final int TYPE_NAME   = 6; // String
    public static final int COLUMN_SIZE = 7; // int
    public static final int NULLABLE    = 11; // int: 
    public static final int COLUMN_DEF  = 13; // String
    public static final int ORDINAL_POSITION = 17; // int, starting at 1
    /*
     * columnNoNulls - might not allow NULL values
     * columnNullable - definitely allows NULL values
     * columnNullableUnknown - nullability unknown 
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

    /* (non-Javadoc)
     * @see henplus.Interruptable#interrupt()
     */
    public void interrupt() {
        _interrupted = true;
    }

    public SQLMetaData getMetaData(SQLSession session) {
	ResultSet rset = null;
        List tableList = new ArrayList();
	try {
	    DatabaseMetaData meta = session.getConnection().getMetaData();
	    rset = meta.getTables(null, null, null, LIST_TABLES);
	    while (rset.next()) {
		tableList.add(rset.getString(3));
	    }
	}
	catch (Exception e) {
	    // ignore.
	}
	finally {
	    if (rset != null) {
		try { rset.close(); } catch (Exception e) {}
	    }
	}
        return getMetaData(session, tableList);
    }
    
    public SQLMetaData getMetaData(SQLSession session, 
                                   Collection /*<String>*/ tableNames) {
        return getMetaData( session, tableNames.iterator() );
    }

    public SQLMetaData getMetaData(SQLSession session, 
                                   final Iterator /*<String>*/ tableNamesIter) {
        SQLMetaData result = new SQLMetaData();

        ResultSet rset = null;
        try {
            _interrupted = false;
            long startTime = System.currentTimeMillis();
            String catalog = session.getConnection().getCatalog();

            if (_interrupted)
                return null;

            DatabaseMetaData meta = session.getConnection().getMetaData();
            
            while (tableNamesIter.hasNext() && !_interrupted) {
                String tableName = (String)tableNamesIter.next();
                rset = meta.getColumns(catalog, null, tableName, null);
                Table table = buildTable(catalog, meta, tableName, rset);
                result.addTable(table);
            }
        }
        catch (Exception e) {
            if (_verbose)
                e.printStackTrace();
            HenPlus.msg().println(
                                  "Database problem reading meta data: " + e.getMessage().trim());
        }
        finally {
            if (rset != null) {
                try {
                    rset.close();
                }
                catch (Exception e) {
                }
            }
        }

        return result;
    }

    public Table getTable(SQLSession session, String tableName) {
        Table table = null;
        ResultSet rset = null;
        try {
            String catalog = session.getConnection().getCatalog();
            DatabaseMetaData meta = session.getConnection().getMetaData();
            rset = meta.getColumns(catalog, null, tableName, null);
            table = buildTable(catalog, meta, tableName, rset);
        }
        catch (Exception e) {
            if (_verbose)
                e.printStackTrace();
            HenPlus.msg().println(
                                  "Database problem reading meta data: " + e.getMessage().trim());
        }
        finally {
            if (rset != null) {
                try {
                    rset.close();
                }
                catch (Exception e) {
                }
            }
        }
        return table;
    }

    private Table buildTable(
                             String catalog,
                             DatabaseMetaData meta,
                             String tableName,
                             ResultSet rset)
        throws SQLException {

        Table table = null;
        if (rset != null) {
            table = new Table(tableName);
            PrimaryKey pk = getPrimaryKey(meta, tableName);
            Map fks = getForeignKeys(meta, tableName);
            // what about the following duplicate?
            // rset = meta.getColumns(catalog, null, tableName, null);
            while (!_interrupted && rset.next()) {
                String colname = rset.getString(COLUMN_NAME);
                Column column = new Column(colname);
                column.setType(rset.getString(TYPE_NAME));
                column.setSize(rset.getInt(COLUMN_SIZE));
                boolean nullable = (rset.getInt(NULLABLE) == DatabaseMetaData.columnNullable)
                    ? true
                    : false;
                column.setNullable(nullable);
                String defaultVal = rset.getString(COLUMN_DEF);
                column.setDefault( (defaultVal != null) ? defaultVal.trim() : null );
                column.setPosition(rset.getInt(ORDINAL_POSITION));
                column.setPkInfo(pk.getColumnPkInfo(colname));
                column.setFkInfo( (ColumnFkInfo)fks.get(colname) );

                table.addColumn(column);
            }
            rset.close();
        }
        return table;
    }

    private PrimaryKey getPrimaryKey(DatabaseMetaData meta, String tabName)
        throws SQLException {
        PrimaryKey result = null;
        ResultSet rset = meta.getPrimaryKeys(null, null, tabName);
        if (rset != null) {
            result = new PrimaryKey();
            String pkname = null;
            while (rset.next()) {
                String col = rset.getString(PK_DESC_COLUMN_NAME);
                pkname = rset.getString(PK_DESC_PK_NAME);
                int pkseq = rset.getInt(PK_DESC_KEY_SEQ);
                result.addColumn(col, pkname, pkseq);
            }
            rset.close();
        }
        return result;
    }

    private Map getForeignKeys(DatabaseMetaData meta, String tabName)
        throws SQLException {
        Map fks = new HashMap();
        
        ResultSet rset = null;
        // some jdbc version 2 drivers (connector/j) have problems with foreign keys...
        try {
            rset = meta.getImportedKeys(null, null, tabName);
        } catch ( NoSuchElementException e ) {
            if (_verbose)
                HenPlus.msg().println("Database problem reading meta data: " + e);
        }
        
        if (rset != null) {
            while (rset.next()) {
                ColumnFkInfo fk = new ColumnFkInfo(rset.getString(FK_FK_NAME),
                                                   rset.getString(FK_PKTABLE_NAME),
                                                   rset.getString(FK_PKCOLUMN_NAME));
                String col = rset.getString(FK_FKCOLUMN_NAME);
                fks.put(col, fk);
            }
            rset.close();
        }
        return fks;
    }

}
