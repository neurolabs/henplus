/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: TableDiffCommand.java,v 1.3 2004-01-28 09:25:49 hzeller Exp $ 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus.plugins.tablediff;

import henplus.Command;
import henplus.CommandDispatcher;
import henplus.HenPlus;
import henplus.Interruptable;
import henplus.SQLSession;
import henplus.SessionManager;
import henplus.commands.ListUserObjectsCommand;
import henplus.sqlmodel.Column;
import henplus.sqlmodel.Table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public final class TableDiffCommand implements Command, Interruptable {
    
    //private final ListUserObjectsCommand _tableCompleter;
    private static final boolean verbose     = true;
    
    protected static final String _command = "tablediff";
    protected static final String COMMAND_DELIMITER = ";";
    protected static final String OPTION_PREFIX = "-";
    protected static final String OPTION_CASESENSITIVE = "c";
    
    private boolean _interrupted = false;

    /**
     * 
     */
    public TableDiffCommand() {
    }
    
    public void interrupt() {
        this._interrupted = true;
    }

    /* (non-Javadoc)
     * @see henplus.Command#getCommandList()
     */
    public String[] getCommandList() {
        return new String[] { _command };
    }

    /* (non-Javadoc)
     * @see henplus.Command#participateInCommandCompletion()
     */
    public boolean participateInCommandCompletion() {
        return true;
    }

    /* (non-Javadoc)
     * @see henplus.Command#execute(henplus.SQLSession, java.lang.String, java.lang.String)
     */
    public int execute(SQLSession session, String command, String parameters) {
        // first set the option for case sensitive comparison of column names
        boolean colNameIgnoreCase = true;
        StringTokenizer st = new StringTokenizer(parameters);
        
        SessionManager sessionManager = HenPlus.getInstance().getSessionManager();
        
        if ( sessionManager.getSessionCount() < 2 ) {
            System.err.println("You need two valid sessions for this command.");
            return SYNTAX_ERROR;
        }
            
        SQLSession first = sessionManager.getSessionByName(st.nextToken());
        SQLSession second = sessionManager.getSessionByName(st.nextToken());
        
        if (first == null || second == null) {
            System.err.println("You need two valid sessions for this command.");
            return SYNTAX_ERROR;
        }
        else if (first == second) {
            System.err.println("You should specify two different sessions for this command.");
            return SYNTAX_ERROR;
        }
        else if (!st.hasMoreTokens()) {
            System.err.println("You should specify at least one table.");
            return SYNTAX_ERROR;
        }
        
        try {
            long start = System.currentTimeMillis();
            int count = 0;
            while (st.hasMoreTokens()) {
                String table = st.nextToken();
                Table ref = first.getTable(table);
                Table diff = second.getTable(table);
                TableDiffResult diffResult = TableDiffer.diffTables(ref, diff, colNameIgnoreCase);
                // TableDiffResult diffResult = getMockResult();
                if (diffResult == null) {
                    HenPlus.msg().println("no diff for table " + table);
                }
                else {
                    HenPlus.msg().println("diff result for table " + table + ":");
                    ResultTablePrinter.printResult(diffResult);
                }
                count++;
            }
            StringBuffer msg = new StringBuffer();
            msg.append("diffing ").append(count).append((count == 1)?" table took ":" tables took ").
            append(System.currentTimeMillis() - start).append(" ms.");
            HenPlus.msg().println(msg.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return SUCCESS;
    }
    
    private TableDiffResult getMockResult() {
        TableDiffResult result = new TableDiffResult();
        
        Column added = new Column("colname");
        added.setDefault("nix");
        added.setNullable(true);
        added.setPosition(23);
        added.setSize(666);
        added.setType("myType");
        result.addAddedColumn(added);
        
        Column removed = new Column("wech");
        removed.setDefault("nix");
        removed.setNullable(true);
        removed.setPosition(23);
        removed.setSize(666);
        removed.setType("myType");
        result.addRemovedColumn(removed);
        
        Column modOrg = new Column("orischinall");
        modOrg.setDefault("orgding");
        modOrg.setNullable(true);
        modOrg.setPosition(23);
        modOrg.setSize(666);
        modOrg.setType("myType");
        
        Column modNew = new Column("moddifaied");
        modNew.setDefault("modding");
        modNew.setNullable(false);
        modNew.setPosition(42);
        modNew.setSize(999);
        modNew.setType("myType");
        
        result.putModifiedColumns(modOrg, modNew);
        
        return result;
    }

    /* (non-Javadoc)
     * @see henplus.Command#complete(henplus.CommandDispatcher, java.lang.String, java.lang.String)
     */
    public Iterator complete(CommandDispatcher disp, 
                                            String partialCommand, 
                                            final String lastWord) {
        
        StringTokenizer st = new StringTokenizer(partialCommand);
        String cmd = st.nextToken();
        int argIndex = st.countTokens();
        
        // System.out.println("[complete] partialCommand: '"+partialCommand+"', lastWord: '" + lastWord+"'");
        /*
         * the following input is given:
         * "command token1 [TAB_PRESSED]"
         * in this case the partialCommand is "command token1", the last word has a length 0!
         * 
         * another input:
         * "command toke[TAB_PRESSED]"
         * then the partialCommand is "command toke", the last word is "toke".
         */
        if (lastWord.length() > 0) {
            argIndex--;
        }
        
        // process the first session
        if (argIndex == 0) {
            return HenPlus.getInstance().getSessionManager().completeSessionName(lastWord);
        }
        // process the second session
        else if (argIndex == 1) {
            final String firstSession = st.nextToken();
            return getSecondSessionCompleter(lastWord, firstSession);
        }
        // process tables
        else if (argIndex > 1) {
            SessionManager sessionManager = HenPlus.getInstance().getSessionManager();
            SQLSession first = sessionManager.getSessionByName(st.nextToken());
            SQLSession second = sessionManager.getSessionByName(st.nextToken());
            
            final HashSet  alreadyGiven = new HashSet();
            while (st.hasMoreElements()) {
                alreadyGiven.add(st.nextToken());
            }
            ListUserObjectsCommand objectList = HenPlus.getInstance().getObjectLister();
            final Iterator firstIter = objectList.completeTableName(first, lastWord);
            final Iterator secondIter = objectList.completeTableName(second, lastWord);
            final Iterator iter = getIntersection(firstIter, secondIter);
            return new Iterator() {
                String table = null;
                public boolean hasNext() {
                    while (iter.hasNext()) {
                        table = (String) iter.next();
                        if (alreadyGiven.contains(table) && !lastWord.equals(table)) {
                            continue;
                        }
                        return true;
                    }
                    return false;
                }
                public Object  next() { return table; }
                public void remove() { 
                    throw new UnsupportedOperationException("no!");
                }
            };
        }
        return null;
    }
    
    private Iterator getIntersection(Iterator first, Iterator second) {
        // first copy the first iterator into a list
        List contentFirst = new ArrayList();
        while (first.hasNext()) {
            contentFirst.add(first.next());
        }
        // now copy all items of the second iterator into a second list
        // which are contained in the first list
        List inter = new ArrayList();
        while (second.hasNext()) {
            Object next = second.next();
            if (contentFirst.contains(next)) {
                inter.add(next);
            }
        }
        return inter.iterator();
    }
    
    private Iterator getSecondSessionCompleter(String lastWord, final String firstSession) {
        final Iterator it = HenPlus.getInstance().getSessionManager().completeSessionName(lastWord);
        return new Iterator() {
            String session = null;
            public boolean hasNext() {
                while (it.hasNext()) {
                    session = (String) it.next();
                    if (session.equals(firstSession)) {
                        continue;
                    }
                    return true;
                }
                return false;
            }
            public Object  next() { return session; }
            public void remove() { 
                throw new UnsupportedOperationException("no!");
            }
        };
    }

    /* (non-Javadoc)
     * @see henplus.Command#isComplete(java.lang.String)
     */
    public boolean isComplete(String command) {
        if ( command.trim().endsWith(COMMAND_DELIMITER) ) {
            return true;
            /*
            StringTokenizer st = new StringTokenizer(command);
            // we need at least four tokens.
            final int minTokens = 4;
            int count = 0;
            while (st.hasMoreTokens() && count < minTokens) {
                count++;
            }
            */
        }
        return false;
    }

    /* (non-Javadoc)
     * @see henplus.Command#requiresValidSession(java.lang.String)
     */
    public boolean requiresValidSession(String cmd) {
        return false;
    }

    /* (non-Javadoc)
     * @see henplus.Command#shutdown()
     */
    public void shutdown() {
    }

    /* (non-Javadoc)
     * @see henplus.Command#getShortDescription()
     */
    public String getShortDescription() {
        return "Perform a diff on tables from two sessions";
    }

    /* (non-Javadoc)
     * @see henplus.Command#getSynopsis(java.lang.String)
     */
    public String getSynopsis(String cmd) {
        return _command + " <sessionname_1> <sessionname_2> <tablename> [<tablename> ..];";
    }

    /* (non-Javadoc)
     * @see henplus.Command#getLongDescription(java.lang.String)
     */
    public String getLongDescription(String cmd) {
        return "\tCompare one or more tables by their meta data.\n" +                    "\tThe following is a list of compared column related\n" +                    "\tproperties, with a \"c\" for a case sensitive and an \"i\" for\n" +                    "\ta case insensitive comparision by default. If you\n" +                    "\twonder what this is for, because you know that sql\n" +                    "\tshould behave case insensitive, then ask your\n" +                    "\tdatabase provider or the developer of the driver you use.\n" +                    "\n" +                    "\t - column name (i)\n" +
                    "\t - type (c)\n" +                    "\t - nullable (-)\n" +                    "\t - default value (c)\n" +                    "\t - primary key definition (c)\n" +                    "\t - foreign key definition (c).\n" +                    "\n" +                    "\tIn the future indices migth be added to the comparison,\n" +                    "\tmoreover, an option \"o\" would be nice to get automatically\n" +                    "\t\"ALTER TABLE ...\" scripts generated to a given output file.";
    }

}
