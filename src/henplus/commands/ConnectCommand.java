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
import henplus.SQLSession;
import henplus.SessionManager;
import henplus.view.Column;
import henplus.view.ColumnMetaData;
import henplus.view.TableRenderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * document me.
 */
public class ConnectCommand extends AbstractCommand {
    
    private static String CONNECTION_CONFIG = "connections";
    private final SessionManager _sessionManager;
    private final SortedSet knownUrls;
    private final HenPlus   henplus;

    private final static ColumnMetaData[] SESS_META;

    static {
	SESS_META = new ColumnMetaData[3];
	SESS_META[0] = new ColumnMetaData("session");
	SESS_META[1] = new ColumnMetaData("user");
	SESS_META[2] = new ColumnMetaData("url");
    }

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

    public ConnectCommand(String argv[], HenPlus henplus, SessionManager sessionManager) {
    	this.henplus = henplus;
        this._sessionManager = sessionManager;
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
                _sessionManager.addSession(currentSessionName, session);
        		knownUrls.add(url);
        		henplus.setPrompt(currentSessionName + "> ");
        		_sessionManager.setCurrentSession(session);
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
	while (_sessionManager.sessionNameExists(key)) {
	    ++count;
	    key = name + "#" + count;
	}
	return key;
    }

    public void shutdown() {
	   _sessionManager.closeAll();

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
	    final Iterator it = _sessionManager.getSessionNames().tailSet(lastWord).iterator();
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
    public int execute(SQLSession currentSession, String cmd, String param) {
	SQLSession session = null;

	StringTokenizer st = new StringTokenizer(param);
	int argc = st.countTokens();
	
	if ("sessions".equals(cmd)) {
	    System.err.println("current session is marked with '*'");
	    SESS_META[0].resetWidth();
	    SESS_META[1].resetWidth();
	    SESS_META[2].resetWidth();
	    TableRenderer table = new TableRenderer(SESS_META, System.out);
	    Map.Entry entry = null;
	    Iterator it = _sessionManager.getSessionNames().iterator();
	    while (it.hasNext()) {
            String sessName = (String)it.next();
            session = _sessionManager.getSessionByName(sessName);
    		String prepend = sessName.equals(currentSessionName) 
    		    ? " * "
    		    : "   ";
    		Column[] row = new Column[3];
    		row[0] = new Column(prepend + sessName);
    		row[1] = new Column(session.getUsername());
    		row[2] = new Column(session.getURL());
    		table.addRow(row);
	    }
	    table.closeTable();
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
		_sessionManager.addSession(currentSessionName, session);
        _sessionManager.setCurrentSession(session);
	    }
	    catch (Exception e) {
		System.err.println(e);
		return EXEC_FAILED;
	    }
	}
	
	else if ("switch".equals(cmd)) {
	    String sessionName = null;
	    if (argc != 1 && _sessionManager.getSessionCount() != 2) {
		return SYNTAX_ERROR;
	    }
	    if (argc == 0 && _sessionManager.getSessionCount() == 2) {
		Iterator i = _sessionManager.getSessionNames().iterator();
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
	    session = _sessionManager.getSessionByName(sessionName);
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
        
        /*  // moved to sessionmanager.renameSession
         * 
            if (_sessionManager.sessionNameExists(sessionName)) {
                System.err.println("A session with that name already exists");
                return EXEC_FAILED;
            }

	    session = _sessionManager.removeSessionWithName(currentSessionName);
	    if (session == null) {
		  return EXEC_FAILED;
	    }
	    _sessionManager.addSession(sessionName, session);
         */
         int renamed = _sessionManager.renameSession(currentSessionName, sessionName);
         if (renamed == EXEC_FAILED)
            return EXEC_FAILED;
            
	    currentSessionName = sessionName;
        session = _sessionManager.getCurrentSession();
	}

	else if ("disconnect".equals(cmd)) {
	    currentSessionName = null;
	    if (argc != 0) {
		  return SYNTAX_ERROR;
	    }
        _sessionManager.closeCurrentSession();
        System.err.println("session closed.");
            
	    if (_sessionManager.hasSessions()) {
            currentSessionName = _sessionManager.getFirstSessionName();
		    session = _sessionManager.getSessionByName(currentSessionName);
	    }
	}
	
	if (currentSessionName != null) {
	    henplus.setPrompt(currentSessionName + "> ");
	}
	else {
	    henplus.setDefaultPrompt();
	}
	henplus.setCurrentSession(session);

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
	    return cmd + " <jdbc-url> [session-name]"; 
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
