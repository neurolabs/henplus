/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */

import java.util.List;
import java.util.SortedMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.TreeMap;
import java.util.Enumeration;
import java.util.StringTokenizer;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineCompleter;

import commands.SetCommand;

/**
 * document me.
 */
public class CommandDispatcher implements ReadlineCompleter {
    private final List commands;  // commands in sequence of addition.
    private final SortedMap commandMap;
    private final SetCommand setCommand;
    private int   _batchCount;

    public CommandDispatcher(SetCommand sc) {
	commandMap = new TreeMap();
	commands = new Vector();
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

    public void register(Command c) {
	commands.add(c);
	String[] cmdStrings = c.getCommandList();
	for (int i = 0; i < cmdStrings.length; ++i) {
	    if (commandMap.containsKey(cmdStrings[i]))
		throw new Error("DEVELOPER: please choose another name, this one is already used: '" + cmdStrings[i] + "'");
	    commandMap.put(cmdStrings[i], c);
	}
    }
    
    private String getCommandNameFrom(String completeCmd) {
	Enumeration tok = new StringTokenizer(completeCmd, " ;\t\n\r\f");
	if (tok.hasMoreElements()) {
	    return (String) tok.nextElement();
	}
	return null;
    }

    /**
     * returns the command from the complete command string.
     */
    public Command getCommandFrom(String completeCmd) {
	String cmd = getCommandNameFrom(completeCmd);
	if (cmd == null) {
	    return null;
	}
	Command c = (Command) commandMap.get(cmd);
	if (c == null) {
	    c = (Command) commandMap.get(""); // "" matches everything.
	}
	return c;
    }
    
    public void shutdown() {
	Iterator i = commandMap.values().iterator();
	while (i.hasNext()) {
	    Command c = (Command) i.next();
	    c.shutdown();
	}
    }

    /**
     * execute the command given. This strips whitespaces and trailing
     * semicolons and calls the Command class.
     */
    public void execute(SQLSession session, String cmd) {
	if (cmd == null)
	    return;
	// remove trailing ';' and whitespaces.
	StringBuffer cmdBuf = new StringBuffer(cmd.trim());
	int i = 0;
	for (i = cmdBuf.length()-1; i > 0; --i) {
	    char c = cmdBuf.charAt(i);
	    if (c != ';'
		&& !Character.isWhitespace(c))
		break;
	}
	if (i <= 0) {
	    return;
	}
	cmdBuf.setLength(i+1);
	cmd = cmdBuf.toString();
	//System.err.println("## '" + cmd + "'");
	String cmdStr = getCommandNameFrom(cmd);
	Command c = getCommandFrom(cmd);
	if (c != null) {
	    try {
		if (session == null && c.requiresValidSession(cmdStr)) {
		    System.err.println("not connected.");
		    return;
		}
		switch (c.execute(session, cmd)) {
		case Command.SYNTAX_ERROR: {
		    String synopsis = c.getSynopsis(cmdStr);
		    if (synopsis != null) {
			System.err.println("usage: " + synopsis);
		    }
		    else {
			System.err.println("syntax error.");
		    }
		    break;
		}
		case Command.EXEC_FAILED: {
		    if (_batchCount > 0) {
			System.err.println("-- failed command: ");
			System.err.println(cmd);
		    }
		}
		}
	    }
	    catch (Exception e) {
		System.err.println(e);
	    }
	}
    }

    private Iterator possibleValues;
    private String   variablePrefix;

    //-- Readline completer ..
    public String completer(String text, int state) {
	String completeCommandString = Readline.getLineBuffer().trim();
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
			&& HenPlus.getInstance().getSession() == null) {
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
