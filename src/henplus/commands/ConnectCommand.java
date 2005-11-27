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
import henplus.io.ConfigurationContainer;
import henplus.view.Column;
import henplus.view.ColumnMetaData;
import henplus.view.TableRenderer;
import henplus.view.util.SortedMatchIterator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * document me.
 */
public class ConnectCommand extends AbstractCommand {
    
    private static String CONNECTION_CONFIG = "connections";
    private final ConfigurationContainer _config;
    private final SessionManager _sessionManager;
    private final SortedMap _knownUrls;
    private final HenPlus   _henplus;

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

    public ConnectCommand(  HenPlus henplus, SessionManager sessionManager) {
    	_henplus = henplus;
        _sessionManager = sessionManager;
    	_knownUrls = new TreeMap();
    	_config = henplus.createConfigurationContainer(CONNECTION_CONFIG);
    	_config.read(new ConfigurationContainer.ReadAction() {
            public void readConfiguration(InputStream inStream) throws Exception {
                if (inStream == null) return;
                BufferedReader in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
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
                    _knownUrls.put(alias, url);
                }
            }
        });
    }
    
    
    public void handleCommandline(CommandLine line){
        String url =null;
        String password = null;
        String username=null;
        String[] argv = line.getArgs();
        if (argv.length > 0) {
        
                url = argv[0];
                username = (argv.length > 1) ? argv[1] : null;
                password = (argv.length > 2) ? argv[2] : null;
       
        }
        if (line.hasOption("U")){
            username=line.getOptionValue("U");
        }
        if (line.hasOption("P")){
            password=line.getOptionValue("P");
        }
        if (line.hasOption("J")){
            url=line.getOptionValue("J");
        }
        if (url!=null){
            try {
                connect(url, username, password);
            }
            catch (Exception e) {
                //e.printStackTrace();
                HenPlus.msg().println(e.getMessage());
            }
        }
        
    }

    /**
     * @param url
     * @param username
     * @param password
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    private void connect(String url, String username, String password) throws ClassNotFoundException, SQLException, IOException {
        SQLSession session;
        session = new SQLSession(url, username, password);
        currentSessionName = createSessionName(session, null);
        _sessionManager.addSession(currentSessionName, session);
        _knownUrls.put(url, url);
        _henplus.setPrompt(currentSessionName + "> ");
        _sessionManager.setCurrentSession(session);
    }
    
    public void registerOptions(Options r) {
        Option option = new Option("J","url",true,"JDBC URL to connect to");
        option.setArgName("jdbc:...");
        r.addOption(option);
        Option option2 = new Option("U","username",true,"Username to connect with");
        option2.setArgName("username");
        r.addOption(option2);
        Option option3 = new Option("P","password",true,"Password to connect with");
        option3.setArgName("password");
        
        r.addOption(option3);
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

       _config.write(new ConfigurationContainer.WriteAction() {
           public void writeConfiguration(OutputStream out) throws Exception {
               PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
               Iterator urlIter = _knownUrls.entrySet().iterator();
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
       });
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
            return new SortedMatchIterator(lastWord, _knownUrls);
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
                if (_knownUrls.containsKey(url)) {
                    String possibleAlias = url;
                    url = (String) _knownUrls.get(url);
                    if (!possibleAlias.equals(url)) {
                        alias = possibleAlias;
                    }
                }
            }
	    try {
		session = new SQLSession(url, null, null);
		_knownUrls.put(url, url);
                if (alias != null) {
                    _knownUrls.put(alias, url);
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
	    _henplus.setPrompt(currentSessionName + "> ");
	}
	else {
	    _henplus.setDefaultPrompt();
	}
	_henplus.setCurrentSession(session);

	return SUCCESS;
    }

    private void showSessions() {
        HenPlus.msg().println("current session is marked with '*'");
        for (int i=0; i < SESS_META.length; ++i) {
            SESS_META[i].resetWidth();
        }
        TableRenderer table = new TableRenderer(SESS_META, HenPlus.out());
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
                +"\t~/.henplus configuration. All connects and aliases \n"
                +"\tare provided in the TAB-completion of this command.";
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
