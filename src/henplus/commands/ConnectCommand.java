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
import henplus.view.util.SortedMatchIterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * document me.
 */
public class ConnectCommand extends AbstractCommand {
    
    private static String CONNECTION_CONFIG = "connections";
    private final SessionManager _sessionManager;
    private final SortedMap knownUrls;
    private final HenPlus   henplus;

    private final static ColumnMetaData[] SESS_META;

    static {
	SESS_META = new ColumnMetaData[5];
	SESS_META[0] = new ColumnMetaData("session");
	SESS_META[1] = new ColumnMetaData("user");
	SESS_META[2] = new ColumnMetaData("jdbc url");
	SESS_META[3] = new ColumnMetaData("uptime");
	SESS_META[4] = new ColumnMetaData("#stmts", 
                                          ColumnMetaData.ALIGN_RIGHT);
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
    	knownUrls = new TreeMap();
    
    	try {
    	    File urlFile = new File(henplus.getConfigDir(),
    				    CONNECTION_CONFIG);
    	    BufferedReader in = new BufferedReader(new FileReader(urlFile));
    	    String urlLine;
    	    while ((urlLine = in.readLine()) != null) {
                StringTokenizer tok = new StringTokenizer(urlLine);
                String url;
                String alias;
                int tokNum = tok.countTokens();
                if (tokNum == 1) {
                    url = tok.nextToken();
                    alias = url;
                }
                else if (tokNum == 2) {
                    url = tok.nextToken();
                    alias = tok.nextToken();
                }
                else {
                    continue;
                }
                knownUrls.put(alias, url);
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
                knownUrls.put(url, url);
                henplus.setPrompt(currentSessionName + "> ");
                _sessionManager.setCurrentSession(session);
    	    }
    	    catch (Exception e) {
    		//e.printStackTrace();
    		HenPlus.msg().println(e.getMessage());
    	    }
    	}
    }
    
    /**
     * create a session name from an URL.
     */
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
            Iterator urlIter = knownUrls.entrySet().iterator();
            while (urlIter.hasNext()) {
                Map.Entry entry = (Map.Entry) urlIter.next();
                String alias = (String) entry.getKey();
                String url = (String) entry.getValue();
                if (alias.equals(url)) {
                    writer.println(url);
                }
                else {
                    writer.print(url);
                    writer.print(" ");
                    writer.println(alias);
                }
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
	    if (argumentCount(partialCommand) > ("".equals(lastWord) ? 1 : 2)) {
		return null;
	    }
            return new SortedMatchIterator(lastWord, knownUrls);
	}
	
	else if (partialCommand.startsWith("switch")) {
	    if (argumentCount(partialCommand) >
		("".equals(lastWord) ? 1 : 2)) {
		return null;
	    }
            return new SortedMatchIterator(lastWord, 
                                           _sessionManager.getSessionNames()) {
                    protected boolean exclude(String sessionName) {
                        return sessionName.equals(currentSessionName);
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
            showSessions();
	    return SUCCESS;
	}

	else if ("connect".equals(cmd)) {
	    if (argc < 1 || argc > 2) {
		return SYNTAX_ERROR;
	    }
	    String url = (String) st.nextElement();
            String alias = (argc==2) ? st.nextToken() : null;
            if (alias == null) {
                /*
                 * we only got one parameter. So the that single parameter
                 * might have been an alias. let's see..
                 */
                if (knownUrls.containsKey(url)) {
                    String possibleAlias = url;
                    url = (String) knownUrls.get(url);
                    if (!possibleAlias.equals(url)) {
                        alias = possibleAlias;
                    }
                }
            }
	    try {
		session = new SQLSession(url, null, null);
		knownUrls.put(url, url);
                if (alias != null) {
                    knownUrls.put(alias, url);
                }
		currentSessionName = createSessionName(session, alias);
		_sessionManager.addSession(currentSessionName, session);
                _sessionManager.setCurrentSession(session);
	    }
	    catch (Exception e) {
		HenPlus.msg().println(e.toString());
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
		HenPlus.msg().println("'" + sessionName + "': no such session");
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
             HenPlus.err().println("A session with that name already exists");
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
            HenPlus.msg().println("session closed.");
            
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

    private void showSessions() {
        HenPlus.msg().println("current session is marked with '*'");
        for (int i=0; i < SESS_META.length; ++i) {
            SESS_META[i].resetWidth();
        }
        TableRenderer table = new TableRenderer(SESS_META, HenPlus.out());
        Map.Entry entry = null;
        Iterator it = _sessionManager.getSessionNames().iterator();
        while (it.hasNext()) {
            String sessName = (String)it.next();
            SQLSession session = _sessionManager.getSessionByName(sessName);
            String prepend = sessName.equals(currentSessionName) 
                ? " * "
                : "   ";
            Column[] row = new Column[5];
            row[0] = new Column(prepend + sessName);
            row[1] = new Column(session.getUsername());
            row[2] = new Column(session.getURL());
            row[3] = new Column(TimeRenderer.renderTime(session.getUptime()));
            row[4] = new Column(session.getStatementCount());
            table.addRow(row);
        }
        table.closeTable();
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
		+"\tIf no session name is given, a session name is chosen.\n"
                +"\tIf a session name is given, this is stored as an alias\n"
                +"\tfor the URL as well, so later you might connect with\n"
                +"\tthat alias conveniently instead:\n"
                +"\t\tconnect jdbc:oracle:thin:foo/bar@localhost:BLUE myAlias\n"
                +"\tallows to later connect simply with\n"
                +"\t\tconnect myAlias\n"
                +"\tOf course, all URLs and aliases are stored in your \n"
                +"\t~/.henplus configuration.";
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
