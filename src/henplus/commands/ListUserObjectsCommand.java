/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.AbstractCommand;
import henplus.HenPlus;
import henplus.Interruptable;
import henplus.SQLSession;
import henplus.SigIntHandler;
import henplus.view.util.NameCompleter;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * FIXME: use SQLMetaData stuff instead.
 */
public class ListUserObjectsCommand extends AbstractCommand implements
Interruptable {
    final private static String[] LIST_TABLES_VIEWS = { "TABLE", "VIEW" };
    final private static String[] LIST_TABLES = { "TABLE" };
    final private static String[] LIST_VIEWS = { "VIEW" };
    final private static int[] TABLE_DISP_COLS = { 2, 3, 4, 5 };
    final private static int[] PROC_DISP_COLS = { 2, 3, 8 };

    /**
     * all tables in one session.
     */
    final private Map<SQLSession,NameCompleter> sessionTables;
    final private Map<SQLSession,NameCompleter> sessionColumns;
    final private HenPlus _henplus;

    private boolean _interrupted;

    public ListUserObjectsCommand(final HenPlus hp) {
        sessionTables = new HashMap<SQLSession,NameCompleter>();
        sessionColumns = new HashMap<SQLSession,NameCompleter>();
        _henplus = hp;
        _interrupted = false;
    }

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "tables", "views", "procedures", "rehash" };
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession session, final String cmd, final String param) {
        if (cmd.equals("rehash")) {
            rehash(session);
        } else {
            try {
                final Connection conn = session.getConnection(); // use createStmt
                final DatabaseMetaData meta = conn.getMetaData();
                final String catalog = conn.getCatalog();
                /*
                 * HenPlus.msg().println("catalog: " + catalog);
                 * ResultSetRenderer catalogrenderer = new
                 * ResultSetRenderer(meta.getSchemas(), "|", true, true, 2000,
                 * HenPlus.out()); catalogrenderer.execute();
                 */
                ResultSetRenderer renderer;
                ResultSet rset;
                String objectType;
                int[] columnDef;
                if ("procedures".equals(cmd)) {
                    objectType = "Procecdures";
                    HenPlus.msg().println(objectType);
                    rset = meta.getProcedures(catalog, null, null);
                    columnDef = PROC_DISP_COLS;
                } else {
                    final boolean showViews = "views".equals(cmd);
                    objectType = showViews ? "Views" : "Tables";
                    HenPlus.msg().println(objectType);
                    rset = meta.getTables(catalog, null, null,
                            showViews ? LIST_VIEWS : LIST_TABLES);
                    columnDef = TABLE_DISP_COLS;
                }

                renderer = new ResultSetRenderer(rset, "|", true, true, 10000,
                        HenPlus.out(), columnDef);
                renderer.getDisplayMetaData()[2].setAutoWrap(78);

                final int tables = renderer.execute();
                if (tables > 0) {
                    HenPlus.msg()
                    .println(tables + " " + objectType + " found.");
                    if (renderer.limitReached()) {
                        HenPlus.msg().println(
                        "..and probably more; reached display limit");
                    }
                }
            } catch (final Exception e) {
                HenPlus.msg().println(e.getMessage());
                return EXEC_FAILED;
            }
        }
        return SUCCESS;
    }

    private NameCompleter getTableCompleter(final SQLSession session) {
        final NameCompleter compl = (NameCompleter) sessionTables.get(session);
        return compl == null ? rehash(session) : compl;
    }

    private NameCompleter getAllColumnsCompleter(final SQLSession session) {
        NameCompleter compl = (NameCompleter) sessionColumns.get(session);
        if (compl != null) {
            return compl;
        }
        /*
         * This may be a lengthy process..
         */
        _interrupted = false;
        SigIntHandler.getInstance().pushInterruptable(this);
        final NameCompleter tables = getTableCompleter(session);
        if (tables == null) {
            return null;
        }
        final Iterator table = tables.getAllNamesIterator();
        compl = new NameCompleter();
        while (!_interrupted && table.hasNext()) {
            final String tabName = (String) table.next();
            final Collection columns = columnsFor(tabName);
            final Iterator cit = columns.iterator();
            while (cit.hasNext()) {
                final String col = (String) cit.next();
                compl.addName(col);
            }
        }
        if (_interrupted) {
            compl = null;
        } else {
            sessionColumns.put(session, compl);
        }
        SigIntHandler.getInstance().popInterruptable();
        return compl;
    }

    public void unhash(final SQLSession session) {
        sessionTables.remove(session);
    }

    /**
     * rehash table names.
     */
    private NameCompleter rehash(final SQLSession session) {
        final NameCompleter result = new NameCompleter();
        final Connection conn = session.getConnection(); // use createStmt
        ResultSet rset = null;
        try {
            final DatabaseMetaData meta = conn.getMetaData();
            rset = meta.getTables(null, null, null, LIST_TABLES_VIEWS);
            while (rset.next()) {
                result.addName(rset.getString(3));
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
        sessionTables.put(session, result);
        sessionColumns.remove(session);
        return result;
    }

    /**
     * fixme: add this to the cached values determined by rehash.
     */
    public Collection columnsFor(String tabName) {
        final SQLSession session = _henplus.getCurrentSession();
        final Set result = new HashSet();
        final Connection conn = session.getConnection(); // use createStmt
        ResultSet rset = null;

        String schema = null;
        final int schemaDelim = tabName.indexOf('.');
        if (schemaDelim > 0) {
            schema = tabName.substring(0, schemaDelim);
            tabName = tabName.substring(schemaDelim + 1);
        }
        try {
            final DatabaseMetaData meta = conn.getMetaData();
            rset = meta.getColumns(conn.getCatalog(), schema, tabName, null);
            while (rset.next()) {
                result.add(rset.getString(4));
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
        return result;
    }

    /**
     * see, if we find exactly one alternative, that is spelled correctly. If we
     * have more than one alternative but one, that has the same length of the
     * requested tablename, return this.
     */
    public String correctTableName(final String tabName) {
        final Iterator it = completeTableName(HenPlus.getInstance()
                .getCurrentSession(), tabName);
        if (it == null) {
            return null;
        }
        boolean foundSameLengthMatch = false;
        int count = 0;
        String correctedName = null;
        if (it.hasNext()) {
            final String alternative = (String) it.next();
            final boolean sameLength = alternative != null && alternative.length() == tabName
                    .length();

            foundSameLengthMatch |= sameLength;
            ++count;
            if (sameLength) {
                correctedName = alternative;
            }
        }
        return count == 1 || foundSameLengthMatch ? correctedName : null;
    }

    /**
     * used from diverse commands that need table name completion.
     */
    public Iterator completeTableName(final SQLSession session, final String partialTable) {
        if (session == null) {
            return null;
        }
        final NameCompleter completer = getTableCompleter(session);
        return completer.getAlternatives(partialTable);
    }

    public Iterator completeAllColumns(final String partialColumn) {
        final SQLSession session = _henplus.getCurrentSession();
        if (session == null) {
            return null;
        }
        final NameCompleter completer = getAllColumnsCompleter(session);
        return completer.getAlternatives(partialColumn);
    }

    public Iterator<String> getTableNamesIteratorForSession(final SQLSession session) {
        return getTableCompleter(session).getAllNamesIterator();
    }

    public SortedSet<String> getTableNamesForSession(final SQLSession session) {
        return getTableCompleter(session).getAllNames();
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "list available user objects";
    }

    @Override
    public String getSynopsis(final String cmd) {
        return cmd;
    }

    @Override
    public String getLongDescription(final String cmd) {
        String dsc;
        if (cmd.equals("rehash")) {
            dsc = "\trebuild the internal hash for tablename completion.";
        } else {
            dsc = "\tLists all " + cmd + " available in this schema.";
        }
        return dsc;
    }

    public void interrupt() {
        _interrupted = true;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
