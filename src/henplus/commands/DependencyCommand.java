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
import java.util.TreeSet;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import henplus.util.*;
import henplus.SQLSession;
import henplus.AbstractCommand;
import henplus.CommandDispatcher;

/**
 * creates a dependency graph.
 */
public class DependencyCommand extends AbstractCommand {
    static final boolean verbose     = false;
    private final static ColumnMetaData[] DESC_META;
    static {
	DESC_META = new ColumnMetaData[3];
	DESC_META[0] = new ColumnMetaData("depth");
	DESC_META[1] = new ColumnMetaData("table ..");
	DESC_META[2] = new ColumnMetaData(".. depends on");
    }
    
    private final ListUserObjectsCommand tableCompleter;

    private final static class Entity implements Comparable {
        private final Set dependsOn;
        private final String name;
        private int depth;
        private boolean recurseSentinel;

        public Entity(String name) {
            depth = 0;
            recurseSentinel = false;
            dependsOn = new HashSet();
            this.name = name;
        }
        
        public void addDependsOn(Entity e) {
            dependsOn.add(e);
        }

        private int updateDepth(int d) {
            if (d > depth) {
                depth = d;
            }
            return depth;
        }
        
        public int dependencyCount() {
            return dependsOn.size();
        }
        
        public String dependencyList() {
            StringBuffer buf = new StringBuffer();
            Iterator it = dependsOn.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                buf.append("(").append(e.getDepth()).append(") ");
                buf.append(e.getName());
                buf.append("\n");
                if (!it.hasNext()) {
                    buf.append(" ");
                }
            }
            return buf.toString();
        }

        public int compareTo(Object o) {
            Entity other = (Entity) o;
            int diff = (depth - other.depth);
            if (diff == 0) {
                // more dependencies before less.
                diff = (dependsOn.size() - other.dependsOn.size());
                if (diff != 0) {
                    return diff;
                }
                diff = other.name.compareTo(name);
            }
            return diff;
        }

        public int getDepth() {
            return depth;
        }
        
        public String getName() {
            return name;
        }

        public int calcDepth() {
            if (recurseSentinel) {
                return updateDepth(1); // oops, cyclic dependency.
            }
            recurseSentinel = true;
            Iterator it = dependsOn.iterator();
            while (it.hasNext()) {
                Entity e = (Entity) it.next();
                updateDepth(e.calcDepth() + 1);
            }
            recurseSentinel = false;
            return depth;
        }
    }

    public DependencyCommand(ListUserObjectsCommand tc) {
	tableCompleter = tc;
    }

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] { "dependency" }; 
    }

    /**
     * add new elements to the newElements. We cannot add this to the
     * global map (that is the source in the first step), since we would
     * otherwise modify our own iterator ..
     */
    private boolean determineForeignKeys(SQLSession session,
                                         Map globalMap,
                                         Map source, Map newElements) 
    {
        boolean anyNew = false;
        ResultSet rset = null;
        try {
            DatabaseMetaData meta = session.getConnection().getMetaData();
            Iterator it = source.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String tabName = (String) entry.getKey();
                Entity entity = (Entity) entry.getValue();

                /*
                 * get foreign keys.
                 */
                rset = meta.getImportedKeys(null, null, tabName);
                while (rset.next()) {
                    String table = rset.getString(3);
                    Entity dep  = null;
                    if (anyNew && newElements.containsKey(table)) {
                        dep = (Entity) newElements.get(table);
                    }
                    else if (globalMap.containsKey(table)) {
                        dep = (Entity) globalMap.get(table);
                    }
                    else {
                        dep = new Entity(table);
                        anyNew = true;
                        newElements.put(table, dep);
                    }
                    entity.addDependsOn(dep);
                }
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
        finally {
            if (rset != null) {
                try { rset.close(); } catch (Exception e) {}
            }
        }
        return anyNew;
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmd, String param) {
	final StringTokenizer st = new StringTokenizer(param);
	final int argc = st.countTokens();
	if (argc < 1) {
	    return SYNTAX_ERROR;
	}

        Map/*<String,Entity>*/ globalEntityMap = new HashMap();
        
        while (st.hasMoreElements()) {
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
            globalEntityMap.put(tabName, new Entity(tabName));
        }
        
        Map source = globalEntityMap;
        Map newElements = new HashMap();
        /*
         * ripple through, until we haven't found anything, that
         * depends on this list..
         */
        while (determineForeignKeys(session, globalEntityMap,
                                    source, newElements)) {
            globalEntityMap.putAll(newElements);
            source = newElements;
            newElements = new HashMap();
        }

        Iterator it;
        it = globalEntityMap.values().iterator();
        while (it.hasNext()) {
            Entity e = (Entity) it.next();
            e.calcDepth();
        }

        /*
         * write out depth.
         */
        for (int i=0; i < DESC_META.length; ++i) {
            DESC_META[i].reset();
        }        
        TableRenderer table = new TableRenderer(DESC_META, System.out);
        Set sortSet = new TreeSet();
        sortSet.addAll(globalEntityMap.values());

        it = sortSet.iterator();
        while (it.hasNext()) {
            Entity e = (Entity) it.next();            
            Column[] row = new Column[3];
            row[0] = new Column(String.valueOf(e.getDepth()));
            row[1] = new Column(e.getName());
            row[2] = new Column(e.dependencyList());
            table.addRow(row);
        }
        table.closeTable();
        
        return SUCCESS;
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
	return "get the dependencies of the given tables";
    }
    
    public String getSynopsis(String cmd) {
	return "dependency <tablename> [<tablename> ..]";
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
