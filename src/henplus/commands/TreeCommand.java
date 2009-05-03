/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import henplus.OutputDevice;

/*     fop --+
 * abc--+    |
 * xyz--|    |
 *    blub --|
 *           foo
 *             |-- bar
 *             |    |-- blah
 *             |    `-- (foo)            <-- cylic reference
 *             `-- baz
 */

/**
 * creates a dependency graph.
 */
public class TreeCommand extends AbstractCommand implements Interruptable {
    private static final int IMP_PRIMARY_KEY_TABLE = 3;
    /** reference in exported/imported key */
    private static final int EXP_FOREIGN_KEY_TABLE = 7;

    static final boolean verbose = HenPlus.VERBOSE;

    private final ListUserObjectsCommand tableCompleter;
    private volatile boolean _interrupted;

    /**
     * A node in a cyclic graph.
     */
    private static abstract class Node implements Comparable {
        private final Set/* <Node> */_children;
        private int _displayDepth;

        protected Node() {
            _children = new TreeSet();
            _displayDepth = -1;
        }

        public Node add(final Node n) {
            _children.add(n);
            return n;
        }

        boolean markDepth(final int target, final int current) {
            if (target == current) {
                if (_displayDepth < 0) {
                    _displayDepth = current;
                    return true;
                }
                return false;
            }
            boolean anyChange = false;
            final Iterator it = _children.iterator();
            while (it.hasNext()) {
                final Node n = (Node) it.next();
                anyChange |= n.markDepth(target, current + 1);
            }
            return anyChange;
        }

        public void markDepths() {
            _displayDepth = 0;
            for (int depth = 1; markDepth(depth, 0); ++depth) {
                // noop
            }
        }

        public void print(final OutputDevice out, final int indentCount) {
            final StringBuffer indent = new StringBuffer();
            for (int i = 0; i < indentCount; ++i) {
                indent.append(" ");
            }
            print(0, new TreeSet(), new StringBuffer(), indent.toString(), out);
        }

        private void print(final int depth, final SortedSet alreadyPrinted,
                final StringBuffer currentIndent, final String indentString,
                final OutputDevice out) {
            final String name = getName();
            if (depth != 0) {
                out.print("-- ");
                final boolean cyclic = depth != _displayDepth
                || alreadyPrinted.contains(name);
                if (cyclic) {
                    out.print("(" + name + ")");
                } else {
                    out.print(name);
                }
                out.println();
                if (cyclic) {
                    return;
                }
            }
            alreadyPrinted.add(name);

            int remaining = _children.size();
            if (remaining > 0) {
                final int previousLength = currentIndent.length();
                currentIndent.append(indentString);
                final Iterator it = _children.iterator();
                while (it.hasNext()) {
                    final Node n = (Node) it.next();
                    out.print(String.valueOf(currentIndent));
                    out.print(remaining == 1 ? "`" : "|");
                    n.print(depth + 1, alreadyPrinted, currentIndent,
                            remaining == 1 ? "    " : "|   ", out);
                    --remaining;
                }
                currentIndent.setLength(previousLength);
            }
        }

        public int printReverse(final OutputDevice out) {
            final List result = new ArrayList();
            printReverse(0, new TreeSet(), result, "", false);
            Iterator it = result.iterator();
            int maxLen = 0;
            while (it.hasNext()) {
                final String line = (String) it.next();
                if (line.length() > maxLen) {
                    maxLen = line.length();
                }
            }
            it = result.iterator();
            while (it.hasNext()) {
                final String line = (String) it.next();
                final int len = line.length();
                for (int i = len; i < maxLen; ++i) {
                    out.print(" ");
                }
                out.println(line);
            }
            return maxLen;
        }

