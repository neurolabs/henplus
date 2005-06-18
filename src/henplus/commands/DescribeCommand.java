/*
 * This is free software, licensed under the Gnu Public License (GPL) get a copy from
 * <http://www.gnu.org/licenses/gpl.html>
 *
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.AbstractCommand;
import henplus.CommandDispatcher;
import henplus.HenPlus;
import henplus.Interruptable;
import henplus.SQLSession;
import henplus.SigIntHandler;
import henplus.util.StringAppender;
import henplus.view.Column;
import henplus.view.ColumnMetaData;
import henplus.view.TableRenderer;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * document me.
 */
public class DescribeCommand extends AbstractCommand implements Interruptable
{
    private final static String[] LIST_TABLES = { "TABLE" };
    private final static ColumnMetaData[] DESC_META;
    static {
        DESC_META    = new ColumnMetaData[9];
        DESC_META[0] = new ColumnMetaData("#", ColumnMetaData.ALIGN_RIGHT);
        DESC_META[1] = new ColumnMetaData("table");
        DESC_META[2] = new ColumnMetaData("column");
        DESC_META[3] = new ColumnMetaData("type");
        DESC_META[4] = new ColumnMetaData("null");
        DESC_META[5] = new ColumnMetaData("default");
        DESC_META[6] = new ColumnMetaData("pk");
        DESC_META[7] = new ColumnMetaData("fk");
        DESC_META[8] = new ColumnMetaData("remark", ColumnMetaData.ALIGN_LEFT, 60);
    }

    private boolean interrupted;
    private boolean verbose;
    private final ListUserObjectsCommand tableCompleter;

