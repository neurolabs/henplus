/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.StringTokenizer;
import java.util.Iterator;

import henplus.HenPlus;
import henplus.SQLSession;
import henplus.AbstractCommand;
import henplus.CommandDispatcher;

/**
 * document me.
 */
public class ConnectCommand extends AbstractCommand {
    private static String CONNECTION_CONFIG = "connections";
    private final SortedMap/*<String,SQLSession>*/ sessions;
    private final SortedSet knownUrls;
    private final HenPlus   henplus;

    /**
     * the current session we are in.
     */
    private String currentSessionName = null;

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "connect", "disconnect", "rename-session", "switch", "sessions"
	};
    }

    public ConnectCommand(String argv[], HenPlus henplus) {
	this.henplus = henplus;
	sessions  = new TreeMap();
	knownUrls = new TreeSet();

	try {
	    File urlFile = new File(henplus.getConfigDir(),
				    CONNECTION_CONFIG);
	    BufferedReader in = new BufferedReader(new FileReader(urlFile));
	    String url;
	    while ((url = in.readLine()) != null) {
		knownUrls.add(url);
	    }
	}
	catch (IOException e) {
	}

	if (argv.length > 0) {
	    try {
		SQLSession session = null;
		String url = argv[0];
		String username = (argv.length > 1) ? argv[1] : null;
		String password = (argv.length > 2) ? argv[2] : null;
		session = new SQLSession(url, username, password);
		currentSessionName = createSessionName(session, null);
		sessions.put(currentSessionName, session);
		knownUrls.add(url);
		henplus.setPrompt(currentSessionName + "> ");
		henplus.setSession(session);
	    }
	    catch (Exception e) {
		//e.printStackTrace();
		System.err.println(e.getMessage());
	    }
	}
    }
    
    private String createSessionName(SQLSession session, String name) {
	String userName = null;
	String dbName   = null;
	String hostname = null;
	String url = session.getURL();

	if (name == null || name.length() == 0) {
	    StringBuffer result = new StringBuffer();
	    userName = session.getUsername();
	    StringTokenizer st = new StringTokenizer(url, ":");
	    while (st.hasMoreElements()) {
		String val = (String) st.nextElement();
		if (val.toUpperCase().equals("JDBC"))
		    continue;
		dbName = val;
		break;
	    }
	    int pos;
	    if ((pos = url.indexOf('@')) >= 0) {
		st = new StringTokenizer(url.substring(pos+1), ":/");
		try { 
		    hostname = (String) st.nextElement(); 
		} 
		catch (Exception e) { /* ignore */ }
	    }
	    else if ((pos = url.indexOf('/')) >= 0) {
		st = new StringTokenizer(url.substring(pos+1), ":/");
		while (st.hasMoreElements()) {
		    String val = (String) st.nextElement();
		    if (val.length() == 0)
			continue;
		    hostname = val;
		    break;
		}
	    }
	    if (userName != null) result.append(userName + "@");
	    if (dbName != null)   result.append(dbName);
	    if (dbName != null && hostname != null) result.append(":");
	    if (hostname != null) result.append(hostname);
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

    public void shutdown() {
	Iterator sessIter = sessions.values().iterator();
	while (sessIter.hasNext()) {
	    SQLSession session = (SQLSession) sessIter.next();
	    session.close();
	}

	try {
	    File urlFile = new File(henplus.getConfigDir(),
				    CONNECTION_CONFIG);
	    PrintWriter writer = new PrintWriter(new FileWriter(urlFile));
	    Iterator urlIter = knownUrls.iterator();
	    while (urlIter.hasNext()) {
		writer.println((String) urlIter.next());
	    }
	    writer.close();
	}
	catch (IOException dont_care) {}
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
	if (partialCommand.startsWith("connect")) {
	    if (argumentCount(partialCommand) >
		("".equals(lastWord) ? 1 : 2)) {
		return null;
	    }
	    final Iterator it = knownUrls.tailSet(lastWord).iterator();
	    return new Iterator() {
		    String url = null;
		    public boolean hasNext() {
			while (it.hasNext()) {
			    url = (String) it.next();
			    if (!url.startsWith(lastWord)) {
				return false;
			    }
			    return true;
			}
			return false;
		    }
		    public Object  next() { return url; }
		    public void remove() { 
			throw new UnsupportedOperationException("no!");
		    }
		};
	}
	
	else if (partialCommand.startsWith("switch")) {
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

	StringTokenizer st = new StringTokenizer(command);
	String cmd = (String) st.nextElement();
	int argc = st.countTokens();
	
	if ("sessions".equals(cmd)) {
	    Map.Entry entry = null;
	    Iterator it = sessions.entrySet().iterator();
	    while (it.hasNext()) {
		entry = (Map.Entry) it.next();
		String sessName = (String) entry.getKey(); 
		session         = (SQLSession) entry.getValue();
		if (sessName.equals(currentSessionName)) {
		    System.err.print(" * ");
		}
		else {
		    System.err.print("   ");
		}
		System.err.print(sessName + "\t");
		System.err.println(session.getURL());
	    }
	    return SUCCESS;
	}

	else if ("connect".equals(cmd)) {
	    if (argc < 1 || argc > 2) {
		return SYNTAX_ERROR;
	    }
	    String url = (String) st.nextElement();
	    currentSessionName = (argc==2) ? (String) st.nextElement() : null;
	    try {
		session = new SQLSession(url, null, null);
		knownUrls.add(url);
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
	    String sessionName = null;
	    if (argc != 1 && sessions.size() != 2) {
		return SYNTAX_ERROR;
	    }
	    if (argc == 0 && sessions.size() == 2) {
		Iterator i = sessions.keySet().iterator();
		while (i.hasNext()) {
		    sessionName = (String) i.next();
		    if (!sessionName.equals(currentSessionName)) {
			break;
		    }
		}
	    }
	    else {
		sessionName = (String) st.nextElement();
	    }
	    session = (SQLSession) sessions.get(sessionName);
	    if (session == null) {
		System.err.println("'" + sessionName + "': no such session");
		return EXEC_FAILED;
	    }
	    currentSessionName = sessionName;
	}

	else if ("rename-session".equals(cmd)) {
	    String sessionName = null;
	    if (argc != 1) {
		return SYNTAX_ERROR;
	    }
	    sessionName = (String) st.nextElement();
	    if (sessionName.length() < 1) {
		return SYNTAX_ERROR;
	    }
	    session = (SQLSession) sessions.remove(currentSessionName);
	    if (session == null) {
		return EXEC_FAILED;
	    }
	    sessions.put(sessionName, session);
	    currentSessionName = sessionName;
	}

	else if ("disconnect".equals(cmd)) {
	    currentSessionName = null;
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
	    henplus.setPrompt(currentSessionName + "> ");
	}
	else {
	    henplus.setDefaultPrompt();
	}
	henplus.setSession(session);

	return SUCCESS;
    }

    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "manage sessions";
    }

    public String getSynopsis(String cmd) {
	if ("connect".equals(cmd)) {
	    return cmd + " <url> [session-name]"; 
	}
	else if ("switch".equals(cmd)) {
	    return cmd + " <session-name>";
	}
	else if ("rename-session".equals(cmd)) {
	    return cmd + " <new-session-name>";
	}
	return cmd; // disconnect
    }

    public String getLongDescription(String cmd) { 
	String dsc = null;
	if ("connect".equals(cmd)) {
	    dsc= "\tconnects to the url with the optional session name.\n"
		+"\tIf no session name is given, a session name is chosen.";
	}
	else if ("disconnect".equals(cmd)) {
	    dsc="\tdisconnect current session. You can leave a session as\n"
	    +"\twell if you just type CTRL-D";
	}
	else if ("switch".equals(cmd)) {
	    dsc="\tswitch to session with the given session name.";
	}
	else if ("sessions".equals(cmd)) {
	    dsc="\tlist all active sessions.";
	}
	else if ("rename-session".equals(cmd)) {
	    dsc="\trename current session. This influences the prompt.";
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