        private int printReverse(final int depth, final SortedSet alreadyPrinted,
                final List output, final String indentString, final boolean isLast) {
            final String name = getName();
            final boolean cyclic = depth != _displayDepth
            || alreadyPrinted.contains(name);
            final String printName = cyclic ? "(" + name + ")--" : name + "--";
            final int myIndent = indentString.length() + printName.length();
            int maxIndent = myIndent;

            if (!cyclic) {
                alreadyPrinted.add(name);
                int remaining = _children.size();
                if (remaining > 0) {
                    final Iterator it = _children.iterator();
                    boolean isFirst = true;
                    while (it.hasNext()) {
                        final Node n = (Node) it.next();
                        int nIndent;
                        nIndent = n.printReverse(depth + 1, alreadyPrinted,
                                output, depth == 0 ? "" : indentString
                                        + "    |", isFirst);
                        if (nIndent > maxIndent) {
                            maxIndent = nIndent;
                        }
                        --remaining;
                        isFirst = false;
                    }
                }
            }

            if (depth != 0) {
                final String outputString = printName + (isLast ? "\\" : "|")
                + indentString;
                output.add(outputString);
            }
            return maxIndent;
        }

        public int compareTo(final Object o) {
            final Node other = (Node) o;
            return getName().compareTo(other.getName());
        }

        /**
         * This is what we need to print the stuff..
         */
        public abstract String getName();
    }

    /**
     * the entity is simply represented as String.
     */
    private static class StringNode extends Node {
        private final String _name;

        public StringNode(final String s) {
            _name = s;
        }

        @Override
        public String getName() {
            return _name;
        }
    }

