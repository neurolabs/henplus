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

/**
 * document me.
 */
public class CommandDispatcher implements ReadlineCompleter {
    private final List commands;  // commands in sequence of addition.
    private final SortedMap commandMap;

    public CommandDispatcher() {
	commandMap = new TreeMap();
	commands = new Vector();
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
	Command c = (Command) commandMap.get(cmd);
	if (c == null) {
	    c = (Command) commandMap.get(""); // "" matches everything.
	}
	return c;
    }

    /**
     * execute the command given. This strips whitespaces and trailing
     * semicolons and calls the Command class.
     */
    public void execute(SQLSession session, String cmd) {
	if (cmd == null)
	    return;
	// bogus: ';' separated ..
	StringBuffer cmdBuf = new StringBuffer(cmd.trim());
	int i = 0;
	for (i = cmdBuf.length()-1; i > 0; --i) {
	    char c = cmdBuf.charAt(i);
	    if (c != ';'
		&& !Character.isWhitespace(c))
		break;
	}
	cmdBuf.setLength(i+1);
	cmd = cmdBuf.toString();
	if (cmd.length() == 0) 
	    return;
	String cmdStr = getCommandNameFrom(cmd);
	Command c = getCommandFrom(cmd);
	if (c != null) {
	    if (c.execute(session, cmd) == Command.SYNTAX_ERROR) {
		String synopsis = c.getSynopsis(cmdStr);
		if (synopsis != null)
		    System.err.println("usage: " + synopsis);
	    }
	}
    }

    private Iterator possibleValues;
    //-- Readline completer ..
    public String completer(String text, int state) {
	String completeCommandString = Readline.getLineBuffer().trim();
	/*
	 * the first word.. the command.
	 */
	if (completeCommandString.equals(text)) {
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
