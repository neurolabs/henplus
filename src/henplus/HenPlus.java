/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: HenPlus.java,v 1.3 2002-01-20 23:30:11 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */

import java.util.Properties;
import java.util.Enumeration;
import java.io.File;
import java.io.IOException;
import java.io.EOFException;

import commands.*;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;

public class HenPlus {
    private static final String EXIT_MSG   = "good bye.";
    private static final String HENPLUSDIR = ".henplus";
    private static final String PROMPT     = "Hen*Plus> ";

    private static HenPlus instance = null; // singleton.

    private CommandDispatcher dispatcher;
    private SQLSession        session;
    private Properties        properties;
    private boolean           terminated;
    private String            prompt;

    private HenPlus(Properties properties, String argv[]) throws IOException {
	terminated = false;
	this.properties = properties;
	
	try {
	    Readline.load(ReadlineLibrary.GnuReadline);
	    System.err.println("using GNU readline.");
	} catch (UnsatisfiedLinkError ignore_me) {
	    System.err.println("no readline found. Using simple stdin.");
	}
	Readline.initReadline("HenPlus");
	try {
	    Readline.readHistoryFile(getHistoryLocation());
	}
	catch (Exception ignore) {}
	
	Readline.setWordBreakCharacters(" ");

	/*
	 * initialize known JDBC drivers.
	 */
	Enumeration props = properties.propertyNames();
	while (props.hasMoreElements()) {
	    String name = (String) props.nextElement();
	    if (name.startsWith("driver.") && name.endsWith(".class")) {
		try {
		    Class.forName(properties.getProperty(name));
		}
		catch (Throwable t) {}
	    }
	}

	dispatcher = new CommandDispatcher();
	dispatcher.register(new HelpCommand());
	dispatcher.register(new DescribeCommand());
	dispatcher.register(new SQLCommand());
	dispatcher.register(new ListUserObjectsCommand());
	dispatcher.register(new ExportCommand());
	dispatcher.register(new ImportCommand());
	dispatcher.register(new ShellCommand());
	dispatcher.register(new ExitCommand());
	dispatcher.register(new StatusCommand());
	dispatcher.register(new ConnectCommand( argv, this ));
	Readline.setCompleter( dispatcher );
	setDefaultPrompt();
    }
    
    public void run() {
	StringBuffer cmd;
	String cmdLine = null;
	while (!terminated) {
	    try {
		cmdLine = Readline.readline( prompt );
	    }
	    catch (EOFException e) {
		if (session != null) {
		    dispatcher.execute(session, "disconnect");
		    continue;
		}
		else {
		    break; // last session closed.
		}
	    }
	    catch (Exception e) { /* ignore */ }
	    if (cmdLine == null)
		continue;
	    dispatcher.execute(session, cmdLine);
	}

	try {
	    Readline.writeHistoryFile(getHistoryLocation());
	}
	catch (Exception ignore) {}
    }

    public void terminate() {
	terminated = true;
    }
    public CommandDispatcher getDispatcher() { return dispatcher; }
    
    public void setSession(SQLSession session) {
	this.session = session;
    }

    public SQLSession getSession() {
	return session;
    }

    public void setPrompt(String prompt) {
	this.prompt = prompt;
    }
    
    public void setDefaultPrompt() {
	setPrompt( PROMPT );
    }

    //*****************************************************************
    public static HenPlus getInstance() {
	return instance;
    }

    public final static void main(String argv[]) throws Exception {
	Properties properties = new Properties();
	
	properties.setProperty("driver.Oracle.class", 
			       "oracle.jdbc.driver.OracleDriver");
	properties.setProperty("driver.Oracle.example",
			       "jdbc:oracle:thin:@localhost:1521:ORCL");

	properties.setProperty("driver.DB2.class",
			       "COM.ibm.db2.jdbc.net.DB2Driver");
	properties.setProperty("driver.DB2.example",
			       "jdbc:db2://localhost:6789/foobar");

	properties.setProperty("driver.MySQL.class",
			       "org.gjt.mm.mysql.Driver");
	properties.setProperty("driver.MySQL.example",
			       "jdbc:mysql://localhost/foobar");

	properties.setProperty("driver.SAP-DB.class",
			       "com.sap.dbtech.jdbc.DriverSapDB");
	properties.setProperty("driver.SAP-DB.example",
			       "jdbc:sapdb://localhost/foobar");

	properties.setProperty("driver.Postgres.class",
			       "org.postgresql.Driver");
	properties.setProperty("driver.Postgres.example",
			       "jdbc:postgresql://localhost/foobar");
	
	String cpy;
	cpy = 
"-------------------------------------------------------------------------\n"
+" HenPlus II 0.1 Copyright(C) 1997, 2001 Henner Zeller <H.Zeller@acm.org>\n"
+" HenPlus is provided AS IS and comes with ABSOLUTELY NO WARRANTY\n"
+" This is free software, and you are welcome to redistribute it under the\n"
+" conditions of the GNU Public License <http://www.gnu.org/>\n"
+"-------------------------------------------------------------------------\n";
	System.err.println(cpy);

	instance = new HenPlus(properties, argv);
	instance.run();
	System.err.println( EXIT_MSG );
    }

    private String getHistoryLocation() {
	/*
	 * test local directory.
	 */
	File henplusDir = new File( HENPLUSDIR );
	if (henplusDir.exists() && henplusDir.isDirectory()) {
	    return HENPLUSDIR + File.separator + "history";
	}

	/*
	 * fallback: home directory.
	 */
	String homeDir = System.getProperty("user.home", ".");
	henplusDir = new File(homeDir + File.separator + HENPLUSDIR);
	if (!henplusDir.exists()) {
	    henplusDir.mkdir();
	}
	return henplusDir.getAbsolutePath() + File.separator + "history";
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
