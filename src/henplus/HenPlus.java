/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: HenPlus.java,v 1.7 2002-01-22 08:01:42 hzeller Exp $
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
    private String            emptyPrompt;
    private StringBuffer      commandBuffer;

    private HenPlus(Properties properties, String argv[]) throws IOException {
	terminated = false;
	this.properties = properties;
	commandBuffer = new StringBuffer();

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
	//dispatcher.register(new DescribeCommand());
	dispatcher.register(new SQLCommand());
	//dispatcher.register(new ListUserObjectsCommand());
	//dispatcher.register(new ExportCommand());
	//dispatcher.register(new ImportCommand());
	dispatcher.register(new ShellCommand());
	dispatcher.register(new ExitCommand());
	dispatcher.register(new StatusCommand());
	dispatcher.register(new ConnectCommand( argv, this ));
	dispatcher.register(new LoadCommand());
	dispatcher.register(new AutocommitCommand()); // replace with 'set'
	Readline.setCompleter( dispatcher );
	setDefaultPrompt();
    }
    
    public void resetBuffer() {
	commandBuffer.setLength(0);
    }

    /**
     * add a new line. returns true if the line was complete.
     */
    public boolean addLine(String line) {
	commandBuffer.append(line);
	commandBuffer.append('\n');
	String completeCommand = commandBuffer.toString();
	Command c = dispatcher.getCommandFrom(completeCommand);
	if (c == null) {
	    return false;
	}
	if (!c.isComplete(completeCommand)) {
	    return false; // wait until we are complete
	}
	resetBuffer();
	dispatcher.execute(session, completeCommand);
	return true;
    }

    public void run() {
	String cmdLine = null;
	String displayPrompt = prompt;
	while (!terminated) {
	    try {
		cmdLine = Readline.readline( displayPrompt );
	    }
	    catch (EOFException e) {
		if (session != null) {
		    dispatcher.execute(session, "disconnect");
		    displayPrompt = prompt;
		    continue;
		}
		else {
		    break; // last session closed.
		}
	    }
	    catch (Exception e) { /* ignore */ }
	    if (cmdLine == null)
		continue;
	    boolean complete = false;
	    complete = addLine(cmdLine);
	    displayPrompt = (complete ? prompt : emptyPrompt);
	}
	
	dispatcher.shutdown();

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
	StringBuffer tmp = new StringBuffer();
	for (int i=prompt.length(); i > 0; --i) {
	    tmp.append(' ');
	}
	emptyPrompt = tmp.toString();
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

    public File getConfigDir() {
	/*
	 * test local directory.
	 */
	File henplusDir = new File( HENPLUSDIR );
	if (henplusDir.exists() && henplusDir.isDirectory()) {
	    return henplusDir;
	}

	/*
	 * fallback: home directory.
	 */
	String homeDir = System.getProperty("user.home", ".");
	henplusDir = new File(homeDir + File.separator + HENPLUSDIR);
	if (!henplusDir.exists()) {
	    henplusDir.mkdir();
	}
	return henplusDir;
    }

    private String getHistoryLocation() {
	return getConfigDir().getAbsolutePath() + File.separator + "history";
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
