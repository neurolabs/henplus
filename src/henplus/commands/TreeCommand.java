/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.sql.*;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import henplus.util.*;
import henplus.SQLSession;
import henplus.AbstractCommand;
import henplus.CommandDispatcher;

/*
 * foo
 * |-- bar
 * |   |-- blah
 * |   `-- (foo)            <-- cylic reference
 * `-- baz
 */

/**
 * creates a dependency graph.
 */
public class TreeCommand extends AbstractCommand {
    static final boolean verbose     = false;
    private final ListUserObjectsCommand tableCompleter;

    /**
     * A node in a cyclic graph.
     */
    private static abstract class Node implements Comparable {
        private final Set/*<Node>*/ _nodeSet;
        protected Node() {
            _nodeSet = new TreeSet();
        }
        
        public Node add(Node n) {
            _nodeSet.add(n);
            return n;
        }

        public void print() {
            print(new TreeSet(), new StringBuffer(), "");
        }

        private void print(SortedSet alreadyPrinted,
                           StringBuffer currentIndent,
                           String indentString) 
        {
            if (indentString.length() > 0) { // otherwise we are toplevel.
                System.out.print("-- ");
            }
            String name = getName();
            boolean cyclic = alreadyPrinted.contains(name);
            //Terminal.blue(System.out);
            if (cyclic) System.out.print("(");
            System.out.print(name);
            if (cyclic) System.out.print(")");
            //Terminal.reset(System.out);
            System.out.println();
            if (cyclic) {
                return;
            }
            alreadyPrinted.add(name);
            int remaining = _nodeSet.size();
            if (remaining > 0) {
                int previousLength = currentIndent.length();
                currentIndent.append(indentString);
                Iterator it = _nodeSet.iterator();
                while (it.hasNext()) {
                    Node n = (Node) it.next();
                    System.out.print(currentIndent);
                    System.out.print((remaining == 1) ? '`' : '|');
                    n.print(alreadyPrinted, currentIndent,
                            (remaining == 1) ? "    " : "|   ");
                    --remaining;
                }
                currentIndent.setLength(previousLength);
            }
        }

        public int compareTo(Object o) {
            Node other = (Node) o;
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
        public StringNode(String s) { _name = s; }
        public String getName() { return _name; }
    }


    public TreeCommand(ListUserObjectsCommand tc) {
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
    public int execute(SQLSession session, String cmd, String param) {
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
            String alternative = tableCompleter.correctTableName(tabName);
            if (alternative != null && !alternative.equals(tabName)) {
                tabName = alternative;
            }
        }

        System.out.println();
        try {
            long startTime = System.currentTimeMillis();
            DatabaseMetaData dbMeta = session.getConnection().getMetaData();
            buildTree(dbMeta, new TreeMap(), tabName).print();
            TimeRenderer.printTime(System.currentTimeMillis()-startTime,
                                   System.err);
            System.err.println();
        }
        catch (Exception e) {
            System.err.println("problem getting database meta data: " 
                               + e.getMessage());
            return EXEC_FAILED;
        }
        return SUCCESS;
    }
    
    private Node buildTree(DatabaseMetaData meta,
                           Map knownNodes, String tabName) 
        throws SQLException
    {
        if (knownNodes.containsKey(tabName)) {
            return (Node) knownNodes.get(tabName);
        }
        
        Node n = new StringNode(tabName);
        knownNodes.put(tabName, n);
        ResultSet rset = null;
        try {
            rset = meta.getExportedKeys(null, null, tabName);
            while (rset.next()) {
                String referencingTable = rset.getString(7);
                n.add(buildTree(meta, knownNodes, referencingTable));
            }
        }
        finally {
            if (rset != null) {
                try { rset.close(); } catch (Exception e) {}
            }
        }
        return n;
    }

    /**
     * complete the table name.
     */
    public Iterator complete(CommandDispatcher disp,
			     String partialCommand, String lastWord) 
    {
	StringTokenizer st = new StringTokenizer(partialCommand);
	String cmd = (String) st.nextElement();
	int argc = st.countTokens();
	// we accept only one argument.
	if (lastWord.startsWith("\"")) {
	    lastWord = lastWord.substring(1);
	}
	return tableCompleter.completeTableName(lastWord);
    }

    private String stripQuotes(String value) {
	if (value.startsWith("\"") && value.endsWith("\"")) {
	    value = value.substring(1, value.length()-1);
	}
	return value;
    }
    
    /**
     * return a descriptive string.
     */
    public String  getShortDescription() { 
	return "tree representation of connected tables";
    }
    
    public String getSynopsis(String cmd) {
	return cmd + " <tablename>";
    }
    
    public String getLongDescription(String cmd) {
	String dsc = null;
        dsc= "\tShow tables, that are connected via foreign keys in a\n"
            +"\ttree like manner. This is very helpful in exploring\n"
            +"\tcomplicated data structures or simply check if all\n"
            +"\tforeign keys are applied. This command works of course\n"
            +"\tonly with databases that support foreign keys (so _not_\n"
            +"\tMySQL). Invoke on the toplevel table you are interested in\n"
            +"\tExample:\n"
            +"\tConsider tables 'bar' and 'baz' that have a foreign key\n"
            +"\ton the table 'foo'. Further a table 'blah', that references\n"
            +"\t'bar'. The table 'foo' in turn references 'bar', thus\n"
            +"\tcyclicly referencing itself. Invoking tree-view on 'foo'\n"
            +"\twould be represented as\n"
            +"\t    foo\n"
            +"\t    |-- bar\n"
            +"\t    |   |-- blah\n"
            +"\t    |   `-- (foo)            <-- cylic reference\n"
            +"\t    `-- baz\n"
            +"\tSo in order to limit the potential cyclic graph in the\n"
            +"\ttree view from infinite to finite, cyclic nodes are shown\n"
            +"\tin parenthesis.";
        return dsc;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