    public DescribeCommand(ListUserObjectsCommand tc){
        tableCompleter = tc;
    }

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "describe", "idescribe" };
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmd, String param) {
        // make use of properties for these properties?
        // (since the options just toggle, this may be convenient)
        boolean showDescriptions = true;
        boolean showIndex = "idescribe".equals(cmd);
        boolean showTime = true;

        final StringTokenizer st = new StringTokenizer(param);
        if (st.countTokens() < 1) return SYNTAX_ERROR;

        // this was a flag to ensure that all options come before the tablenames
        // can probably be removed...
        boolean more_options = true;
        while(st.hasMoreTokens()) {
            String tabName = st.nextToken();
            if (more_options && tabName.startsWith("-")) {
                if (tabName.indexOf('i')>-1) showIndex =  !showIndex;
                if (tabName.indexOf('v')>-1) showDescriptions = !showDescriptions;
                if (tabName.indexOf('t')>-1) showTime = !showTime;
            }
            else {
                // more_options = false; // options can stand at every position --> toggle
                boolean correctName = true;

                if (tabName.startsWith("\"")) {
                    tabName = stripQuotes(tabName);
                    correctName = false;
                }

                // separate schama and table.
                String schema = null;
                int schemaDelim = tabName.indexOf('.');
                if (schemaDelim > 0) {
                    schema = tabName.substring(0, schemaDelim);
                    tabName = tabName.substring(schemaDelim + 1);
                }

                // FIXME: provide correct name as well for schema!
                if (correctName) {
                    String alternative = tableCompleter.correctTableName(tabName);
                    if (alternative != null && !alternative.equals(tabName)) {
                        tabName = alternative;
                        HenPlus.out().println("describing table: '" + tabName + "' (corrected name)");
                    }
                }

                ResultSet rset = null;
                Set doubleCheck = new HashSet();
                try {
                    interrupted = false;
                    SigIntHandler.getInstance().pushInterruptable(this);
                    boolean anyLeftArrow = false;
                    boolean anyRightArrow = false;
                    long startTime = System.currentTimeMillis();
                    String catalog = session.getConnection().getCatalog();
                    String description = null;

                    if (interrupted) return SUCCESS;

                    DatabaseMetaData meta = session.getConnection().getMetaData();
                    for (int i = 0; i < DESC_META.length; ++i) {
                        DESC_META[i].resetWidth();
                    }

                    rset = meta.getTables(catalog, schema, tabName, LIST_TABLES);
                    if (rset != null && rset.next()) {
                        description = rset.getString(5); // remark
                    }
                    rset.close();

                    /*
                     * get primary keys.
                     */
                    if (interrupted) return SUCCESS;
                    Map pks = new HashMap();
                    rset = meta.getPrimaryKeys(null, schema, tabName);
                    if (rset != null) while (!interrupted && rset.next()) {
                        String col = rset.getString(4);
                        int pkseq = rset.getInt(5);
                        String pkname = rset.getString(6);
                        String desc = (pkname != null) ? pkname : "*";
                        if (pkseq > 1) {
                            desc = StringAppender.getInstance().append(desc).append("{").append(pkseq).append("}").toString();
                            // desc += "{" + pkseq + "}";
                        }
                        pks.put(col, desc);
                    }
                    rset.close();

                    /*
                     * get referenced primary keys.
                     */
                    if (interrupted) return SUCCESS;
                    rset = meta.getExportedKeys(null, schema, tabName);
                    if (rset != null)
                            while (!interrupted && rset.next()) {
                                String col = rset.getString(4);
                                String fktable = rset.getString(7);
                                String fkcolumn = rset.getString(8);
                                fktable = StringAppender.getInstance().append(fktable).append("(").append(fkcolumn).append(")")
                                        .toString();
                                String desc = (String) pks.get(col);
                                desc = (desc == null) ? StringAppender.start(" <- ").append(fktable).toString()
                                        : StringAppender.start(desc).append("\n <- ").append(fktable).toString();
                                anyLeftArrow = true;
                                pks.put(col, desc);
                            }
                    rset.close();

                    /*
                     * get foreign keys.
                     */
                    if (interrupted) return SUCCESS;
                    Map fks = new HashMap();

                    // some jdbc version 2 drivers (connector/j) have problems with foreign keys...
                    try {
                        rset = meta.getImportedKeys(null, schema, tabName);
                    }
                    catch (NoSuchElementException e) {
                        if (verbose) {
                            HenPlus.msg().println("Database problem reading meta data: " + e);
                        }
                    }
                    if (rset != null) {
                        while (!interrupted && rset.next()) {
                            String table = rset.getString(3);
                            String pkcolumn = rset.getString(4);
                            table = table + "(" + pkcolumn + ")";
                            String col = rset.getString(8);
                            String fkname = rset.getString(12);
                            String desc = (fkname != null) ? StringAppender.start(fkname).append("\n -> ").toString() : " -> ";
                            desc += table;
                            anyRightArrow = true;
                            fks.put(col, desc);
                        }
                    }
                    rset.close();
                    
                    HenPlus.out().println("Table: " + tabName);
                    if (description != null) {
                        HenPlus.out().println(description);
                    }

                    if (catalog != null) {
                        HenPlus.msg().println("catalog: " + catalog);
                    }
                    if (anyLeftArrow) {
                        HenPlus.msg().println(" '<-' : referenced by");
                    }
                    if (anyRightArrow) {
                        HenPlus.msg().println(" '->' : referencing");
                    }

                    /*
                     * if all columns belong to the same table name, then
                     * don't report it. A different table
                     * name may only occur in rare circumstance like object
                     * oriented databases.
                     */
                    boolean allSameTableName = true;

                    /*
                    * build up actual describe table.
                    */
                    if (interrupted) return SUCCESS;

                    rset = meta.getColumns(catalog, schema, tabName, null);
                    List rows = new ArrayList();
                    int colNum = 0;
                    boolean anyDescription = false;
                    if (rset != null) {
                        while (!interrupted && rset.next()) {
                            final Column[] row = new Column[9];
                            row[0] = new Column(++colNum);
                            final String thisTabName = rset.getString(3);
                            row[1] = new Column(thisTabName);
                            allSameTableName &= tabName.equals(thisTabName);
                            final String colname = rset.getString(4);
                            if (doubleCheck.contains(colname)) {
                                continue;
                            }
                            doubleCheck.add(colname);
                            row[2] = new Column(colname);
                            String type = rset.getString(6);
                            final int colSize = rset.getInt(7);
                            if (colSize > 0) {
                                type = StringAppender.start(type).append("(").append(colSize).append(")").toString();
                            }

                            row[3] = new Column(type);
                            final String defaultVal = rset.getString(13);
                            row[4] = new Column(rset.getString(18));
                            // oracle appends newline to default values for some reason.
                            row[5] = new Column(((defaultVal != null) ? defaultVal.trim() : null));
                            final String pkdesc = (String) pks.get(colname);
                            row[6] = new Column((pkdesc != null) ? pkdesc : "");
                            final String fkdesc = (String) fks.get(colname);
                            row[7] = new Column((fkdesc != null) ? fkdesc : "");

                            final String colDesc = (showDescriptions) ? rset.getString(12) : null;
                            row[8] = new Column(colDesc);
                            anyDescription |= (colDesc != null);
                            rows.add(row);
                        }
                    }
                    rset.close();

                    /*
                     * we render the table now, since we only know now,
                     * whether we will show the first
                     * column and the description column or not.
                     */
                    DESC_META[1].setDisplay(!allSameTableName);
                    DESC_META[8].setDisplay(anyDescription);
                    TableRenderer table = new TableRenderer(DESC_META, HenPlus.out());
                    Iterator it = rows.iterator();
                    while (it.hasNext()) {
                        table.addRow((Column[]) it.next());
                    }
                    table.closeTable();

                    if (interrupted) return SUCCESS;

                    if (showIndex) {
                        showIndexInformation(tabName, schema, meta);
                    }

                    if (showTime) {
                        TimeRenderer.printTime(System.currentTimeMillis() - startTime, HenPlus.out());
                        HenPlus.out().println();
                    }

                }
                catch (Exception e) {
                    if (verbose) e.printStackTrace();
                    String ex = (e.getMessage() != null) ? e.getMessage().trim() : e.toString();
                    HenPlus.msg().println("Database problem reading meta data: " + ex);
                    return EXEC_FAILED;
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

            }
        }
        return SUCCESS;
    }

    /**
     * @param tabName
     * @param schema
     * @param meta
     * @return @throws SQLException
     */
    private void showIndexInformation(String tabName, String schema, DatabaseMetaData meta) 
        throws SQLException
    {
        ResultSet rset;
        HenPlus.out().println("index information:");
        boolean anyIndex = false;
        rset = meta.getIndexInfo(null, schema, tabName, false, true);
        if (rset != null) while (!interrupted && rset.next()) {
            boolean nonUnique;
            String idxName = null;
            nonUnique = rset.getBoolean(4);
            idxName = rset.getString(6);
            if (idxName == null) continue; // statistics, otherwise.
            // output part.
                anyIndex = true;
                HenPlus.out().print("\t");
                if (!nonUnique) HenPlus.out().print("unique ");
                HenPlus.out().print("index " + idxName);
                String colName = rset.getString(9);
                // work around postgres-JDBC-driver bug:
                if (colName != null && colName.length() > 0) {
                    HenPlus.out().print(" on " + colName);
                }
                HenPlus.out().println();
            }
        rset.close();
        if (!anyIndex) {
            HenPlus.out().println("\t<none>");
        }
    }

    /**
     * complete the table name.
     */
    public Iterator complete(CommandDispatcher disp, String partialCommand, String lastWord)
    {
        StringTokenizer st = new StringTokenizer(partialCommand);
        st.nextElement(); // consume first element.
        if (lastWord.startsWith("\"")) {
            lastWord = lastWord.substring(1);
        }
        return tableCompleter.completeTableName(HenPlus.getInstance().getCurrentSession(), lastWord);
    }

    private String stripQuotes(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    //-- Interruptable interface
    public synchronized void interrupt() {
        interrupted = true;
    }

    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
        return "describe a database object";
    }

    public String getSynopsis(String cmd) {
        return cmd + " [options] <tablenames>";
    }

    public String getLongDescription(String cmd) {
        String dsc;
        dsc =     "\tDescribe the meta information of the named user object\n"
                + "\t(only tables for now). The name you type is case sensitive\n"
                + "\tbut henplus tries its best to correct it.\n"
                + "\tThe 'describe' command just describes the table, the\n"
                + "\t'idescribe' command determines the index information as\n"
                + "\twell; some databases are really slow in this, so this is\n"
                + "\tan extra command"
                // TODO: add getOptions() to Command-Interface?
                + "\n\n\tRecognized options are:\n"
                + "\t -i show index information (same as idescribe)\n"
                + "\t -v show column descriptions"
                + "\n\n\tIf an option is positioned between two tablenames, its current state is toggled."
                + "\n"
                ;
        return dsc;
    }

}

/* Emacs:
 * Local variables:
 * c-basic-offset: 4
 * tab-width: 8
 * indent-tabs-mode: nil
 * compile-command: "ant -emacs -find build.xml"
 * End:
 * vi:set tabstop=8 shiftwidth=4 nowrap:
 */
