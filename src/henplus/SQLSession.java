/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
import java.util.*;
import java.io.File;
import java.io.EOFException;
import java.util.Properties;
import java.util.Enumeration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineCompleter;
import org.gnu.readline.ReadlineLibrary;

import commands.*;

/**
 * document me.
 */
public class SQLSession {
    private Connection conn;
    private CommandDispatcher dispatcher;
    private boolean terminated = false;
    private static final String PROMPT = "Hen*Plus> ";
    private static final String EXIT_MSG = "good bye.";

    /**
     * creates a new SQL session. Open the database connection, initializes
     * the readline library
     */
    public SQLSession(Properties props, String argv[]) 
	throws IllegalArgumentException, ClassNotFoundException, SQLException {
	if (argv.length < 4)
	    throw new IllegalArgumentException("usage: <driver> <url> <user> <password>");
	String driverName = argv[0];
	Class driver = null;
	Enumeration e = props.propertyNames();
	while (e.hasMoreElements()) {
	    String name = (String) e.nextElement();
	    if (name.equals("driver." + driverName + ".class")) {
		driver = Class.forName(props.getProperty(name));
		break;
	    }
	}
	if (driver == null) {
	    throw new IllegalArgumentException("no driver found for '" 
					       + driverName + "'");
	}
	
	String url      = argv[1];
	String username = argv[2];
	String password = argv[3];
	
	System.err.print ("HenPlus II connecting to '" 
			  + url + '\'');
	conn = DriverManager.getConnection(url, username, password);
	System.err.println(" .. done.");

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
	dispatcher = new CommandDispatcher();
	dispatcher.register(new HelpCommand());
	dispatcher.register(new DescribeCommand());
	dispatcher.register(new SQLCommand());
	dispatcher.register(new ListUserObjectsCommand());
	dispatcher.register(new ExportCommand());
	dispatcher.register(new ImportCommand());
	dispatcher.register(new ShellCommand());
	dispatcher.register(new ExitCommand());
	Readline.setCompleter( dispatcher );
    }
    
    public String getHistoryLocation() {
	String homeDir = System.getProperty("user.home", ".");
	return homeDir + File.separator + ".henplus";
    }

    public void run() throws Exception {
	StringBuffer cmd;
	String cmdLine = null;
	while (!terminated) {
	    try {
		cmdLine = Readline.readline( PROMPT );
	    }
	    catch (EOFException e) {
		break;
	    }
	    catch (Exception e) { /* ignore */ }
	    if (cmdLine == null)
		continue;
	    dispatcher.execute(this, cmdLine);
	}
	System.err.println( EXIT_MSG );
	try {
	    Readline.writeHistoryFile(getHistoryLocation());
	}
	catch (Exception ignore) {}
    }
    
    public void terminate() {
	terminated = true;
    }

    /**
     * returns the current connection of this session.
     */
    public Connection getConnection() { return conn; }

    /**
     * returns the command dispatcher.
     */
    public CommandDispatcher getDispatcher() { return dispatcher; }
}
/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */

