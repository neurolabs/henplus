/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import henplus.commands.SetCommand;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.gnu.readline.ReadlineCompleter;

/**
 * document me.
 */
public class CommandDispatcher implements ReadlineCompleter {
    private final static boolean verbose = false; // debug
    private final List/*<Command>*/ commands; // commands in seq. of addition.
    private final SortedMap commandMap;
    private final SetCommand setCommand;
    private final List/*<ExecutionL<Listener>*/ executionListeners;
    private int   _batchCount;

    public CommandDispatcher(SetCommand sc) {
	commandMap = new TreeMap();
	commands = new ArrayList();
        executionListeners = new ArrayList();
	setCommand = sc;
	_batchCount = 0;
    }

    /**
     * returns the commands in the sequence they have been added.
     */
    public Iterator getRegisteredCommands() {
	return commands.iterator();
    }

    /**
     * returns a sorted list of command names.
     */
    public Iterator getRegisteredCommandNames() {
	return commandMap.keySet().iterator();
    }

    /**
     * returns a sorted list of command names, starting with the first entry
     * matching the key.
     */
    public Iterator getRegisteredCommandNames(String key) {
	return commandMap.tailMap(key).keySet().iterator();
    }

    /*
     * if we start a batch (reading from file), the commands are not shown,
     * except the commands that failed.
     */
    public void startBatch() { ++_batchCount; }
    public void endBatch() { --_batchCount; }
    public boolean isInBatch() { return _batchCount > 0; }

    public void register(Command c) {
	commands.add(c);
	String[] cmdStrings = c.getCommandList();
	for (int i = 0; i < cmdStrings.length; ++i) {
	    if (commandMap.containsKey(cmdStrings[i]))
		throw new IllegalArgumentException("attempt to register command '" + cmdStrings[i] + "', that is already used");
	    commandMap.put(cmdStrings[i], c);
	}
    }

    // methods to make aliases work.
    public boolean containsCommand(String cmd) {
	return commandMap.containsKey(cmd);
    }

    public void registerAdditionalCommand(String cmd, Command c) {
	commandMap.put(cmd, c);
    }
    
    public void unregisterAdditionalCommand(String cmd) {
	commandMap.remove(cmd);
    }

    /**
     * unregister command. This is an 'expensive' operation, since we
     * go through the internal list until we find the command and remove
     * it. But since the number of commands is low and this is a rare
     * operation (the plugin-mechanism does this) .. we don't care.
     */
    public void unregister(Command c) {
	commands.remove(c);
	Iterator entries = commandMap.entrySet().iterator();
	while (entries.hasNext()) {
	    Map.Entry e = (Map.Entry) entries.next();
	    if (e.getValue() == c) {
                entries.remove();
            }
	}
    }

    /**
     * extracts the command from the commandstring. This even works, if there
     * is not delimiter between the command and its arguments (this is esp.
     * needed for the commands '?', '!', '@' and '@@').
     */
    public String getCommandNameFrom(String completeCmd) {
	if (completeCmd == null || completeCmd.length() == 0) return null;
	String cmd = completeCmd.toLowerCase();
	final String startChar = cmd.substring(0, 1);
	Iterator it = getRegisteredCommandNames(startChar);
	String longestMatch = null;
	while (it.hasNext()) {
	    String testMatch = (String) it.next();
	    if (cmd.startsWith(testMatch)) {
		longestMatch = testMatch;
	    }
	    else if (!testMatch.startsWith(startChar)) {
		break; // ok, thats it.
	    }
	}
	// ok, fallback: grab the first whitespace delimited part.
	if (longestMatch == null) {
	    Enumeration tok = new StringTokenizer(completeCmd, " ;\t\n\r\f");
	    if (tok.hasMoreElements()) {
		return (String) tok.nextElement();
	    }
	}
	return longestMatch;
    }

    public Command getCommandFrom(String completeCmd) {
	return getCommandFromCooked(getCommandNameFrom(completeCmd));
    }

    private Command getCommandFromCooked(String completeCmd) {
	if (completeCmd == null) return null;
	Command c = (Command) commandMap.get(completeCmd);
	if (c == null) {
	    c = (Command) commandMap.get(""); // "" matches everything.
	}
	return c;
    }
    
    public void shutdown() {
	Iterator i = commands.iterator();
	while (i.hasNext()) {
	    Command c = (Command) i.next();
            try {
                c.shutdown();
            }
            catch (Exception e) {
                if (verbose) e.printStackTrace();
            }
	}
    }
    
    /**
     * Add an execution listener that is informed whenever a command
     * is executed.
     * @param listener an Execution Listener
     */
    public void addExecutionListener(ExecutionListener listener) {
        if (!executionListeners.contains(listener)) {
            executionListeners.add(listener);
        }
    }

    /**
     * remove an execution listener.
     * @param listener the execution listener to be removed
     * @return true, if this has been successful.
     */
    public boolean removeExecutionListener(ExecutionListener listener) {
        return executionListeners.remove(listener);
    }

    private void informBeforeListeners(SQLSession session, String cmd) {
        Iterator it = executionListeners.iterator();
        while (it.hasNext()) {
            ExecutionListener listener = (ExecutionListener) it.next();
            listener.beforeExecution(session, cmd);
        }
    }

    private void informAfterListeners(SQLSession session, String cmd, 
                                      int result) 
    {
        Iterator it = executionListeners.iterator();
        while (it.hasNext()) {
            ExecutionListener listener = (ExecutionListener) it.next();
            listener.afterExecution(session, cmd, result);
        }
    }

    /**
     * execute the command given. This strips whitespaces and trailing
     * semicolons and calls the Command class.
     */
    public void execute(SQLSession session, final String givenCommand) {
	if (givenCommand == null)
	    return;

	// remove trailing ';' and whitespaces.
	StringBuffer cmdBuf = new StringBuffer(givenCommand.trim());
	int i = 0;
	for (i = cmdBuf.length()-1; i > 0; --i) {
	    char c = cmdBuf.charAt(i);
	    if (c != ';'
		&& !Character.isWhitespace(c))
		break;
	}
	if (i < 0) {
	    return;
	}
	cmdBuf.setLength(i+1);
	String cmd = cmdBuf.toString();
	//System.err.println("## '" + cmd + "'");
	String cmdStr = getCommandNameFrom(cmd);
	Command c = getCommandFromCooked(cmdStr);
	//System.err.println("name: "+  cmdStr + "; c=" + c);
	if (c != null) {
	    try {
		cmd = cmd.substring(cmdStr.length());
		if (session == null && c.requiresValidSession(cmdStr)) {
		    System.err.println("not connected.");
		    return;
		}
                
                int result;
                informBeforeListeners(session, givenCommand);
                result = c.execute(session, cmdStr, cmd);
                informAfterListeners(session, givenCommand, result);

		switch ( result ) {
		case Command.SYNTAX_ERROR: {
		    String synopsis = c.getSynopsis(cmdStr);
		    if (synopsis != null) {
			System.err.println("usage: " + synopsis);
		    }
		    else {
			System.err.println("syntax error.");
		    }
		}
		    break;
		case Command.EXEC_FAILED: {
                    /*
                     * if we are in batch mode, then no message is written
                     * to the screen by default. Thus we don't know, _what_
                     * command actually failed. So in this case, write out
                     * the offending command.
                     */
		    if ( isInBatch() ) {
			System.err.println("-- failed command: ");
			System.err.println(cmdStr + " " + cmd);
		    }
		}
		    break;
                default:
                    /* nope */
		}
	    }
	    catch (Throwable e) {
		if (verbose) e.printStackTrace();
		System.err.println(e);
                informAfterListeners(session, givenCommand, 
                                     Command.EXEC_FAILED);
	    }
	}
    }

    private Iterator possibleValues;
    private String   variablePrefix;

    //-- Readline completer ..
    public String completer(String text, int state) {
	final HenPlus henplus = HenPlus.getInstance();
	String completeCommandString = henplus.getPartialLine().trim();
	boolean variableExpansion = false;

	/*
	 * ok, do we have a variable expansion ?
	 */
	int pos = text.length()-1;
	while (pos > 0
	       && (text.charAt(pos) != '$')
	       && Character
	       .isJavaIdentifierPart(text.charAt(pos))) {
	    --pos;
	}
	// either $... or ${...
	if ((pos >= 0 && text.charAt(pos) == '$')) {
	    variableExpansion = true;
	}
	else if ((pos >= 1) 
		 && text.charAt(pos-1) == '$'
		 && text.charAt(pos) == '{') {
	    variableExpansion = true;
	    --pos;
	}
	
	if (variableExpansion) {
	    if (state == 0) {
		variablePrefix = text.substring(0, pos);
		String varname = text.substring(pos);
		possibleValues = setCommand.completeUserVar(varname);
	    }
	    if (possibleValues.hasNext()) {
		return variablePrefix + ((String) possibleValues.next());
	    }
	    return null;
	}
	/*
	 * the first word.. the command.
	 */
	else if (completeCommandString.equals(text)) {
	    text = text.toLowerCase();
	    if (state == 0) {
		possibleValues = getRegisteredCommandNames(text);
	    }
	    while (possibleValues.hasNext()) {
		String nextKey = (String) possibleValues.next();
		if (nextKey.length() == 0)// don't complete the 'empty' thing.
		    continue;
		if (text.length() < 1) {
		    Command c = (Command) commandMap.get(nextKey);
		    if (!c.participateInCommandCompletion())
			continue;
		    if (c.requiresValidSession(nextKey) 
			&& henplus.getCurrentSession() == null) {
			continue;
		    }
		}
		if (nextKey.startsWith(text))
		    return nextKey;
		return null;
	    }
	    return null;
	}
	/*
	 * .. otherwise get completion from the specific command.
	 */
	else {
	    if (state == 0) {
		Command cmd = getCommandFrom(completeCommandString);
		if (cmd == null) {
		    return null;
		}
		possibleValues = cmd.complete(this,completeCommandString,text);
	    }
	    if (possibleValues != null && possibleValues.hasNext()) {
		return (String) possibleValues.next();
	    }
	    return null;
	}
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
