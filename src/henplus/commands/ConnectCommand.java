/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import java.util.SortedMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.StringTokenizer;
import java.util.Iterator;

import HenPlus;
import SQLSession;
import AbstractCommand;
import CommandDispatcher;

/**
 * document me.
 */
public class ConnectCommand extends AbstractCommand {
    private SortedMap/*<String,SQLSession>*/ sessions;
    /**
     * the current session we are in.
     */
    private String currentSessionName = null;

    public ConnectCommand(String argv[], HenPlus henplus) {
	sessions = new TreeMap();
	if (argv.length > 0) {
	    try {
		SQLSession session = null;
		String url = argv[0];
		String username = (argv.length > 1) ? argv[1] : null;
		String password = (argv.length > 2) ? argv[2] : null;
		session = new SQLSession(url, username, password);
		String sessionName = createSessionName(session, null);
		sessions.put(sessionName, session);
		henplus.setPrompt(sessionName + "> ");
		henplus.setSession(session);
	    }
	    catch (Exception e) {
		System.err.println(e.getMessage());
	    }
	}
    }

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "connect", "disconnect", "switch"
	};
    }
    
    private String createSessionName(SQLSession session, String name) {
	if (name == null || name.length() == 0) {
	    StringBuffer result = new StringBuffer();
	    if (session.getUsername() != null) {
		result.append(session.getUsername());
		result.append('@');
	    }
	    StringTokenizer st = new StringTokenizer(session.getURL(), ":");
	    try {
		st.nextElement();
		result.append(st.nextElement());
	    }
	    catch (Exception ign) {}
	    name = result.toString();
	}
	String key = name;
	int count = 0;
	while (sessions.containsKey(key)) {
	    ++count;
	    key = name + "#" + count;
	}
	return key;
    }

    /**
     * we can connect, even if we don't have a running connection.
     */
    public boolean requiresValidSession(String cmd) { 
	if ("connect".equals(cmd)) {
	    return false;
	}
	return true;
    }

    /**
     * complete session names. But not the session we are currently in, since
     * we don't want to switch to our own session, right ?
     */
    public Iterator complete(CommandDispatcher disp,
			     String partialCommand, final String lastWord) 
    {
	if (partialCommand.startsWith("switch")) {
	    if (argumentCount(partialCommand) >
		("".equals(lastWord) ? 1 : 2)) {
		return null;
	    }
	    final Iterator it = sessions.tailMap(lastWord)
		.keySet().iterator();
	    return new Iterator() {
		    String sessionName = null;
		    public boolean hasNext() {
			while (it.hasNext()) {
			    sessionName = (String) it.next();
			    if (!sessionName.startsWith(lastWord)) {
				return false;
			    }
			    if (sessionName.equals(currentSessionName)) {
				continue;
			    }
			    return true;
			}
			return false;
		    }
		    public Object  next() { return sessionName; }
		    public void remove() { 
			throw new UnsupportedOperationException("no!");
		    }
		};
	}
	return null; 
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession currentSession, String command) {
	SQLSession session = null;
	currentSessionName = null;

	StringTokenizer st = new StringTokenizer(command);
	String cmd = (String) st.nextElement();
	int argc = st.countTokens();

	if ("connect".equals(cmd)) {
	    if (argc < 1 || argc > 2) {
		return SYNTAX_ERROR;
	    }
	    String url = (String) st.nextElement();
	    if (argc == 2) {
		currentSessionName = (String) st.nextElement();
	    }
	    try {
		session = new SQLSession(url, null, null);
		currentSessionName = createSessionName(session, 
						       currentSessionName);
		sessions.put(currentSessionName, session);
	    }
	    catch (Exception e) {
		System.err.println(e);
		return EXEC_FAILED;
	    }
	}
	
	else if ("switch".equals(cmd)) {
	    if (argc != 1) {
		return SYNTAX_ERROR;
	    }
	    currentSessionName = (String) st.nextElement();
	    session = (SQLSession) sessions.get(currentSessionName);
	    if (session == null) {
		return EXEC_FAILED;
	    }
	}

	else if ("disconnect".equals(cmd)) {
	    if (argc != 0) {
		return SYNTAX_ERROR;
	    }
	    // find session..
	    Map.Entry entry = null;
	    Iterator it = sessions.entrySet().iterator();
	    while (it.hasNext()) {
		entry = (Map.Entry) it.next();
		if (entry.getValue() == currentSession) {
		    it.remove();
		    currentSession.close();
		    System.err.println("session closed.");
		    break;
		}
	    }
	    if (!sessions.isEmpty()) {
		currentSessionName = (String) sessions.firstKey();
		session = (SQLSession) sessions.get(currentSessionName);
	    }
	}
	
	if (currentSessionName != null) {
	    HenPlus.getInstance().setPrompt(currentSessionName + "> ");
	}
	else {
	    HenPlus.getInstance().setDefaultPrompt();
	}
	HenPlus.getInstance().setSession(session);

	return SUCCESS;
    }

    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "[dis]connect/switch to session";
    }

    public String getSynopsis(String cmd) {
	if ("connect".equals(cmd)) {
	    return "connect <url> [session-name]"; 
	}
	else if ("switch".equals(cmd)) {
	    return "switch <session-name>";
	}
	return cmd;
    }

    public String getLongDescription(String cmd) { 
	String dsc = null;
	if ("connect".equals(cmd)) {
	    dsc= "\tconnects to the url with the optional session name.\n"
		+"\tIf no session name is given, a session name is chosen.";
	}
	else if ("disconnect".equals(cmd)) {
	    dsc="\tdisconnect current session.";
	}
	else if ("switch".equals(cmd)) {
	    dsc="\tswitch to session with the given session name.";
	}
	return dsc;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