    public TreeCommand(final ListUserObjectsCommand tc) {
        tableCompleter = tc;
    }

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "tree-view" };
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession session, final String cmd, final String param) {
        final StringTokenizer st = new StringTokenizer(param);
        final int argc = st.countTokens();
        if (argc != 1) {
            return SYNTAX_ERROR;
        }

        boolean correctName = true;
        String tabName = (String) st.nextElement();
        if (tabName.startsWith("\"")) {
            tabName = stripQuotes(tabName);
            correctName = false;
        }
        if (correctName) {
            final String alternative = tableCompleter.correctTableName(tabName);
            if (alternative != null && !alternative.equals(tabName)) {
                tabName = alternative;
            }
        }
        final String schema = null; // fixme: determine

        try {
            final long startTime = System.currentTimeMillis();
            final DatabaseMetaData dbMeta = session.getConnection()
            .getMetaData();

            _interrupted = false;
            SigIntHandler.getInstance().pushInterruptable(this);

            // build a tree of all tables I depend on..
            final Node myParents = buildTree(new ReferenceMetaDataSource() {
                public ResultSet getReferenceMetaData(final String schema,
                        final String table) throws SQLException {
                    return dbMeta.getImportedKeys(null, schema, table);
                }
            }, IMP_PRIMARY_KEY_TABLE, new TreeMap(), schema, tabName);
            if (_interrupted) {
                return SUCCESS;
            }
            myParents.markDepths();

            // build a tree of all tables that depend on me ...
            final Node myChilds = buildTree(new ReferenceMetaDataSource() {
                public ResultSet getReferenceMetaData(final String schema,
                        final String table) throws SQLException {
                    return dbMeta.getExportedKeys(null, schema, table);
                }
            }, EXP_FOREIGN_KEY_TABLE, new TreeMap(), schema, tabName);
            if (_interrupted) {
                return SUCCESS;
            }
            myChilds.markDepths();

            final int reversIndent = myParents.printReverse(HenPlus.out());

            final int tabLen = tabName.length();
            int startPos = reversIndent - tabLen / 2;
            if (startPos < 0) {
                startPos = 0;
            }
            for (int i = 0; i < startPos; ++i) {
                HenPlus.out().print(" ");
            }
            HenPlus.out().attributeBold();
            HenPlus.out().println(tabName);
            HenPlus.out().attributeReset();

            myChilds.print(HenPlus.out(), startPos + tabLen / 2);

            TimeRenderer.printTime(System.currentTimeMillis() - startTime,
                    HenPlus.msg());
            HenPlus.msg().println();
        } catch (final Exception e) {
            HenPlus.msg().println(
                    "problem getting database meta data: " + e.getMessage());
            return EXEC_FAILED;
        }
        return SUCCESS;
    }

    private interface ReferenceMetaDataSource {
        ResultSet getReferenceMetaData(String schema, String table)
        throws SQLException;
    }

    /**
     * build a subtree from the MetaData for the table with the given name. If
     * this node already exists (because of a cyclic dependency), return that.
     * recursively called to build the whole tree. This determines its refernece
     * data from the ReferenceMetaDataSource 'lambda' that either wraps
     * getImportedKeys() or getExportedKeys(). The 'sourceColumn' defines the
     * column in which the appropriate table name is.
     */
    private Node buildTree(final ReferenceMetaDataSource source, final int sourceColumn,
            final Map knownNodes, final String schema, final String tabName) throws SQLException {
        if (knownNodes.containsKey(tabName)) {
            return (Node) knownNodes.get(tabName);
        }

        final Node n = new StringNode(tabName);
        knownNodes.put(tabName, n);
        ResultSet rset = null;
        try {
            rset = source.getReferenceMetaData(schema, tabName);
            // read this into a list to avoid recursive calls to MetaData
            // which some JDBC-drivers don't like..
            final List refTables = new ArrayList();
            while (rset.next()) {
                final String referencingTable = rset.getString(sourceColumn);
                refTables.add(referencingTable);
            }

            final Iterator it = refTables.iterator();
            while (it.hasNext()) {
                final String referencingTable = (String) it.next();
                if (_interrupted) {
                    return null;
                }
                n.add(buildTree(source, sourceColumn, knownNodes, schema,
                        referencingTable));
            }
        } finally {
            if (rset != null) {
                try {
                    rset.close();
                } catch (final Exception e) {
                }
            }
        }
        return n;
    }

    /**
     * complete the table name.
     */
    @Override
    public Iterator complete(final CommandDispatcher disp, final String partialCommand,
            String lastWord) {
        final StringTokenizer st = new StringTokenizer(partialCommand);
        st.nextElement(); // skip cmd.
        // we accept only one argument.
        if (lastWord.startsWith("\"")) {
            lastWord = lastWord.substring(1);
        }
        return tableCompleter.completeTableName(HenPlus.getInstance()
                .getCurrentSession(), lastWord);
    }

    private String stripQuotes(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    /** interrupt interface */
    public void interrupt() {
        _interrupted = true;
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "tree representation of connected tables";
    }

    @Override
    public String getSynopsis(final String cmd) {
        return cmd + " <tablename>";
    }

    @Override
    public String getLongDescription(final String cmd) {
        String dsc = null;
        dsc = "\tShow tables, that are connected via foreign keys in a\n"
            + "\ttree like manner. This is very helpful in exploring\n"
            + "\tcomplicated data structures or simply check if all\n"
            + "\tforeign keys are applied. This command works of course\n"
            + "\tonly with databases that support foreign keys.\n"
            + "\tInvoke on the toplevel table you are interested in\n"
            + "\tExample:\n"
            + "\tConsider tables 'bar' and 'baz' that have a foreign key\n"
            + "\ton the table 'foo'. Further a table 'blah', that references\n"
            + "\t'bar'. The table 'foo' in turn references 'bar', thus\n"
            + "\tcyclicly referencing itself. Invoking tree-view on 'foo'\n"
            + "\twould be represented as\n"
            + "\t    foo\n"
            + "\t    |-- bar\n"
            + "\t    |   |-- blah\n"
            + "\t    |   `-- (foo)            <-- cylic/already printed reference\n"
            + "\t    `-- baz\n"
            + "\tSo in order to limit the potential cyclic graph in the\n"
            + "\ttree view from infinite to finite, cyclic nodes or nodes already\n"
            + "\tdisplayed unfolded are shown in parenthesis.";
        return dsc;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
