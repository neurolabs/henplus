/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: TableDiffCommand.java,v 1.5 2004-03-23 11:06:40 magrokosmos Exp $ 
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
import henplus.view.util.NameCompleter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;

public final class TableDiffCommand implements Command, Interruptable {
    
    //private final ListUserObjectsCommand _tableCompleter;
    private static final boolean verbose     = true;
    
    protected static final String _command = "tablediff";
    protected static final String COMMAND_DELIMITER = ";";
    protected static final String OPTION_PREFIX = "-";
    protected static final String OPTION_CASESENSITIVE = "c";
    
    private volatile boolean _interrupted = false;

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
        
        if (st.countTokens() < 3) {
            return SYNTAX_ERROR;
        }

        SessionManager sessionManager = HenPlus.getInstance().getSessionManager();
        
        if ( sessionManager.getSessionCount() < 2 ) {
            System.err.println("You need two valid sessions for this command.");
            return SYNTAX_ERROR;
        }
            
        SQLSession first = sessionManager.getSessionByName(st.nextToken());
        SQLSession second = sessionManager.getSessionByName(st.nextToken());
        
        if (first == null || second == null) {
            HenPlus.msg().println("You need two valid sessions for this command.");
            return EXEC_FAILED;
        }
        else if (first == second) {
            HenPlus.msg().println("You should specify two different sessions for this command.");
            return EXEC_FAILED;
        }
        else if (!st.hasMoreTokens()) {
            HenPlus.msg().println("You should specify at least one table.");
            return EXEC_FAILED;
        }
        
        try {
            long start = System.currentTimeMillis();
            int count = 0;
            
            String nextToken = st.nextToken();

            ListUserObjectsCommand objectLister = HenPlus.getInstance().getObjectLister();
            SortedSet tablesOne = objectLister.getTableNamesForSession( first );
            SortedSet tablesTwo = objectLister.getTableNamesForSession( second );
            
            Set alreadyDiffed = new HashSet();      // which tables got already diffed?
            
            /*
             * which tables are found in the first session via wildcards but are not contained
             * in the second session?
             */
            ArrayList missedFromWildcards = new ArrayList();
            
            do {
        
                if ( "*".equals( nextToken ) ) {
                    Iterator iter = objectLister.getTableNamesIteratorForSession( first );
                    while ( iter.hasNext() ) {
                        Object objTableName = iter.next();
                        count =
                            diffConditionally(
                                objTableName,
                                colNameIgnoreCase,
                                first,
                                second,
                                tablesTwo,
                                alreadyDiffed,
                                missedFromWildcards,
                                count);
                    }
                }
                else if ( nextToken.indexOf( '*' ) > -1 ) {
                    String tablePrefix = nextToken.substring( 0, nextToken.length() -1 );
                    
                    NameCompleter compl = new NameCompleter( tablesOne );
                    Iterator iter = compl.getAlternatives( tablePrefix );
                    while ( iter.hasNext() ) {
                        Object match = iter.next();
                        count =
                            diffConditionally(
                                match,
                                colNameIgnoreCase,
                                first,
                                second,
                                tablesTwo,
                                alreadyDiffed,
                                missedFromWildcards,
                                count);
                    }
                    
                }
                else if ( !alreadyDiffed.contains( nextToken ) ) {
                    diffTable(first, second, nextToken, colNameIgnoreCase);
                    alreadyDiffed.add( nextToken );
                    count++;
                }
            } while ( st.hasMoreTokens() && ( nextToken = st.nextToken() ) != null );
            
            StringBuffer msg = new StringBuffer();
            msg.append("Diffing ").append(count).append((count == 1)?" table took ":" tables took ").
            append(System.currentTimeMillis() - start).append(" ms.");
            
            // if there were tables found via wildcards but not contained in both sessions then let
            // the user know this.
            if ( missedFromWildcards.size() > 0 ) {
                msg.append( "\nTables which matched a given wildcard in your first\n" +                    "session but were not found in your second session:\n");
                Iterator iter = missedFromWildcards.iterator();
                while ( iter.hasNext() ) {
                    msg.append( iter.next() ).append( ", " );
                }
                // remove the last two chars
                msg.delete( msg.length() - 2, msg.length() );
            }
            
            HenPlus.msg().println(msg.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return SUCCESS;
    }

    private int diffConditionally(
        Object objTableName,
        boolean colNameIgnoreCase,
        SQLSession first,
        SQLSession second,
        SortedSet tablesTwo,
        Set alreadyDiffed,
        List missedFromWildcards,
        int count) {
        if ( tablesTwo.contains( objTableName ) ) {
            if ( !alreadyDiffed.contains( objTableName ) ) {
                String tableName = ( String )objTableName;
                diffTable(first, second, tableName, colNameIgnoreCase);
                alreadyDiffed.add( objTableName );
                count++;
            }
        }
        else {
            missedFromWildcards.add( objTableName );
        }
        return count;
    }

    private void diffTable(
        SQLSession first,
        SQLSession second,
        String tableName,
        boolean colNameIgnoreCase) {
        Table ref = first.getTable( tableName );
        Table diff = second.getTable( tableName );
        TableDiffResult diffResult = TableDiffer.diffTables(ref, diff, colNameIgnoreCase);
        if (diffResult == null) {
            HenPlus.msg().println("No diff for table " + tableName);
        }
        else {
            HenPlus.msg().println("Diff result for table " + tableName + ":");
            ResultTablePrinter.printResult(diffResult);
        }
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
        return "perform a diff on meta data of tables from two sessions";
    }

    /* (non-Javadoc)
     * @see henplus.Command#getSynopsis(java.lang.String)
     */
    public String getSynopsis(String cmd) {
        return _command + " <sessionname-1> <sessionname-2> ( <tablename> | <prefix>\\* | \\* )+;";
    }

    /* (non-Javadoc)
     * @see henplus.Command#getLongDescription(java.lang.String)
     */
    public String getLongDescription(String cmd) {
        return "\tCompare one or more tables by their meta data.\n"
            +"\n"
            +"\tYou are able to use wildcards (*) to match all tables or\n"
            +"\ta specific set of tables.\n"
            +"\tE.g. you might specify \"*\" to match all tables which are contained\n"
            +"\tin both sessions, or\"tb_*\" to match all tables from your sessions\n"
            +"\tstarting with \"tb_\".\n"
            +"\n"
            +"\tThe following is a list of compared column related\n" 
            +"\tproperties, with a \"c\" for a case sensitive and an \"i\" for\n" 
            +"\ta case insensitive comparision by default. If you\n" 
            +"\twonder what this is for, because you know that sql\n" 
            +"\tshould behave case insensitive, then ask your\n" 
            +"\tdatabase provider or the developer of the driver you use.\n"
            +"\n" 
            +"\t - column name (i)\n" 
            +"\t - type (c)\n" 
            +"\t - nullable (-)\n" 
            +"\t - default value (c)\n" 
            +"\t - primary key definition (c)\n"
            +"\t - foreign key definition (c).\n"
            +"\n"
            +"\tIn the future indices migth be added to the comparison,\n"
            +"\tmoreover, an option \"o\" would be nice to get automatically\n"
            +"\t\"ALTER TABLE ...\" scripts generated to a given output file.";
    }
}
